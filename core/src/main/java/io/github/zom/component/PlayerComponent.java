package io.github.zom.component;

import com.artemis.Component;
import io.github.zom.util.ItemInstance;

/**
 * Player identity, equipment slots, and movement speed.
 *
 * CHANGE: Added equippedHeld and equippedHolstered ItemInstance references so
 * weapon state (currentAmmo) is preserved when weapons are moved between slots.
 */
public class PlayerComponent extends Component {

    public String skinName  = "player_skin_def_1";
    public String handsName = "player_hands_white";

    public int heldItemId      = 0;
    public int holsteredItemId = 0;
    public int vestId          = 0;
    public int helmetId        = 0;
    public int pantsId         = 0;
    public int topId           = 0;
    public int backpackId      = 0;
    public int slingBagId      = 0;
    public int footwearId      = 0;

    /** Full instance for held weapon — preserves currentAmmo across equip/holster cycles. */
    public ItemInstance equippedHeld;
    /** Full instance for holstered weapon. */
    public ItemInstance equippedHolstered;
    /** Full instance for backpack container. */
    public ItemInstance equippedBackpack;
    /** Full instance for sling bag container. */
    public ItemInstance equippedSlingBag;

    public boolean dirty = true;
    public float   speed = 120f;

    public void equip(String slot, ItemInstance instance) {
        int itemId = instance != null ? instance.itemId : 0;
        switch (slot) {
            case "held":
                heldItemId    = itemId;
                equippedHeld  = instance;
                break;
            case "holstered":
                holsteredItemId   = itemId;
                equippedHolstered = instance;
                break;
            case "vest":     vestId     = itemId; break;
            case "helmet":   helmetId   = itemId; break;
            case "pants":    pantsId    = itemId; break;
            case "top":      topId      = itemId; break;
            case "backpack":
                backpackId       = itemId;
                equippedBackpack = instance;
                break;
            case "slingbag":
            case "sling_bag":
                slingBagId       = itemId;
                equippedSlingBag = instance;
                break;
            case "footwear": footwearId = itemId; break;
        }
        dirty = true;
    }

    public void equip(String slot, int itemId) {
        if (itemId == 0) { unequip(slot); return; }
        equip(slot, ItemInstance.create(itemId, 1));
    }

    public void unequip(String slot) {
        switch (slot) {
            case "held":
                heldItemId   = 0;
                equippedHeld = null;
                break;
            case "holstered":
                holsteredItemId   = 0;
                equippedHolstered = null;
                break;
            case "vest":     vestId     = 0; break;
            case "helmet":   helmetId   = 0; break;
            case "pants":    pantsId    = 0; break;
            case "top":      topId      = 0; break;
            case "backpack":
                backpackId       = 0;
                equippedBackpack = null;
                break;
            case "slingbag":
            case "sling_bag":
                slingBagId       = 0;
                equippedSlingBag = null;
                break;
            case "footwear": footwearId = 0; break;
        }
        dirty = true;
    }
}
