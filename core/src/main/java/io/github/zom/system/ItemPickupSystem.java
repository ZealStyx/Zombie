package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.systems.IteratingSystem;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import io.github.zom.component.InventoryComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.component.WorldItemComponent;

/**
 * When the player presses F, picks up the nearest world item within range
 * and adds it to the player's inventory.
 */
public class ItemPickupSystem extends IteratingSystem {

    private static final float PICKUP_RANGE = 1.5f;

    private ComponentMapper<PlayerComponent>    mPlayer;
    private ComponentMapper<TransformComponent> mTransform;
    private ComponentMapper<InventoryComponent> mInventory;
    private ComponentMapper<WorldItemComponent> mWorldItem;

    private EntitySubscription worldItemSubscription;

    public ItemPickupSystem() {
        super(Aspect.all(
            PlayerComponent.class,
            TransformComponent.class,
            InventoryComponent.class
        ));
    }

    @Override
    protected void initialize() {
        worldItemSubscription = world.getAspectSubscriptionManager()
            .get(Aspect.all(WorldItemComponent.class, TransformComponent.class));
    }

    @Override
    protected void process(int entityId) {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.F)) return;

        TransformComponent playerTransform = mTransform.get(entityId);
        InventoryComponent inventory       = mInventory.get(entityId);

        float playerCx = playerTransform.x + playerTransform.w * 0.5f;
        float playerCy = playerTransform.y + playerTransform.h * 0.5f;

        int   bestItemId = -1;
        float bestDistSq = PICKUP_RANGE * PICKUP_RANGE;

        IntBag items = worldItemSubscription.getEntities();
        int[]  ids   = items.getData();
        for (int i = 0, n = items.size(); i < n; i++) {
            int itemEntityId = ids[i];
            TransformComponent itemTransform = mTransform.get(itemEntityId);

            float itemCx = itemTransform.x + itemTransform.w * 0.5f;
            float itemCy = itemTransform.y + itemTransform.h * 0.5f;

            float dx = itemCx - playerCx;
            float dy = itemCy - playerCy;
            float distSq = dx * dx + dy * dy;

            if (distSq <= bestDistSq) {
                bestDistSq = distSq;
                bestItemId = itemEntityId;
            }
        }

        if (bestItemId < 0) return;

        WorldItemComponent worldItem = mWorldItem.get(bestItemId);
        int leftover = inventory.inventory.add(worldItem.itemId, worldItem.quantity);

        if (leftover == 0) {
            world.delete(bestItemId);
            Gdx.app.log("ItemPickup", "Picked up item " + worldItem.itemId + " x" + worldItem.quantity);
        } else if (leftover < worldItem.quantity) {
            int pickedUp = worldItem.quantity - leftover;
            worldItem.quantity = leftover;
            Gdx.app.log("ItemPickup", "Partial pickup: item " + worldItem.itemId + " x" + pickedUp);
        } else {
            Gdx.app.log("ItemPickup", "Inventory full — could not pick up item " + worldItem.itemId);
        }
    }
}
