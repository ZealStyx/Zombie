package io.github.zom.util;

import java.util.UUID;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDataDef;
import io.github.zom.config.ItemGridDef;

/**
 * A concrete instance of an item in the world or inventory.
 * rotated — when true gridW ↔ gridH are swapped in the grid.
 */
public class ItemInstance {

    public int     itemId;
    public int     quantity;
    public int     currentAmmo;
    public String  uuid;
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
        ItemGridDef  gd = ConfigLoader.getItemGridConfig().get(itemId);
        ItemDataDef  dd = ConfigLoader.getItemDataConfig().get(itemId);

        if (gd != null && gd.isContainer()) {
            instance.container = new ContainerInventory(gd.containerRows, gd.containerCols);
        }
        if (dd != null && dd.isGun()) {
            instance.currentAmmo = dd.clipSize;
        }
        return instance;
    }

    public int effectiveW() {
        ItemGridDef gd = ConfigLoader.getItemGridConfig().get(itemId);
        if (gd == null) return 1;
        return rotated ? gd.gridH : gd.gridW;
    }

    public int effectiveH() {
        ItemGridDef gd = ConfigLoader.getItemGridConfig().get(itemId);
        if (gd == null) return 1;
        return rotated ? gd.gridW : gd.gridH;
    }

    public void rotate() { rotated = !rotated; }

    @Override
    public String toString() {
        return "ItemInstance[id=" + itemId + ", qty=" + quantity
            + (rotated ? ", rotated" : "") + ", uuid=" + uuid + "]";
    }
}
