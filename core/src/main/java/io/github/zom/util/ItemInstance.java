package io.github.zom.util;

import java.util.UUID;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDef;

/**
 * Represents a concrete instance of an item in the world or in an inventory.
 * Contains instance-specific properties like stack quantity, loaded ammunition,
 * and container inventory references.
 */
public class ItemInstance {

    public int itemId;
    public int quantity;
    public int currentAmmo; // Loaded ammo for guns
    public String uuid;

    // Sub-grid inventory for container items (backpacks/sling bags)
    public ContainerInventory container;

    public ItemInstance() {
        this.uuid = UUID.randomUUID().toString();
    }

    public ItemInstance(int itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * Factory method to create a fully initialized ItemInstance.
     * Automatically initializes sub-grid containers for backpack/sling bag items.
     */
    public static ItemInstance create(int itemId, int quantity) {
        ItemInstance instance = new ItemInstance(itemId, quantity);
        ItemDef def = ConfigLoader.getItemDatabase().get(itemId);
        if (def != null) {
            // Check if this item is a container (backpack/sling bag)
            if (def.containerRows > 0 && def.containerCols > 0) {
                instance.container = new ContainerInventory(def.containerRows, def.containerCols);
            }
            // If it's a gun, initialize its loaded clip ammo
            if (def.isGun()) {
                instance.currentAmmo = def.clipSize;
            }
        }
        return instance;
    }

    @Override
    public String toString() {
        return "ItemInstance[id=" + itemId + ", quantity=" + quantity + ", uuid=" + uuid + "]";
    }
}
