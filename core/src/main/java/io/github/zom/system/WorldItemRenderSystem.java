package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.github.zom.component.TransformComponent;
import io.github.zom.component.WorldItemComponent;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDef;
import io.github.zom.rendering.TextureCache;

/**
 * Draws the on-ground sprite for every dropped item entity in the world.
 *
 * Sprite path comes from ItemDef.sprite.on_ground (e.g. "items/on_ground/192.png").
 * Textures are cached by TextureCache — no repeated I/O per frame.
 *
 * Requires: WorldItemComponent, TransformComponent
 */
public class WorldItemRenderSystem extends IteratingSystem {

    private ComponentMapper<WorldItemComponent> mItem;
    private ComponentMapper<TransformComponent> mTransform;

    private final SpriteBatch batch;

    public WorldItemRenderSystem(SpriteBatch batch) {
        super(Aspect.all(WorldItemComponent.class, TransformComponent.class));
        this.batch = batch;
    }

    @Override
    protected void process(int entityId) {
        WorldItemComponent item      = mItem.get(entityId);
        TransformComponent transform = mTransform.get(entityId);

        if (item.itemId <= 0) return;

        ItemDef def = ConfigLoader.getItemDatabase().get(item.itemId);
        if (def == null || def.sprite == null || def.sprite.on_ground == null) return;

        TextureRegion region = TextureCache.get().region(def.sprite.on_ground);
        batch.draw(region,
            transform.x, transform.y,
            transform.w, transform.h);
    }
}
