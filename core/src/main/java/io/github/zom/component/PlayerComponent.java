package io.github.zom.component;

import com.artemis.Component;
import io.github.zom.util.ItemInstance;

/**
 * Player identity, equipment slots, and movement speed.
 * Item IDs from items.json (0 = empty). Skin/hands names from player.json.
 * dirty=true triggers PlayerRenderer.rebuild() on next frame.
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

    // References to the container instances for backpacks and sling bags
    public ItemInstance equippedBackpack;
    public ItemInstance equippedSlingBag;

    public boolean dirty = true;

    /** Movement speed in pixels/second (PPU=1, player sprite = 30px). */
    public float speed = 120f;

    public void equip(String slot, ItemInstance instance) {
        int itemId = instance != null ? instance.itemId : 0;
        switch (slot) {
            case "held":      heldItemId      = itemId; break;
            case "holstered": holsteredItemId = itemId; break;
            case "vest":      vestId          = itemId; break;
            case "helmet":    helmetId        = itemId; break;
            case "pants":     pantsId         = itemId; break;
            case "top":       topId           = itemId; break;
            case "backpack":
                backpackId = itemId;
                equippedBackpack = instance;
                break;
            case "slingbag":
            case "sling_bag":
                slingBagId = itemId;
                equippedSlingBag = instance;
                break;
            case "footwear":  footwearId      = itemId; break;
        }
        dirty = true;
    }

    public void equip(String slot, int itemId) {
        if (itemId == 0) {
            unequip(slot);
            return;
        }
        ItemInstance inst = ItemInstance.create(itemId, 1);
        equip(slot, inst);
    }

    public void unequip(String slot) {
        switch (slot) {
            case "held":      heldItemId      = 0; break;
            case "holstered": holsteredItemId = 0; break;
            case "vest":      vestId          = 0; break;
            case "helmet":    helmetId        = 0; break;
            case "pants":     pantsId         = 0; break;
            case "top":       topId           = 0; break;
            case "backpack":
                backpackId = 0;
                equippedBackpack = null;
                break;
            case "slingbag":
            case "sling_bag":
                slingBagId = 0;
                equippedSlingBag = null;
                break;
            case "footwear":  footwearId      = 0; break;
        }
        dirty = true;
    }
}

