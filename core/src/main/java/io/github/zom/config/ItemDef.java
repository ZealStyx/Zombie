package io.github.zom.config;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/** One item row from items.json. */
public class ItemDef {

    public int     id;
    public int     old_id;
    public String  name;
    public String  description;
    /** food | medical | ammo | throwable | attachment | material | tool |
     *  footwear | accessory | melee | secondary | primary |
     *  vest | helmet | pants | top | backpack | deployable | unknown */
    public String  type;
    public boolean equippable;
    public Sprite  sprite;

    // ── Weapon stats (non-null only for weapons) ─────────────────────────────

    /** Damage per hit/shot. 0 = not a weapon. */
    public float damage;
    /** Max effective range in pixels. */
    public float range;
    /** Magazine/clip size for guns. 0 = melee weapon. */
    public int clipSize;
    /** Seconds between shots. */
    public float fireCooldown;
    /** Seconds to reload. */
    public float reloadTime;
    /** Item ID of required ammo. 0 = no ammo needed (melee). */
    public int ammoItemId;
    /** Fire mode: "semi", "auto", or "both". Null/empty = semi. */
    public String fireMode;

    // ── Inventory grid size ──────────────────────────────────────────────────

    /** Width in grid cells (default 1). */
    public int gridW = 1;
    /** Height in grid cells (default 1). */
    public int gridH = 1;

    // ── Container stats (for backpacks / sling bags) ─────────────────────────

    /** Number of inventory rows this container provides. 0 = not a container. */
    public int containerRows;
    /** Number of inventory cols this container provides. 0 = not a container. */
    public int containerCols;

    /** Helper: is this item a gun (has clipSize > 0)? */
    public boolean isGun() {
        return clipSize > 0;
    }

    /** Helper: supports automatic fire? */
    public boolean supportsAutoFire() {
        return "auto".equals(fireMode) || "both".equals(fireMode);
    }

    /** Helper: supports semi-automatic fire? */
    public boolean supportsSemiFire() {
        return fireMode == null || "semi".equals(fireMode) || "both".equals(fireMode);
    }

    public static class Sprite {
        /** "items/item/<id>.png" — inventory icon. */
        public String icon;
        /** "items/on_ground/<id>.png" — world drop sprite. */
        public String on_ground;
        /**
         * pose → direction → [framePath, …]  (or "frames" for directionless)
         * null for non-equippable items or items with no equipped/ folder.
         */
        public ObjectMap<String, ObjectMap<String, Array<String>>> equipped;
    }
}

