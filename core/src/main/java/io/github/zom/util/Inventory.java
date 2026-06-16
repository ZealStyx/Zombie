package io.github.zom.util;

import com.badlogic.gdx.utils.Array;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDef;

/**
 * A 2D grid-based inventory.
 * Items can occupy multiple grid cells (width x height).
 * Supports stacking of 1x1 items, boundary checks, and collision/occupancy grids.
 */
public class Inventory {

    public static final int MAX_STACK_SIZE = 99;

    /** Item types that never stack — each instance takes its own slot. */
    private static final String[] NO_STACK_TYPES = {
        "melee", "primary", "secondary",
        "vest", "helmet", "pants", "top", "backpack", "footwear", "deployable"
    };

    public int rows;
    public int cols;
    public Array<ItemPlacement> placements = new Array<>();

    // Keep track of which cells are occupied
    protected transient boolean[][] occupied;

    public Inventory() {}

    public Inventory(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        rebuildOccupiedGrid();
    }

    /**
     * Recalculates the occupancy grid based on current item placements.
     */
    public void rebuildOccupiedGrid() {
        occupied = new boolean[rows][cols];
        if (placements == null) {
            placements = new Array<>();
        }
        for (ItemPlacement p : placements) {
            ItemDef def = ConfigLoader.getItemDatabase().get(p.instance.itemId);
            if (def == null) continue;
            int w = Math.max(1, def.gridW);
            int h = Math.max(1, def.gridH);
            for (int r = p.r; r < p.r + h; r++) {
                for (int c = p.c; c < p.c + w; c++) {
                    if (r >= 0 && r < rows && c >= 0 && c < cols) {
                        occupied[r][c] = true;
                    }
                }
            }
        }
    }

    /**
     * Checks if the cell (r, c) is occupied.
     */
    public boolean isCellOccupied(int r, int c) {
        if (occupied == null) rebuildOccupiedGrid();
        if (r < 0 || r >= rows || c < 0 || c >= cols) return true;
        return occupied[r][c];
    }

    /**
     * Gets the item placement covering the grid cell (r, c).
     */
    public ItemPlacement getPlacementAt(int r, int c) {
        if (placements == null) return null;
        for (ItemPlacement p : placements) {
            ItemDef def = ConfigLoader.getItemDatabase().get(p.instance.itemId);
            if (def == null) continue;
            int w = Math.max(1, def.gridW);
            int h = Math.max(1, def.gridH);
            if (r >= p.r && r < p.r + h && c >= p.c && c < p.c + w) {
                return p;
            }
        }
        return null;
    }

    /**
     * Checks if an item of type def can fit at starting cell (startR, startC).
     */
    public boolean canFit(ItemDef def, int startR, int startC) {
        return canFit(def, startR, startC, null);
    }

    /**
     * Checks if an item of type def can fit at starting cell (startR, startC), ignoring one instance.
     */
    public boolean canFit(ItemDef def, int startR, int startC, ItemInstance ignore) {
        if (occupied == null) rebuildOccupiedGrid();
        int w = Math.max(1, def.gridW);
        int h = Math.max(1, def.gridH);
        if (startR < 0 || startR + h > rows || startC < 0 || startC + w > cols) {
            return false;
        }

        // Instead of reading the cached occupied array, check collision manually to respect ignore list
        for (int r = startR; r < startR + h; r++) {
            for (int c = startC; c < startC + w; c++) {
                ItemPlacement p = getPlacementAt(r, c);
                if (p != null) {
                    if (ignore != null && p.instance.uuid.equals(ignore.uuid)) {
                        continue;
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Adds an item instance at a specific coordinate (r, c).
     */
    public boolean addAt(ItemInstance inst, int r, int c) {
        ItemDef def = ConfigLoader.getItemDatabase().get(inst.itemId);
        if (def == null) return false;
        if (!canFit(def, r, c, inst)) return false;

        // If it's already in the placements, remove it first (repositioning)
        remove(inst);

        placements.add(new ItemPlacement(inst, r, c));
        rebuildOccupiedGrid();
        return true;
    }

    /**
     * Adds an item quantity of a certain ID. Creates instances as needed.
     */
    public int add(int itemId, int quantity) {
        if (quantity <= 0) return 0;
        ItemInstance inst = ItemInstance.create(itemId, quantity);
        int leftover = add(inst);
        return leftover;
    }

    /**
     * Adds an item instance to the inventory.
     * Stacks into existing items of the same ID first (if 1x1 stackable),
     * then finds a free spot for the remainder.
     * Returns the leftover quantity.
     */
    public int add(ItemInstance inst) {
        ItemDef def = ConfigLoader.getItemDatabase().get(inst.itemId);
        if (def == null) return inst.quantity;

        boolean stackable = isStackable(inst.itemId);
        if (stackable && def.gridW == 1 && def.gridH == 1) {
            // Stack with existing
            for (ItemPlacement p : placements) {
                if (p.instance.itemId == inst.itemId && p.instance.quantity < MAX_STACK_SIZE) {
                    int canFit = MAX_STACK_SIZE - p.instance.quantity;
                    int take = Math.min(canFit, inst.quantity);
                    p.instance.quantity += take;
                    inst.quantity -= take;
                    if (inst.quantity <= 0) {
                        return 0;
                    }
                }
            }
        }

        // Find empty spot for the remaining item
        int w = Math.max(1, def.gridW);
        int h = Math.max(1, def.gridH);
        for (int r = 0; r <= rows - h; r++) {
            for (int c = 0; c <= cols - w; c++) {
                if (canFit(def, r, c)) {
                    placements.add(new ItemPlacement(inst, r, c));
                    rebuildOccupiedGrid();
                    return 0;
                }
            }
        }

        return inst.quantity;
    }

    /**
     * Removes a quantity of itemId from the inventory.
     * Returns true if the full quantity was removed, false if not.
     */
    public boolean remove(int itemId, int quantity) {
        if (count(itemId) < quantity) return false;

        int remaining = quantity;
        for (int i = placements.size - 1; i >= 0 && remaining > 0; i--) {
            ItemPlacement p = placements.get(i);
            if (p.instance.itemId == itemId) {
                int take = Math.min(p.instance.quantity, remaining);
                p.instance.quantity -= take;
                remaining -= take;
                if (p.instance.quantity <= 0) {
                    placements.removeIndex(i);
                }
            }
        }
        rebuildOccupiedGrid();
        return remaining == 0;
    }

    /**
     * Removes a specific item instance from the inventory.
     */
    public boolean remove(ItemInstance inst) {
        if (placements == null) return false;
        for (int i = 0; i < placements.size; i++) {
            if (placements.get(i).instance.uuid.equals(inst.uuid)) {
                placements.removeIndex(i);
                rebuildOccupiedGrid();
                return true;
            }
        }
        return false;
    }

    /**
     * Simulates original 1D slot index removal.
     */
    public int removeFromSlot(int slotIndex, int quantity) {
        int r = slotIndex / cols;
        int c = slotIndex % cols;
        ItemPlacement p = getPlacementAt(r, c);
        if (p == null) return 0;
        int id = p.instance.itemId;
        int take = Math.min(p.instance.quantity, quantity);
        p.instance.quantity -= take;
        if (p.instance.quantity <= 0) {
            placements.removeValue(p, true);
        }
        rebuildOccupiedGrid();
        return id;
    }

    /**
     * Counts the total quantity of itemId in the inventory.
     */
    public int count(int itemId) {
        int total = 0;
        if (placements == null) return 0;
        for (ItemPlacement p : placements) {
            if (p.instance.itemId == itemId) {
                total += p.instance.quantity;
            }
        }
        return total;
    }

    public boolean contains(int itemId) {
        return count(itemId) > 0;
    }

    public boolean isFull() {
        if (occupied == null) rebuildOccupiedGrid();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!occupied[r][c]) return false;
            }
        }
        return true;
    }

    public void clear() {
        placements.clear();
        rebuildOccupiedGrid();
    }

    public static boolean isStackable(int itemId) {
        ItemDef def = ConfigLoader.getItemDatabase().get(itemId);
        if (def == null) return true;
        for (String t : NO_STACK_TYPES) {
            if (t.equals(def.type)) return false;
        }
        return true;
    }
}
