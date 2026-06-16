package io.github.zom.util;

import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDef;

/**
 * Fixed-size inventory grid (rows × cols).
 * Pure data — no UI, no rendering. The UI layer reads this to draw slots.
 *
 * Stacking: items of the same id stack up to MAX_STACK_SIZE.
 * Non-stackable types (weapons, clothing) always occupy their own slot.
 */
public class Inventory {

    public static final int MAX_STACK_SIZE = 99;

    /** Item types that never stack — each instance takes its own slot. */
    private static final String[] NO_STACK_TYPES = {
        "melee", "primary", "secondary",
        "vest", "helmet", "pants", "top", "backpack", "footwear", "deployable"
    };

    // ── Grid ─────────────────────────────────────────────────────────────────

    private final InventorySlot[] slots;
    public  final int rows;
    public  final int cols;

    public Inventory(int rows, int cols) {
        this.rows  = rows;
        this.cols  = cols;
        this.slots = new InventorySlot[rows * cols];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new InventorySlot();
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public InventorySlot getSlot(int index) {
        return slots[index];
    }

    public InventorySlot getSlot(int row, int col) {
        return slots[row * cols + col];
    }

    public int totalSlots() {
        return slots.length;
    }

    // ── Add items ─────────────────────────────────────────────────────────────

    /**
     * Try to add the given quantity of itemId.
     * Stacks into existing slots first, then fills empty slots.
     * @return the quantity that could NOT be added (0 = full success)
     */
    public int add(int itemId, int quantity) {
        if (quantity <= 0) return 0;
        int remaining = quantity;

        if (isStackable(itemId)) {
            // Phase 1: fill existing partial stacks
            for (InventorySlot s : slots) {
                if (!s.isEmpty() && s.itemId == itemId && s.quantity < MAX_STACK_SIZE) {
                    int canFit = MAX_STACK_SIZE - s.quantity;
                    int take   = Math.min(canFit, remaining);
                    s.quantity += take;
                    remaining  -= take;
                    if (remaining == 0) return 0;
                }
            }
        }

        // Phase 2: fill empty slots
        for (InventorySlot s : slots) {
            if (s.isEmpty()) {
                int take = isStackable(itemId) ? Math.min(MAX_STACK_SIZE, remaining) : 1;
                s.set(itemId, take);
                remaining -= take;
                if (remaining == 0) return 0;
                if (!isStackable(itemId) && remaining > 0) continue;
            }
        }

        return remaining; // leftover that didn't fit
    }

    /**
     * Remove a quantity of itemId from the inventory.
     * Removes from the last matching slot first.
     * @return true if the full quantity was removed, false if there wasn't enough
     */
    public boolean remove(int itemId, int quantity) {
        // Check we have enough first
        if (count(itemId) < quantity) return false;

        int remaining = quantity;
        for (int i = slots.length - 1; i >= 0 && remaining > 0; i--) {
            InventorySlot s = slots[i];
            if (!s.isEmpty() && s.itemId == itemId) {
                int take = Math.min(s.quantity, remaining);
                s.quantity -= take;
                remaining  -= take;
                if (s.quantity <= 0) s.clear();
            }
        }
        return remaining == 0;
    }

    /** Remove exactly one item from a specific slot index. Returns the item id, or 0. */
    public int removeFromSlot(int slotIndex, int quantity) {
        InventorySlot s = slots[slotIndex];
        if (s.isEmpty()) return 0;
        int id   = s.itemId;
        int take = Math.min(s.quantity, quantity);
        s.quantity -= take;
        if (s.quantity <= 0) s.clear();
        return id;
    }

    /** Swap the contents of two slots (for drag-and-drop). */
    public void swapSlots(int indexA, int indexB) {
        InventorySlot a = slots[indexA];
        InventorySlot b = slots[indexB];
        int tmpId = a.itemId, tmpQty = a.quantity;
        a.set(b.itemId, b.quantity);
        b.set(tmpId, tmpQty);
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /** Total quantity of itemId across all slots. */
    public int count(int itemId) {
        int total = 0;
        for (InventorySlot s : slots) {
            if (!s.isEmpty() && s.itemId == itemId) total += s.quantity;
        }
        return total;
    }

    /** True if at least 1 of itemId exists. */
    public boolean contains(int itemId) {
        return count(itemId) > 0;
    }

    /** Index of first slot holding itemId, or -1. */
    public int findFirst(int itemId) {
        for (int i = 0; i < slots.length; i++) {
            if (!slots[i].isEmpty() && slots[i].itemId == itemId) return i;
        }
        return -1;
    }

    /** True if every slot is occupied. */
    public boolean isFull() {
        for (InventorySlot s : slots) {
            if (s.isEmpty()) return false;
        }
        return true;
    }

    /** Number of empty slots. */
    public int freeSlots() {
        int count = 0;
        for (InventorySlot s : slots) {
            if (s.isEmpty()) count++;
        }
        return count;
    }

    /** Clear all slots. */
    public void clear() {
        for (InventorySlot s : slots) s.clear();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static boolean isStackable(int itemId) {
        ItemDef def = ConfigLoader.getItemDatabase().get(itemId);
        if (def == null) return true;
        for (String t : NO_STACK_TYPES) {
            if (t.equals(def.type)) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Inventory[" + rows + "x" + cols + "]:\n");
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                sb.append("  [").append(r).append(",").append(c).append("] ")
                  .append(getSlot(r, c)).append("\n");
            }
        }
        return sb.toString();
    }
}
