package io.github.zom.util;

/**
 * One slot in an inventory grid.
 * Empty slot: itemId == 0.
 */
public class InventorySlot {

    public int itemId   = 0;
    public int quantity = 0;

    public InventorySlot() {}

    public InventorySlot(int itemId, int quantity) {
        this.itemId   = itemId;
        this.quantity = quantity;
    }

    public boolean isEmpty() {
        return itemId == 0 || quantity <= 0;
    }

    public void clear() {
        itemId   = 0;
        quantity = 0;
    }

    public void set(int itemId, int quantity) {
        this.itemId   = itemId;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return isEmpty() ? "[empty]" : "[id=" + itemId + " x" + quantity + "]";
    }
}
