package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.systems.IteratingSystem;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.zom.component.InventoryComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.component.WorldItemComponent;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDef;
import io.github.zom.rendering.FontCache;
import io.github.zom.util.ItemInstance;

/**
 * F key (desktop) / Interact button (Android) → pick up nearest world item
 * within PICKUP_RANGE pixels.
 *
 * FIX: AndroidControllerSystem.interactPressed was set by the touch overlay
 * but never read here, so the "F" button on Android did nothing.
 * Added an isAndroid branch that checks the static flag in addition to the
 * keyboard F key.
 */
public class ItemPickupSystem extends IteratingSystem {

    public static final float PICKUP_RANGE = 40f;
    public static final float LABEL_RANGE  = 60f;

    private ComponentMapper<PlayerComponent>    mPlayer;
    private ComponentMapper<TransformComponent> mTransform;
    private ComponentMapper<InventoryComponent> mInventory;
    private ComponentMapper<WorldItemComponent> mWorldItem;

    private EntitySubscription worldItemSub;

    private float   playerCx, playerCy;
    private boolean hasPlayer;

    private final BitmapFont  labelFont;
    private final GlyphLayout layout = new GlyphLayout();

    public ItemPickupSystem() {
        super(Aspect.all(PlayerComponent.class, TransformComponent.class, InventoryComponent.class));
        labelFont = FontCache.get().regular(9);
    }

    @Override
    protected void initialize() {
        worldItemSub = world.getAspectSubscriptionManager()
            .get(Aspect.all(WorldItemComponent.class, TransformComponent.class));
    }

    @Override
    protected void process(int entityId) {
        TransformComponent playerTf = mTransform.get(entityId);
        InventoryComponent inv      = mInventory.get(entityId);

        playerCx  = playerTf.x + playerTf.w * 0.5f;
        playerCy  = playerTf.y + playerTf.h * 0.5f;
        hasPlayer = true;

        // FIX: check both keyboard F and Android interact button
        boolean isAndroid    = Gdx.app.getType() == Application.ApplicationType.Android;
        boolean pickupPressed = isAndroid
            ? AndroidControllerSystem.interactPressed
            : Gdx.input.isKeyJustPressed(Input.Keys.F);

        if (!pickupPressed) return;

        int   bestId     = -1;
        float bestDistSq = PICKUP_RANGE * PICKUP_RANGE;

        IntBag items = worldItemSub.getEntities();
        int[]  data  = items.getData();
        for (int i = 0, n = items.size(); i < n; i++) {
            int eid = data[i];
            TransformComponent tf = mTransform.get(eid);
            float dx  = (tf.x + tf.w * 0.5f) - playerCx;
            float dy  = (tf.y + tf.h * 0.5f) - playerCy;
            float dSq = dx * dx + dy * dy;
            if (dSq <= bestDistSq) { bestDistSq = dSq; bestId = eid; }
        }

        if (bestId < 0) return;

        WorldItemComponent wi      = mWorldItem.get(bestId);
        ItemInstance       inst    = wi.getItemInstance();
        int                origQty = inst.quantity;
        int                leftover = inv.inventory.add(inst);

        if (leftover == 0) {
            world.delete(bestId);
            Gdx.app.log("Pickup", "Picked up " + wi.itemId + " ×" + origQty);
        } else if (leftover < origQty) {
            wi.quantity   = leftover;
            inst.quantity = leftover;
            Gdx.app.log("Pickup", "Partial pickup " + wi.itemId);
        } else {
            Gdx.app.log("Pickup", "Inventory full");
        }
    }

    public void drawLabels(SpriteBatch batch) {
        if (!hasPlayer) return;

        labelFont.setColor(Color.WHITE);

        IntBag items = worldItemSub.getEntities();
        int[]  data  = items.getData();
        for (int i = 0, n = items.size(); i < n; i++) {
            int eid = data[i];
            TransformComponent tf  = mTransform.get(eid);
            float icx  = tf.x + tf.w * 0.5f;
            float icy  = tf.y + tf.h * 0.5f;
            float dist = (float) Math.sqrt((icx - playerCx) * (icx - playerCx)
                + (icy - playerCy) * (icy - playerCy));
            if (dist > LABEL_RANGE) continue;

            WorldItemComponent wi  = mWorldItem.get(eid);
            ItemDef            def = ConfigLoader.getItemDatabase().get(wi.itemId);
            if (def == null) continue;

            layout.setText(labelFont, def.name);
            float lx = tf.x + tf.w * 0.5f - layout.width * 0.5f;
            float ly = tf.y + tf.h + 3f;
            labelFont.draw(batch, def.name, lx, ly);
        }

        hasPlayer = false;
    }
}
