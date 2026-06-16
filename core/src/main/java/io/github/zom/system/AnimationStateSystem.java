package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.systems.IteratingSystem;

import io.github.zom.component.AnimationStateComponent;

/**
 * Advances stateTime on every entity that has an AnimationStateComponent.
 * Also clears the locked flag once a one-shot animation has run long enough
 * (estimated by DEFAULT_ONESHOT_DURATION; precise detection happens in
 * PlayerRenderSystem / ZedRenderSystem which have access to the actual Animation).
 *
 * Priority: run BEFORE render systems.
 */
public class AnimationStateSystem extends IteratingSystem {

    /** Fallback one-shot duration if we can't check the Animation directly. */
    private static final float DEFAULT_ONESHOT_DURATION = 0.5f;

    private ComponentMapper<AnimationStateComponent> mAnim;

    public AnimationStateSystem() {
        super(Aspect.all(AnimationStateComponent.class));
    }

    @Override
    protected void process(int entityId) {
        AnimationStateComponent anim = mAnim.get(entityId);
        anim.tick(world.getDelta());

        // Release lock after a safe fallback duration
        if (anim.locked && anim.stateTime >= DEFAULT_ONESHOT_DURATION) {
            anim.locked = false;
        }
    }
}
