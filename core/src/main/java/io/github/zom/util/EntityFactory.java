package io.github.zom.util;

import com.artemis.World;

import io.github.zom.component.AnimationStateComponent;
import io.github.zom.component.CollisionComponent;
import io.github.zom.component.InventoryComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.PlayerRendererComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.component.WorldItemComponent;
import io.github.zom.component.ZedComponent;
import io.github.zom.component.ZedRendererComponent;

/**
 * Convenience factory for creating fully-composed entities.
 * All component wiring is done here so game code stays clean.
 */
public final class EntityFactory {

    private EntityFactory() {}

    // ── Player ────────────────────────────────────────────────────────────────

    /**
     * Create a player entity at the given world position.
     * Returns the entity id.
     *
     * Default loadout: def_1 skin, white hands, no equipment.
     * Inventory grid: 5 rows × 10 cols = 50 slots.
     */
    public static int createPlayer(World world, float x, float y) {
        int id = world.create();

        TransformComponent transform = world.edit(id).create(TransformComponent.class);
        transform.set(x, y, 1f, 1f);
        transform.direction = "down";

        world.edit(id).create(AnimationStateComponent.class);   // defaults: idle, stateTime=0
        world.edit(id).create(PlayerComponent.class);            // defaults: def_1 skin, no gear
        world.edit(id).create(PlayerRendererComponent.class);    // renderer built lazily by system

        InventoryComponent inv = world.edit(id).create(InventoryComponent.class);
        inv.init(5, 10);

        world.edit(id).create(CollisionComponent.class);

        return id;
    }

    // ── Zed ──────────────────────────────────────────────────────────────────

    /**
     * Create a zed entity at the given world position.
     *
     * @param zedType      e.g. "normal", "fast", "army", "buried", "jumper", "screamer", "shooter", "tank"
     * @param skinName     alive skin name, e.g. "zed_normal_skin3"
     * @param deadSkinName dead skin name, e.g. "zed_normal_skin3_dead"
     */
    public static int createZed(World world,
                                 float x, float y,
                                 String zedType,
                                 String skinName,
                                 String deadSkinName) {
        int id = world.create();

        TransformComponent transform = world.edit(id).create(TransformComponent.class);
        transform.set(x, y, 1f, 1f);
        transform.direction = "down";

        AnimationStateComponent anim = world.edit(id).create(AnimationStateComponent.class);
        anim.setPose("idle");

        ZedComponent zed = world.edit(id).create(ZedComponent.class);
        zed.zedType      = zedType;
        zed.skinName     = skinName;
        zed.deadSkinName = deadSkinName;
        zed.alive        = true;
        zed.dirty        = true;

        world.edit(id).create(ZedRendererComponent.class);
        world.edit(id).create(CollisionComponent.class);

        return id;
    }

    // ── World item ────────────────────────────────────────────────────────────

    /**
     * Drop an item into the world at position (x, y).
     * The on-ground sprite size is 0.6×0.6 world units by default.
     */
    public static int createWorldItem(World world,
                                       float x, float y,
                                       int itemId, int quantity) {
        int id = world.create();

        TransformComponent transform = world.edit(id).create(TransformComponent.class);
        transform.set(x, y, 0.6f, 0.6f);

        WorldItemComponent item = world.edit(id).create(WorldItemComponent.class);
        item.itemId   = itemId;
        item.quantity = quantity;

        return id;
    }
}
