package io.github.zom.config;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;

/** Equipped sprite data for one item (one entry in equipped.json). */
public class EquippedDatabase {

    /**
     * Per-item equipped sprite data.
     * Keys are pose names: "idle","run","axe","pistol","twohand","using","h_axe","h_run","h_pistol","h_twohand"
     * Values: direction → frame paths  ("down","left","right","up")
     *         or "frames" → frame paths  (for directionless poses like "using", "scream")
     */
    public static class Entry {
        public int    id;
        public String name;
        public String type;
        /** Only poses that actually have sprite files are listed here. */
        public Array<String> available_poses;
        /** pose → { direction → [framePath, ...] }   or   { "frames" → [framePath, ...] } */
        public ObjectMap<String, ObjectMap<String, Array<String>>> poses;

        /** Convenience: does this item have sprites for the given pose? */
        public boolean hasPose(String pose) {
            return available_poses != null && available_poses.contains(pose, false);
        }

        /**
         * Get frames for a specific pose and direction.
         * Falls back through the fallback chain if the pose isn't present.
         */
        public Array<String> getFrames(String pose, String direction) {
            if (poses == null) return null;
            ObjectMap<String, Array<String>> poseData = poses.get(pose);
            if (poseData == null) return null;
            // Directional
            Array<String> frames = poseData.get(direction);
            if (frames != null) return frames;
            // Directionless (scream, stun, using, etc.)
            return poseData.get("frames");
        }
    }

    /**
     * Fallback pose chain: if an item doesn't have a holstered pose,
     * fall back to the base pose.
     *   h_run    → run
     *   h_axe    → axe
     *   h_pistol → pistol
     *   h_twohand→ twohand
     */
    public ObjectMap<String, String> fallback_chain;

    /** Raw item map — JSON keys are string item IDs. Rebuilt to intMap after load. */
    public ObjectMap<String, Entry> items;

    private final IntMap<Entry> byId = new IntMap<>(256);

    public void buildIndex() {
        if (items == null) return;
        for (ObjectMap.Entry<String, Entry> e : items) {
            byId.put(Integer.parseInt(e.key), e.value);
        }
    }

    public Entry get(int id) {
        return byId.get(id);
    }

    /**
     * Resolve the correct pose name for this item.
     * If the item doesn't have the requested pose, walks the fallback chain.
     * Returns null if neither the pose nor any fallback is available.
     */
    public String resolvePose(int itemId, String requestedPose) {
        Entry e = get(itemId);
        if (e == null) return null;
        if (e.hasPose(requestedPose)) return requestedPose;
        // Walk fallback chain
        if (fallback_chain != null) {
            String fb = fallback_chain.get(requestedPose);
            if (fb != null && e.hasPose(fb)) return fb;
        }
        return null;
    }
}
