package io.github.zom.util;

import java.util.UUID;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDef;
import io.github.zom.config.ItemGridDef;

/**
 * A concrete instance of an item in the world or inventory.
 *
 * NEW: rotated — when true the item's gridW and gridH are swapped in the grid.
 * Toggled with R while dragging in the inventory UI.
 */
public class ItemInstance {

    public int     itemId;
    public int     quantity;
    public int     currentAmmo;
    public String  uuid;
    /** When true gridW ↔ gridH are swapped — item is rotated 90°. */
    public boolean rotated = false;

    public ContainerInventory container;

    public ItemInstance() {
        this.uuid = UUID.randomUUID().toString();
    }

    public ItemInstance(int itemId, int quantity) {
        this.itemId   = itemId;
        this.quantity = quantity;
        this.uuid     = UUID.randomUUID().toString();
    }

    public static ItemInstance create(int itemId, int quantity) {
        ItemInstance instance = new ItemInstance(itemId, quantity);
        ItemGridDef gd = ConfigLoader.getItemGridConfig().get(itemId);
        if (gd != null) {
            if (gd.isContainer()) {
                instance.container = new ContainerInventory(gd.containerRows, gd.containerCols);
            }
            if (gd.isGun()) {
                instance.currentAmmo = gd.clipSize;
            }
        }
        return instance;
    }

    /**
     * Effective grid width in the current orientation.
     * Uses ItemGridConfig; falls back to 1.
     */
    public int effectiveW() {
        ItemGridDef gd = ConfigLoader.getItemGridConfig().get(itemId);
        if (gd == null) return 1;
        return rotated ? gd.gridH : gd.gridW;
    }

    /**
     * Effective grid height in the current orientation.
     */
    public int effectiveH() {
        ItemGridDef gd = ConfigLoader.getItemGridConfig().get(itemId);
        if (gd == null) return 1;
        return rotated ? gd.gridW : gd.gridH;
    }

    /** Toggle rotation (swap W↔H). */
    public void rotate() { rotated = !rotated; }

    @Override
    public String toString() {
        return "ItemInstance[id=" + itemId + ", qty=" + quantity
            + (rotated ? ", rotated" : "") + ", uuid=" + uuid + "]";
    }
}
