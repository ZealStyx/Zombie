package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.zom.component.AnimationStateComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.PlayerRendererComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.rendering.PlayerRenderer;
import io.github.zom.rendering.SpriteRenderState;

/**
 * Draws every player entity each frame using its four sprite layers.
 *
 * On first encounter (or when PlayerComponent.dirty is set), creates/rebuilds
 * the PlayerRenderer with the current equipment loadout.
 *
 * Requires: PlayerComponent, PlayerRendererComponent, TransformComponent, AnimationStateComponent
 */
public class PlayerRenderSystem extends IteratingSystem {

    private ComponentMapper<PlayerComponent>         mPlayer;
    private ComponentMapper<PlayerRendererComponent> mRenderer;
    private ComponentMapper<TransformComponent>      mTransform;
    private ComponentMapper<AnimationStateComponent> mAnim;

    private final SpriteBatch batch;

    // Reused per-frame to avoid allocation
    private final SpriteRenderState renderState = new SpriteRenderState();

    public PlayerRenderSystem(SpriteBatch batch) {
        super(Aspect.all(
            PlayerComponent.class,
            PlayerRendererComponent.class,
            TransformComponent.class,
            AnimationStateComponent.class
        ));
        this.batch = batch;
    }

    @Override
    protected void process(int entityId) {
        PlayerComponent         player    = mPlayer.get(entityId);
        PlayerRendererComponent rendComp  = mRenderer.get(entityId);
        TransformComponent      transform = mTransform.get(entityId);
        AnimationStateComponent anim      = mAnim.get(entityId);

        // ── Lazy-create or rebuild renderer on dirty flag ─────────────────────
        if (rendComp.renderer == null) {
            rendComp.renderer = new PlayerRenderer();
            player.dirty = true;
        }

        if (player.dirty) {
            rendComp.renderer.rebuild(
                player.skinName, player.handsName,
                player.heldItemId, player.holsteredItemId,
                player.vestId, player.helmetId, player.pantsId,
                player.topId,  player.backpackId, player.footwearId
            );
            player.dirty = false;
        }

        // ── Build render state from ECS components ────────────────────────────
        renderState.pose      = anim.pose;
        renderState.direction = transform.direction;
        renderState.stateTime = anim.stateTime;
        renderState.locked    = anim.locked;

        // ── Draw ──────────────────────────────────────────────────────────────
        rendComp.renderer.draw(
            batch, renderState,
            transform.x, transform.y,
            transform.w, transform.h
        );
    }
}
