package io.github.zom.config;

/**
 * Per-item game properties from item_data.json.
 * Only items with meaningful combat/container/food stats appear in this file
 * (currently ~93 of 337 items). Items not present have no special gameplay data.
 */
public class ItemDataDef {

    public int     id;
    public String  name;
    public String  type;
    public boolean equippable;

    // ── Weapon stats ──────────────────────────────────────────────────────────
    public float  damage       = 0f;
    public float  range        = 0f;
    public int    clipSize     = 0;
    public float  fireCooldown = 0f;
    public float  reloadTime   = 0f;
    public int    ammoItemId   = 0;
    /** "semi" | "auto" | "both". Null/absent → semi. */
    public String fireMode     = "semi";

    // ── Container ─────────────────────────────────────────────────────────────
    public int containerRows = 0;
    public int containerCols = 0;

    // ── Food / consumable (extend as design grows) ────────────────────────────
    public float healAmount    = 0f;
    public float hungerRestore = 0f;
    public float thirstRestore = 0f;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isGun()         { return clipSize > 0; }
    public boolean isContainer()   { return containerRows > 0 && containerCols > 0; }
    public boolean isConsumable()  { return healAmount > 0 || hungerRestore > 0 || thirstRestore > 0; }

    public boolean supportsAutoFire() {
        return "auto".equals(fireMode) || "both".equals(fireMode);
    }
    public boolean supportsSemiFire() {
        return fireMode == null || "semi".equals(fireMode) || "both".equals(fireMode);
    }
}
