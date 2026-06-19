package io.github.zom.config;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Loaded from assets/config/item_grid.json.
 *
 * Provides:
 *  - O(1) lookup of grid size and weapon stats by item id.
 *  - Per-slot maximum size (largest item that can go in a given gear slot),
 *    used by InventoryUiSystem to size the gear slot widget correctly.
 */
public class ItemGridConfig {

    /** Pixel size of one inventory cell (from JSON "cellSize" field). */
    public int cellSize = 30;

    /** Raw array from JSON. */
    public Array<ItemGridDef> items;

    // Fast lookup
    private final IntMap<ItemGridDef> byId = new IntMap<>(512);

    public void buildIndex() {
        if (items == null) return;
        for (ItemGridDef d : items) byId.put(d.id, d);
    }

    public ItemGridDef get(int id) { return byId.get(id); }

    // ── Slot max-size helpers ──────────────────────────────────────────────────

    /**
     * Returns the largest (gridW, gridH) of all items compatible with a gear slot.
     * Used to size gear slot widgets so any equippable item fits without stretching.
     *
     * @param slotName "held" | "holstered" | "vest" | "helmet" | "pants" | "top" | "backpack" | "slingbag" | "footwear"
     * @return int[2] = {maxW, maxH} in grid cells
     */
    public int[] slotMaxSize(String slotName) {
        int maxW = 1, maxH = 1;
        if (items == null) return new int[]{maxW, maxH};
        for (ItemGridDef d : items) {
            if (typeMatchesSlot(d.type, slotName)) {
                if (d.gridW > maxW) maxW = d.gridW;
                if (d.gridH > maxH) maxH = d.gridH;
            }
        }
        return new int[]{maxW, maxH};
    }

    private static boolean typeMatchesSlot(String type, String slot) {
        if (type == null) return false;
        switch (slot) {
            case "held":
            case "holstered": return "melee".equals(type) || "primary".equals(type) || "secondary".equals(type);
            case "vest":      return "vest".equals(type);
            case "helmet":    return "helmet".equals(type);
            case "pants":     return "pants".equals(type);
            case "top":       return "top".equals(type);
            case "footwear":  return "footwear".equals(type);
            case "backpack":  return "backpack".equals(type);
            case "slingbag":  return "backpack".equals(type) || "slingbag".equals(type);
            default:          return false;
        }
    }

    public String size() {
        return "ItemGridConfig[cellSize=" + cellSize + ", items=" + items.size + "]";
    }
}
