package io.github.zom.config;

/**
 * Per-item grid layout and weapon stats from item_grid.json.
 * Kept separate from ItemDef / items.json to avoid bloat.
 *
 * gridW / gridH — inventory cell footprint (default 1×1).
 * Weapon fields  — only meaningful when type is melee / secondary / primary.
 * containerRows/Cols — only meaningful for backpack / slingbag types.
 */
public class ItemGridDef {

    public int    id;
    public String name;
    public String type;

    // ── Grid footprint ────────────────────────────────────────────────────────
    /** How many columns this item occupies in the inventory grid. */
    public int gridW = 1;
    /** How many rows this item occupies in the inventory grid. */
    public int gridH = 1;

    // ── Weapon stats ──────────────────────────────────────────────────────────
    public float  damage       = 0f;
    public float  range        = 0f;
    public int    clipSize     = 0;
    public float  fireCooldown = 0f;
    public float  reloadTime   = 0f;
    public int    ammoItemId   = 0;
    /** "semi" | "auto" | "both". Null = semi. */
    public String fireMode     = "semi";

    // ── Container (backpack / sling bag) ──────────────────────────────────────
    public int containerRows = 0;
    public int containerCols = 0;

    /** True if this item has a clip (gun). */
    public boolean isGun() { return clipSize > 0; }

    /** True if this item provides its own inventory grid. */
    public boolean isContainer() { return containerRows > 0 && containerCols > 0; }
}
