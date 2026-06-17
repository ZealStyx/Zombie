package io.github.zom.util;

import com.badlogic.gdx.utils.Array;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDef;
import io.github.zom.config.ItemGridDef;

/**
 * 2D grid-based inventory.
 *
 * FIX — grid sizes now read from ItemGridConfig (item_grid.json) via
 * ItemInstance.effectiveW/H, which respect the rotated flag.
 *
 * FIX — base player inventory corrected to 6 cols × 4 rows per spec.
 *   (EntityFactory.createPlayer now calls inv.init(4, 6).)
 */
public class Inventory {

    public static final int MAX_STACK_SIZE = 99;

    private static final String[] NO_STACK_TYPES = {
        "melee", "primary", "secondary",
        "vest", "helmet", "pants", "top", "backpack", "footwear", "deployable"
    };

    public int rows;
    public int cols;
    public Array<ItemPlacement> placements = new Array<>();

    protected transient boolean[][] occupied;

    public Inventory() {}

    public Inventory(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        rebuildOccupiedGrid();
    }

    public void rebuildOccupiedGrid() {
        occupied = new boolean[rows][cols];
        if (placements == null) placements = new Array<>();
        for (ItemPlacement p : placements) {
            int w = p.instance.effectiveW();
            int h = p.instance.effectiveH();
            for (int r = p.r; r < p.r + h; r++) {
                for (int c = p.c; c < p.c + w; c++) {
                    if (r >= 0 && r < rows && c >= 0 && c < cols) {
                        occupied[r][c] = true;
                    }
                }
            }
        }
    }

    public boolean isCellOccupied(int r, int c) {
        if (occupied == null) rebuildOccupiedGrid();
        if (r < 0 || r >= rows || c < 0 || c >= cols) return true;
        return occupied[r][c];
    }

    public ItemPlacement getPlacementAt(int r, int c) {
        if (placements == null) return null;
        for (ItemPlacement p : placements) {
            int w = p.instance.effectiveW();
            int h = p.instance.effectiveH();
            if (r >= p.r && r < p.r + h && c >= p.c && c < p.c + w) {
                return p;
            }
        }
        return null;
    }

    /** Check if item fits at (startR, startC) in its current orientation. */
    public boolean canFit(int startR, int startC, ItemInstance inst) {
        return canFit(startR, startC, inst, null);
    }

    /** Check if item fits at (startR, startC), ignoring one existing instance. */
    public boolean canFit(int startR, int startC, ItemInstance inst, ItemInstance ignore) {
        int w = inst.effectiveW();
        int h = inst.effectiveH();
        if (startR < 0 || startR + h > rows || startC < 0 || startC + w > cols) return false;
        for (int r = startR; r < startR + h; r++) {
            for (int c = startC; c < startC + w; c++) {
                ItemPlacement p = getPlacementAt(r, c);
                if (p != null) {
                    if (ignore != null && p.instance.uuid.equals(ignore.uuid)) continue;
                    return false;
                }
            }
        }
        return true;
    }

    /** Legacy overload still used in a few places — reads gridW/H from ItemGridDef. */
    public boolean canFit(ItemDef def, int startR, int startC, ItemInstance ignore) {
        if (ignore != null) return canFit(startR, startC, ignore, null); // best we can do with just def
        ItemGridDef gd = ConfigLoader.getItemGridConfig().get(def.id);
        int w = gd != null ? gd.gridW : 1;
        int h = gd != null ? gd.gridH : 1;
        if (startR < 0 || startR + h > rows || startC < 0 || startC + w > cols) return false;
        for (int r = startR; r < startR + h; r++) {
            for (int c = startC; c < startC + w; c++) {
                if (getPlacementAt(r, c) != null) return false;
            }
        }
        return true;
    }

    public boolean addAt(ItemInstance inst, int r, int c) {
        if (!canFit(r, c, inst, inst)) return false;
        remove(inst);
        placements.add(new ItemPlacement(inst, r, c));
        rebuildOccupiedGrid();
        return true;
    }

    public int add(int itemId, int quantity) {
        if (quantity <= 0) return 0;
        return add(ItemInstance.create(itemId, quantity));
    }

    public int add(ItemInstance inst) {
        // Stack 1×1 stackable items first
        if (isStackable(inst.itemId) && inst.effectiveW() == 1 && inst.effectiveH() == 1) {
            for (ItemPlacement p : placements) {
                if (p.instance.itemId == inst.itemId && p.instance.quantity < MAX_STACK_SIZE) {
                    int take = Math.min(MAX_STACK_SIZE - p.instance.quantity, inst.quantity);
                    p.instance.quantity += take;
                    inst.quantity -= take;
                    if (inst.quantity <= 0) return 0;
                }
            }
        }
        // Find empty cell
        int w = inst.effectiveW(), h = inst.effectiveH();
        for (int r = 0; r <= rows - h; r++) {
            for (int c = 0; c <= cols - w; c++) {
                if (canFit(r, c, inst)) {
                    placements.add(new ItemPlacement(inst, r, c));
                    rebuildOccupiedGrid();
                    return 0;
                }
            }
        }
        return inst.quantity;
    }

    public boolean remove(int itemId, int quantity) {
        if (count(itemId) < quantity) return false;
        int remaining = quantity;
        for (int i = placements.size - 1; i >= 0 && remaining > 0; i--) {
            ItemPlacement p = placements.get(i);
            if (p.instance.itemId == itemId) {
                int take = Math.min(p.instance.quantity, remaining);
                p.instance.quantity -= take;
                remaining -= take;
                if (p.instance.quantity <= 0) placements.removeIndex(i);
            }
        }
        rebuildOccupiedGrid();
        return remaining == 0;
    }

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

    public int removeFromSlot(int slotIndex, int quantity) {
        int r = slotIndex / cols;
        int c = slotIndex % cols;
        ItemPlacement p = getPlacementAt(r, c);
        if (p == null) return 0;
        int id   = p.instance.itemId;
        int take = Math.min(p.instance.quantity, quantity);
        p.instance.quantity -= take;
        if (p.instance.quantity <= 0) placements.removeValue(p, true);
        rebuildOccupiedGrid();
        return id;
    }

    public int count(int itemId) {
        if (placements == null) return 0;
        int total = 0;
        for (ItemPlacement p : placements) {
            if (p.instance.itemId == itemId) total += p.instance.quantity;
        }
        return total;
    }

    public boolean contains(int itemId) { return count(itemId) > 0; }

    public boolean isFull() {
        if (occupied == null) rebuildOccupiedGrid();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (!occupied[r][c]) return false;
        return true;
    }

    public void clear() { placements.clear(); rebuildOccupiedGrid(); }

    public static boolean isStackable(int itemId) {
        ItemDef def = ConfigLoader.getItemDatabase().get(itemId);
        if (def == null) return true;
        for (String t : NO_STACK_TYPES) if (t.equals(def.type)) return false;
        return true;
    }
}
