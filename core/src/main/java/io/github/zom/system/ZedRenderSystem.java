package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.zom.component.AnimationStateComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.component.ZedComponent;
import io.github.zom.component.ZedRendererComponent;
import io.github.zom.rendering.SpriteRenderState;
import io.github.zom.rendering.ZedRenderer;

/**
 * Draws every zed entity each frame.
 *
 * On first encounter (or when ZedComponent.dirty is set), creates a new
 * ZedRenderer from the zed's type and skin names.
 *
 * Alive zeds:  drawAlive()  — shadow (jumper) + body skin
 * Dead  zeds:  drawDead()   — plays the die1/die2 animation until done, then holds last frame
 * Buried zeds: drawSpawnFx()— shown while underground before becoming a normal alive zed
 *
 * Requires: ZedComponent, ZedRendererComponent, TransformComponent, AnimationStateComponent
 */
public class ZedRenderSystem extends IteratingSystem {

    private ComponentMapper<ZedComponent>         mZed;
    private ComponentMapper<ZedRendererComponent> mRenderer;
    private ComponentMapper<TransformComponent>   mTransform;
    private ComponentMapper<AnimationStateComponent> mAnim;

    private final SpriteBatch batch;

    private final SpriteRenderState renderState = new SpriteRenderState();

    public ZedRenderSystem(SpriteBatch batch) {
        super(Aspect.all(
            ZedComponent.class,
            ZedRendererComponent.class,
            TransformComponent.class,
            AnimationStateComponent.class
        ));
        this.batch = batch;
    }

    @Override
    protected void process(int entityId) {
        ZedComponent         zed       = mZed.get(entityId);
        ZedRendererComponent rendComp  = mRenderer.get(entityId);
        TransformComponent   transform = mTransform.get(entityId);
        AnimationStateComponent anim   = mAnim.get(entityId);

        // ── Lazy-create or rebuild renderer on dirty flag ─────────────────────
        if (rendComp.renderer == null || zed.dirty) {
            rendComp.renderer = new ZedRenderer(
                zed.zedType,
                zed.skinName,
                zed.deadSkinName
            );
            zed.dirty = false;
        }

        float x = transform.x;
        float y = transform.y;
        float w = transform.w;
        float h = transform.h;

        // ── Buried zed: spawn fx phase ────────────────────────────────────────
        // When the AnimationState is "hidden1", "hidden2", "jump1", or "jump2",
        // we're in the underground/emergence phase — use spawn fx draw path.
        String pose = anim.pose;
        if (isSpawnFxPose(pose)) {
            renderState.pose      = pose;
            renderState.direction = transform.direction;
            renderState.stateTime = anim.stateTime;
            rendComp.renderer.drawSpawnFx(batch, renderState, x, y, w, h);
            return;
        }

        // ── Dead zed ─────────────────────────────────────────────────────────
        if (!zed.alive) {
            rendComp.deadStateTime += world.getDelta();
            rendComp.renderer.drawDead(batch, zed.dieVariant, rendComp.deadStateTime, x, y, w, h);
            return;
        }

        // ── Alive zed ────────────────────────────────────────────────────────
        renderState.pose      = pose;
        renderState.direction = transform.direction;
        renderState.stateTime = anim.stateTime;
        rendComp.renderer.drawAlive(batch, renderState, x, y, w, h);
    }

    private static boolean isSpawnFxPose(String pose) {
        return "hidden1".equals(pose) || "hidden2".equals(pose)
            || "jump1".equals(pose)   || "jump2".equals(pose);
    }
}
