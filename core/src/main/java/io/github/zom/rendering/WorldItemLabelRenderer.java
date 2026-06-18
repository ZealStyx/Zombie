package io.github.zom.rendering;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.World;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.zom.component.TransformComponent;
import io.github.zom.component.WorldItemComponent;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDef;

/**
 * Static helper that draws floating item-name labels above nearby ground items.
 *
 * Extracted from ItemPickupSystem (which is deleted in Section 5.5) so the
 * label-rendering behaviour is preserved even after F-key pickup is removed.
 * GameRenderSystem calls draw() each frame after the sprite pass.
 *
 * LABEL_RANGE matches the old ItemPickupSystem.LABEL_RANGE value (60 px).
 */
public class WorldItemLabelRenderer {

    public static final float LABEL_RANGE = 60f;

    private final EntitySubscription worldItemSub;
    private final ComponentMapper<TransformComponent>  mTransform;
    private final ComponentMapper<WorldItemComponent>  mWorldItem;

    private final BitmapFont  labelFont;
    private final GlyphLayout layout = new GlyphLayout();

    public WorldItemLabelRenderer(World world) {
        worldItemSub = world.getAspectSubscriptionManager()
            .get(Aspect.all(WorldItemComponent.class, TransformComponent.class));
        mTransform = world.getMapper(TransformComponent.class);
        mWorldItem = world.getMapper(WorldItemComponent.class);
        labelFont  = FontCache.get().regular(9);
    }

    /**
     * Draw item-name labels above all ground items within LABEL_RANGE of the
     * player centre (pcx, pcy). Call between batch.begin() and batch.end().
     */
    public void draw(SpriteBatch batch, float pcx, float pcy) {
        labelFont.setColor(Color.WHITE);

        IntBag items = worldItemSub.getEntities();
        int[]  data  = items.getData();
        for (int i = 0, n = items.size(); i < n; i++) {
            int eid = data[i];
            TransformComponent tf  = mTransform.get(eid);
            float icx  = tf.x + tf.w * 0.5f;
            float icy  = tf.y + tf.h * 0.5f;
            float dist = (float) Math.sqrt((icx - pcx) * (icx - pcx)
                + (icy - pcy) * (icy - pcy));
            if (dist > LABEL_RANGE) continue;

            WorldItemComponent wi  = mWorldItem.get(eid);
            ItemDef            def = ConfigLoader.getItemDatabase().get(wi.itemId);
            if (def == null) continue;

            layout.setText(labelFont, def.name);
            float lx = tf.x + tf.w * 0.5f - layout.width * 0.5f;
            float ly = tf.y + tf.h + 3f;
            labelFont.draw(batch, def.name, lx, ly);
        }
    }
}
