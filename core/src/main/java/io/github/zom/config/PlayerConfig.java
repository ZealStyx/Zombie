package io.github.zom.config;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Deserialized from config/player.json.
 * Holds every skin + hands sprite path, organised by pose → direction → [frames].
 */
public class PlayerConfig {

    /** Bottom-to-top render order: body_skin → hands → equipped_clothing → equipped_held */
    public Array<String> render_layers;

    public String default_skin;
    public String default_hands;

    public Array<String> available_skins;
    public Array<String> available_hands;

    /** skin_name → SkinEntry */
    public ObjectMap<String, SkinEntry> skins;
    /** hand_name → HandEntry */
    public ObjectMap<String, HandEntry> hands;

    // ── Inner classes ────────────────────────────────────────────────────────

    public static class SkinEntry {
        /**
         * pose → direction → [framePath]   directional poses
         *      → "frames"  → [framePath]   directionless (using)
         */
        public ObjectMap<String, ObjectMap<String, Array<String>>> poses;
        /** Death animation frames in order: die_0.png … die_5.png */
        public Array<String> dead;

        public Array<String> getFrames(String pose, String direction) {
            if (poses == null) return null;
            ObjectMap<String, Array<String>> pd = poses.get(pose);
            if (pd == null) return null;
            Array<String> f = pd.get(direction);
            return f != null ? f : pd.get("frames");
        }
    }

    public static class HandEntry {
        public ObjectMap<String, ObjectMap<String, Array<String>>> poses;

        public Array<String> getFrames(String pose, String direction) {
            if (poses == null) return null;
            ObjectMap<String, Array<String>> pd = poses.get(pose);
            if (pd == null) return null;
            Array<String> f = pd.get(direction);
            return f != null ? f : pd.get("frames");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public SkinEntry getSkin(String name)  { return skins != null ? skins.get(name)  : null; }
    public HandEntry getHands(String name) { return hands != null ? hands.get(name)  : null; }
}
