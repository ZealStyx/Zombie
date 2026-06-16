package io.github.zom.config;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Deserialized from config/zed.json.
 *
 * Structure:
 *   sfx       → { fx_name → [framePath, …] }
 *   zed_types → { type_name → ZedTypeEntry }
 *
 * Types: normal, fast, army, buried, jumper, screamer, shooter, tank
 */
public class ZedConfig {

    /** VFX sequences: bio_explode, bio_splash, bullet. fx_name → frame paths. */
    public ObjectMap<String, Array<String>> sfx;

    /** zed_type_name → ZedTypeEntry */
    public ObjectMap<String, ZedTypeEntry> zed_types;

    // ── Inner classes ────────────────────────────────────────────────────────

    public static class ZedTypeEntry {
        /** skin_name → SkinEntry (alive skins) */
        public ObjectMap<String, SkinEntry> skins;
        /** dead_skin_name → { "die1" → [frames], "die2" → [frames] } */
        public ObjectMap<String, ObjectMap<String, Array<String>>> dead;
        /** Optional — buried zed: hidden + emerge jump frames */
        public SkinEntry spawn_fx;
        /** Optional — jumper shadow layer */
        public SkinEntry shadow;
    }

    public static class SkinEntry {
        public Array<String> available_poses;
        public ObjectMap<String, ObjectMap<String, Array<String>>> poses;

        public boolean hasPose(String pose) {
            return available_poses != null && available_poses.contains(pose, false);
        }

        public Array<String> getFrames(String pose, String direction) {
            if (poses == null) return null;
            ObjectMap<String, Array<String>> pd = poses.get(pose);
            if (pd == null) return null;
            Array<String> f = pd.get(direction);
            return f != null ? f : pd.get("frames");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public ZedTypeEntry getType(String type) {
        return zed_types != null ? zed_types.get(type) : null;
    }

    public SkinEntry getSkin(String type, String skinName) {
        ZedTypeEntry t = getType(type);
        return (t != null && t.skins != null) ? t.skins.get(skinName) : null;
    }

    public String randomSkinName(String type, com.badlogic.gdx.math.MathUtils mu) {
        ZedTypeEntry t = getType(type);
        if (t == null || t.skins == null || t.skins.size == 0) return null;
        Array<String> keys = t.skins.keys().toArray();
        return keys.get(mu.random(keys.size - 1));
    }

    public String randomDeadSkinName(String type, com.badlogic.gdx.math.MathUtils mu) {
        ZedTypeEntry t = getType(type);
        if (t == null || t.dead == null || t.dead.size == 0) return null;
        Array<String> keys = t.dead.keys().toArray();
        return keys.get(mu.random(keys.size - 1));
    }

    public Array<String> getDeadFrames(String type, String deadSkinName, String variant) {
        ZedTypeEntry t = getType(type);
        if (t == null || t.dead == null) return null;
        ObjectMap<String, Array<String>> ds = t.dead.get(deadSkinName);
        return ds != null ? ds.get(variant) : null;
    }
}
