package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.systems.IteratingSystem;
import com.artemis.utils.IntBag;
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
 * F key → pick up nearest world item within PICKUP_RANGE pixels.
 *
 * Also draws the item name above the sprite when the player is within
 * LABEL_RANGE pixels, using RobotoMono-Regular 9px (white, pixel-crisp).
 *
 * The label SpriteBatch is the same batch passed to WorldItemRenderSystem;
 * ItemPickupSystem calls batch.begin/end itself for the label pass because
 * it runs inside the ECS process loop where the batch is already active.
 * To avoid double begin(), the label drawing is deferred to a separate
 * drawLabels(SpriteBatch) call made by WorldItemRenderSystem after sprite
 * drawing.
 */
public class ItemPickupSystem extends IteratingSystem {

    public static final float PICKUP_RANGE = 40f; // px
    public static final float LABEL_RANGE = 60f; // px — show name label when closer than this

    private ComponentMapper<PlayerComponent> mPlayer;
    private ComponentMapper<TransformComponent> mTransform;
    private ComponentMapper<InventoryComponent> mInventory;
    private ComponentMapper<WorldItemComponent> mWorldItem;

    private EntitySubscription worldItemSub;

    // For drawing item name labels — set each frame by process(), read by
    // drawLabels()
    private float playerCx, playerCy;
    private boolean hasPlayer;

    private final BitmapFont labelFont;
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
        InventoryComponent inv = mInventory.get(entityId);

        playerCx = playerTf.x + playerTf.w * 0.5f;
        playerCy = playerTf.y + playerTf.h * 0.5f;
        hasPlayer = true;

        if (!Gdx.input.isKeyJustPressed(Input.Keys.F))
            return;

        int bestId = -1;
        float bestDistSq = PICKUP_RANGE * PICKUP_RANGE;

        IntBag items = worldItemSub.getEntities();
        int[] data = items.getData();
        for (int i = 0, n = items.size(); i < n; i++) {
            int eid = data[i];
            TransformComponent tf = mTransform.get(eid);
            float dx = (tf.x + tf.w * 0.5f) - playerCx;
            float dy = (tf.y + tf.h * 0.5f) - playerCy;
            float dSq = dx * dx + dy * dy;
            if (dSq <= bestDistSq) {
                bestDistSq = dSq;
                bestId = eid;
            }
        }

        if (bestId < 0)
            return;

        WorldItemComponent wi = mWorldItem.get(bestId);
        ItemInstance inst = wi.getItemInstance();
        int origQty = inst.quantity;
        int leftover = inv.inventory.add(inst);

        if (leftover == 0) {
            world.delete(bestId);
            Gdx.app.log("Pickup", "Picked up " + wi.itemId + " ×" + origQty);
        } else if (leftover < origQty) {
            wi.quantity = leftover;
            inst.quantity = leftover;
            Gdx.app.log("Pickup", "Partial pickup " + wi.itemId);
        } else {
            Gdx.app.log("Pickup", "Inventory full");
        }
    }

    /**
     * Called by WorldItemRenderSystem (inside an active batch) to draw item
     * name labels for items within LABEL_RANGE of the player.
     */
    public void drawLabels(SpriteBatch batch) {
        if (!hasPlayer)
            return;

        labelFont.setColor(Color.WHITE);

        IntBag items = worldItemSub.getEntities();
        int[] data = items.getData();
        for (int i = 0, n = items.size(); i < n; i++) {
            int eid = data[i];
            TransformComponent tf = mTransform.get(eid);
            float icx = tf.x + tf.w * 0.5f;
            float icy = tf.y + tf.h * 0.5f;
            float dx = icx - playerCx;
            float dy = icy - playerCy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > LABEL_RANGE)
                continue;

            WorldItemComponent wi = mWorldItem.get(eid);
            ItemDef def = ConfigLoader.getItemDatabase().get(wi.itemId);
            if (def == null)
                continue;

            layout.setText(labelFont, def.name);
            float lx = tf.x + tf.w * 0.5f - layout.width * 0.5f;
            float ly = tf.y + tf.h + 3f; // 3px above sprite top
            labelFont.draw(batch, def.name, lx, ly);
        }

        hasPlayer = false; // reset; set again next frame when player is processed
    }
}
