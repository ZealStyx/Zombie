package io.github.zom.util;

/**
 * A sub-grid inventory that belongs to a backpack or sling bag item.
 * Persists inside the container item instance even when unequipped or dropped.
 */
public class ContainerInventory extends Inventory {

    public ContainerInventory() {
        super();
    }

    public ContainerInventory(int rows, int cols) {
        super(rows, cols);
    }
}
