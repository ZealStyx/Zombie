package io.github.zom.component;

import com.artemis.Component;

/**
 * Marks an entity as the local player and holds all player-specific state
 * that drives the PlayerRenderer: skin, hands, and every equipment slot.
 *
 * Item IDs come from items.json (0 = slot empty).
 * skinName / handsName come from player.json available_skins / available_hands.
 *
 * Set dirty=true whenever any slot changes; PlayerRenderSystem will call
 * PlayerRenderer.rebuild() on the next frame and clear the flag.
 */
public class PlayerComponent extends Component {

    // ── Identity ─────────────────────────────────────────────────────────────
    public String skinName  = "player_skin_def_1";
    public String handsName = "player_hands_white";

    // ── Weapon slots ─────────────────────────────────────────────────────────
    /** Primary or melee weapon actively held and animated (layer 4). */
    public int heldItemId      = 0;
    /** Secondary weapon holstered but visible on body (uses h_ poses). */
    public int holsteredItemId = 0;

    // ── Clothing slots ───────────────────────────────────────────────────────
    public int vestId     = 0;
    public int helmetId   = 0;
    public int pantsId    = 0;
    public int topId      = 0;
    public int backpackId = 0;
    public int footwearId = 0;

    // ── Dirty flag ───────────────────────────────────────────────────────────
    /** True when any equipment slot changed — PlayerRenderSystem rebuilds on next frame. */
    public boolean dirty = true;

    // ── Speed ─────────────────────────────────────────────────────────────────
    /** Walk / run speed in world units per second. */
    public float speed = 4f;

    // ── Convenience mutators ─────────────────────────────────────────────────

    public void equip(String slot, int itemId) {
        switch (slot) {
            case "held":      heldItemId      = itemId; break;
            case "holstered": holsteredItemId = itemId; break;
            case "vest":      vestId          = itemId; break;
            case "helmet":    helmetId        = itemId; break;
            case "pants":     pantsId         = itemId; break;
            case "top":       topId           = itemId; break;
            case "backpack":  backpackId      = itemId; break;
            case "footwear":  footwearId      = itemId; break;
        }
        dirty = true;
    }

    public void unequip(String slot) {
        equip(slot, 0);
    }
}
