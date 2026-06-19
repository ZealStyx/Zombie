package io.github.zom.config;

/**
 * Per-item grid geometry from item_grid.json.
 * Contains ONLY inventory footprint and container dimensions.
 * All weapon/food stats moved to ItemDataDef / item_data.json.
 */
public class ItemGridDef {

    public int    id;
    public String name;
    public String type;

    public int gridW = 1;
    public int gridH = 1;

    public int containerRows = 0;
    public int containerCols = 0;

    /** True if this item provides its own inventory grid (backpack/slingbag). */
    public boolean isContainer() { return containerRows > 0 && containerCols > 0; }

    /**
     * True when the item's natural orientation is portrait (taller than wide).
     * Used by InventoryUiSystem to decide whether to rotate the icon in
     * held/holstered gear slots.
     */
    public boolean isPortrait() { return gridH > gridW; }
}
