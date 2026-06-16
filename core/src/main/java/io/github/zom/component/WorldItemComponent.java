package io.github.zom.component;

import com.artemis.Component;
import io.github.zom.util.ItemInstance;

/**
 * A world-dropped item entity.
 * itemId   — from items.json
 * quantity — stack count
 * The on-ground sprite path: ItemDef.sprite.on_ground → "items/on_ground/<id>.png"
 * Sprite is rendered at its native pixel dimensions (no scaling).
 */
public class WorldItemComponent extends Component {
    public int itemId   = 0;
    public int quantity = 1;
    public ItemInstance itemInstance;

    public ItemInstance getItemInstance() {
        if (itemInstance == null) {
            itemInstance = ItemInstance.create(itemId, quantity);
        }
        return itemInstance;
    }
}

