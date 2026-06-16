package io.github.zom.util;

/**
 * Represents the placement of an ItemInstance at specific grid row and column coordinates.
 */
public class ItemPlacement {

    public ItemInstance instance;
    public int r; // Row coordinate (0-indexed)
    public int c; // Column coordinate (0-indexed)

    public ItemPlacement() {}

    public ItemPlacement(ItemInstance instance, int r, int c) {
        this.instance = instance;
        this.r = r;
        this.c = c;
    }

    @Override
    public String toString() {
        return "ItemPlacement[" + instance + " at (" + r + "," + c + ")]";
    }
}
