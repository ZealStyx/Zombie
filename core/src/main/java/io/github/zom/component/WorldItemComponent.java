package io.github.zom.component;

import com.artemis.Component;

/**
 * Marks an entity as a world-dropped item.
 *
 * itemId   — id from items.json; used to look up the on-ground sprite path
 * quantity — stack size visible in the world
 *
 * Sprite path: ItemDef.sprite.on_ground → "items/on_ground/<itemId>.png"
 * WorldItemRenderSystem reads this every frame to draw the cached TextureRegion.
 * ItemPickupSystem reads this when the player presses F.
 */
public class WorldItemComponent extends Component {
    public int itemId   = 0;
    public int quantity = 1;
}
