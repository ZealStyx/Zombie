package io.github.zom.component;

import com.artemis.Component;

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
    public int footwearId      = 0;

    public boolean dirty = true;

    /** Movement speed in pixels/second (PPU=1, player sprite = 30px). */
    public float speed = 120f;

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

    public void unequip(String slot) { equip(slot, 0); }
}
