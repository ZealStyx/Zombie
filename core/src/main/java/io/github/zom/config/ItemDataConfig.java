package io.github.zom.config;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

/**
 * Top-level wrapper for item_data.json.
 * Call buildIndex() after JSON deserialization.
 */
public class ItemDataConfig {

    public Array<ItemDataDef> items;

    private final IntMap<ItemDataDef> byId = new IntMap<>(128);

    public void buildIndex() {
        if (items == null) return;
        for (ItemDataDef def : items) byId.put(def.id, def);
    }

    /**
     * Returns the ItemDataDef for the given id, or null if the item has no
     * special game stats (pure cosmetic / material / misc items).
     */
    public ItemDataDef get(int id) { return byId.get(id); }

    public int size() { return items == null ? 0 : items.size; }
}
