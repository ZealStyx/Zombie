package io.github.zom.config;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

/** Top-level wrapper for items.json. Call buildIndex() after JSON deserialization. */
public class ItemDatabase {

    /** Raw array deserialized from JSON — all 337 items. */
    public Array<ItemDef> items;

    private final IntMap<ItemDef> byId = new IntMap<>(512);

    /** Build the O(1) id → ItemDef map. Called by ConfigLoader.load(). */
    public void buildIndex() {
        for (ItemDef def : items) {
            byId.put(def.id, def);
        }
    }

    /** @return the ItemDef for the given id, or null if not found. */
    public ItemDef get(int id) {
        return byId.get(id);
    }

    public int size() {
        return items == null ? 0 : items.size;
    }
}
