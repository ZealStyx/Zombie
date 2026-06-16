package io.github.zom.rendering;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import io.github.zom.config.EquippedDatabase;
import io.github.zom.config.PlayerConfig;
import io.github.zom.config.ZedConfig;

/**
 * Converts raw config POJOs into ready-to-use AnimationSets.
 * One AnimationSet covers all poses + directions for one skin variant.
 *
 * Usage:
 *   AnimationSet playerSkin = AnimationSetBuilder.forPlayerSkin("player_skin_def_1");
 *   AnimationSet hands      = AnimationSetBuilder.forPlayerHands("player_hands_white");
 *   AnimationSet itemEquip  = AnimationSetBuilder.forEquippedItem(192);   // Mosin
 *   AnimationSet zedSkin    = AnimationSetBuilder.forZedSkin("normal", "zed_normal_skin1");
 */
public final class AnimationSetBuilder {

    private static final String[] DIRECTIONS = { "down", "left", "right", "up" };

    private AnimationSetBuilder() {}

    // ── Player skin ──────────────────────────────────────────────────────────

    public static AnimationSet forPlayerSkin(PlayerConfig cfg, String skinName) {
        PlayerConfig.SkinEntry skin = cfg.getSkin(skinName);
        if (skin == null || skin.poses == null) return new AnimationSet();
        AnimationSet out = new AnimationSet();
        addDirectionalPoses(out, skin.poses);
        // Dead animation — stored flat, no direction
        if (skin.dead != null && skin.dead.size > 0) {
            out.add("dead", "none", skin.dead);
        }
        return out;
    }

    // ── Player hands ─────────────────────────────────────────────────────────

    public static AnimationSet forPlayerHands(PlayerConfig cfg, String handName) {
        PlayerConfig.HandEntry hand = cfg.getHands(handName);
        if (hand == null || hand.poses == null) return new AnimationSet();
        AnimationSet out = new AnimationSet();
        addDirectionalPoses(out, hand.poses);
        return out;
    }

    // ── Equipped item overlay ────────────────────────────────────────────────

    public static AnimationSet forEquippedItem(EquippedDatabase db, int itemId) {
        EquippedDatabase.Entry entry = db.get(itemId);
        if (entry == null || entry.poses == null) return new AnimationSet();
        AnimationSet out = new AnimationSet();
        addDirectionalPoses(out, entry.poses);
        return out;
    }

    // ── Zed alive skin ───────────────────────────────────────────────────────

    public static AnimationSet forZedSkin(ZedConfig cfg, String type, String skinName) {
        ZedConfig.SkinEntry skin = cfg.getSkin(type, skinName);
        if (skin == null || skin.poses == null) return new AnimationSet();
        AnimationSet out = new AnimationSet();
        addDirectionalPoses(out, skin.poses);
        return out;
    }

    // ── Zed dead skin ────────────────────────────────────────────────────────

    /**
     * Builds an AnimationSet for a dead zed skin.
     * Each die variant ("die1", "die2") is stored as a directionless pose.
     */
    public static AnimationSet forZedDead(ZedConfig cfg, String type, String deadSkinName) {
        ZedConfig.ZedTypeEntry typeEntry = cfg.getType(type);
        if (typeEntry == null || typeEntry.dead == null) return new AnimationSet();
        ObjectMap<String, Array<String>> deadSkin = typeEntry.dead.get(deadSkinName);
        if (deadSkin == null) return new AnimationSet();
        AnimationSet out = new AnimationSet();
        for (ObjectMap.Entry<String, Array<String>> e : deadSkin) {
            // e.key = "die1" or "die2"; stored as pose:"die1" dir:"none"
            out.add(e.key, "none", e.value);
        }
        return out;
    }

    // ── Zed spawn fx (buried) ────────────────────────────────────────────────

    public static AnimationSet forZedSpawnFx(ZedConfig cfg, String type) {
        ZedConfig.ZedTypeEntry typeEntry = cfg.getType(type);
        if (typeEntry == null || typeEntry.spawn_fx == null) return new AnimationSet();
        AnimationSet out = new AnimationSet();
        addDirectionalPoses(out, typeEntry.spawn_fx.poses);
        return out;
    }

    // ── Zed shadow (jumper) ──────────────────────────────────────────────────

    public static AnimationSet forZedShadow(ZedConfig cfg, String type) {
        ZedConfig.ZedTypeEntry typeEntry = cfg.getType(type);
        if (typeEntry == null || typeEntry.shadow == null) return new AnimationSet();
        AnimationSet out = new AnimationSet();
        addDirectionalPoses(out, typeEntry.shadow.poses);
        return out;
    }

    // ── Shared helper ────────────────────────────────────────────────────────

    /**
     * Iterates a pose map:
     *   pose → { "down"/"left"/"right"/"up" → [paths] }   (directional)
     *   pose → { "frames" → [paths] }                      (directionless)
     */
    private static void addDirectionalPoses(
            AnimationSet out,
            ObjectMap<String, ObjectMap<String, Array<String>>> posesMap) {

        if (posesMap == null) return;
        for (ObjectMap.Entry<String, ObjectMap<String, Array<String>>> poseEntry : posesMap) {
            String pose = poseEntry.key;
            ObjectMap<String, Array<String>> dirMap = poseEntry.value;
            if (dirMap == null) continue;

            // Directional?
            boolean anyDir = false;
            for (String d : DIRECTIONS) {
                Array<String> paths = dirMap.get(d);
                if (paths != null && paths.size > 0) {
                    out.add(pose, d, paths);
                    anyDir = true;
                }
            }
            // Directionless (frames key)
            if (!anyDir) {
                Array<String> paths = dirMap.get("frames");
                if (paths != null && paths.size > 0) {
                    out.add(pose, "none", paths);
                }
            }
        }
    }
}
