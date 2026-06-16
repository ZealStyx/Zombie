package io.github.zom.rendering;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ZedConfig;

/**
 * Renderer for a single zed entity.
 * Each zed entity gets its own ZedRenderer instance, built when the zed spawns.
 *
 * A zed can have:
 *   - One alive skin (directional: idle / walk / run / attack + type-specific poses)
 *   - One dead skin (die1 / die2 variants, directionless)
 *   - Optional shadow layer (jumper only, drawn before the skin)
 *   - Optional spawn_fx layer (buried only: hidden1/hidden2/jump1/jump2)
 */
public class ZedRenderer {

    private final AnimationSet skinAnims;     // alive skin
    private final AnimationSet deadAnims;     // death skin (or null if not yet assigned)
    private final AnimationSet shadowAnims;   // optional jumper shadow
    private final AnimationSet spawnFxAnims;  // optional buried spawn effect

    private final String zedType;

    /**
     * @param zedType       e.g. "normal", "fast", "army", "buried", "jumper", "screamer", "shooter", "tank"
     * @param skinName      e.g. "zed_normal_skin1"
     * @param deadSkinName  e.g. "zed_normal_skin1_dead" (may be null — assign later on death)
     */
    public ZedRenderer(String zedType, String skinName, String deadSkinName) {
        this.zedType = zedType;
        ZedConfig cfg = ConfigLoader.getZedConfig();

        skinAnims   = AnimationSetBuilder.forZedSkin(cfg, zedType, skinName);
        deadAnims   = deadSkinName != null
            ? AnimationSetBuilder.forZedDead(cfg, zedType, deadSkinName) : null;
        shadowAnims = AnimationSetBuilder.forZedShadow(cfg, zedType);
        spawnFxAnims= AnimationSetBuilder.forZedSpawnFx(cfg, zedType);
    }

    // ── Draw ─────────────────────────────────────────────────────────────────

    /**
     * Draw the zed's alive animation.
     * Call this every frame while the zed is alive.
     */
    public void drawAlive(SpriteBatch batch, SpriteRenderState state,
                          float x, float y, float w, float h) {
        // Shadow first (jumper only — drawn at ground level)
        drawLayer(batch, shadowAnims, "default", "none", state.stateTime, x, y, w, h);

        // Body
        drawLayer(batch, skinAnims, state.pose, state.direction, state.stateTime, x, y, w, h);
    }

    /**
     * Draw the spawn emergence animation (buried zed only).
     * Use pose "hidden1"/"hidden2" while underground, "jump1"/"jump2" when emerging.
     */
    public void drawSpawnFx(SpriteBatch batch, SpriteRenderState state,
                            float x, float y, float w, float h) {
        if (spawnFxAnims == null) return;
        drawLayer(batch, spawnFxAnims, state.pose, "none", state.stateTime, x, y, w, h);
    }

    /**
     * Draw the death animation.
     * @param dieVariant "die1" or "die2" — chosen at death time based on dead skin available variants
     */
    public void drawDead(SpriteBatch batch, String dieVariant, float stateTime,
                         float x, float y, float w, float h) {
        if (deadAnims == null) return;
        drawLayer(batch, deadAnims, dieVariant, "none", stateTime, x, y, w, h);
    }

    /** Returns true if this renderer has the specified death animation pose loaded. */
    public boolean hasDeadPose(String pose) {
        return deadAnims != null && deadAnims.hasPose(pose);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void drawLayer(SpriteBatch batch, AnimationSet anims,
                           String pose, String direction, float stateTime,
                           float x, float y, float w, float h) {
        if (anims == null) return;
        Animation<TextureRegion> anim = anims.get(pose, direction);
        if (anim == null) anim = anims.get(pose, "none");
        if (anim == null) anim = anims.get("idle", direction);
        if (anim == null) return;
        TextureRegion frame = anim.getKeyFrame(stateTime);
        if (frame != null) batch.draw(frame, x, y, w, h);
    }

    public String getZedType() { return zedType; }
}
