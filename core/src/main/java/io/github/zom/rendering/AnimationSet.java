package io.github.zom.rendering;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Holds every directional Animation for one skin (player or zed).
 *
 * Key format:  "pose:direction"
 *   e.g. "run:down"   "idle:left"   "using:none"   "attack:right"
 *
 * Directionless poses (scream, stun, using, fly, …) use "none" as direction.
 * Built once from config frame-path arrays — no I/O after construction.
 */
public class AnimationSet {

    public static final float DEFAULT_FPS = 8f;

    private final ObjectMap<String, Animation<TextureRegion>> anims = new ObjectMap<>(32);

    /** One-shot poses play once then hold the last frame. */
    private static final String[] PLAY_ONCE_POSES = {
        "using", "attack", "scream", "stun", "throw", "dodge",
        "fly", "jump1", "jump2", "hidden1", "hidden2", "default", "die1", "die2", "dead"
    };

    // ── Construction ─────────────────────────────────────────────────────────

    /**
     * Register an animation for the given pose + direction.
     *
     * @param pose      e.g. "run", "idle", "axe"
     * @param direction "down"|"left"|"right"|"up"  or "none" for directionless
     * @param paths     ordered frame paths (relative to assets/)
     * @param fps       playback speed
     */
    public void add(String pose, String direction, Array<String> paths, float fps) {
        if (paths == null || paths.size == 0) return;
        Array<TextureRegion> frames = TextureCache.get().regions(paths);
        Animation.PlayMode mode = isPlayOnce(pose)
            ? Animation.PlayMode.NORMAL
            : Animation.PlayMode.LOOP;
        anims.put(key(pose, direction), new Animation<>(1f / fps, frames, mode));
    }

    public void add(String pose, String direction, Array<String> paths) {
        add(pose, direction, paths, DEFAULT_FPS);
    }

    // ── Retrieval ────────────────────────────────────────────────────────────

    /** Get the animation for pose + direction, or null if not registered. */
    public Animation<TextureRegion> get(String pose, String direction) {
        return anims.get(key(pose, direction));
    }

    /** True if any animation has been registered for this pose (any direction). */
    public boolean hasPose(String pose) {
        for (String k : anims.keys()) {
            if (k.startsWith(pose + ":")) return true;
        }
        return false;
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private static String key(String pose, String direction) {
        return pose + ":" + direction;
    }

    private static boolean isPlayOnce(String pose) {
        for (String p : PLAY_ONCE_POSES) {
            if (p.equals(pose)) return true;
        }
        return false;
    }
}
