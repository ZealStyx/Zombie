package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.systems.IteratingSystem;

import io.github.zom.component.AnimationStateComponent;

/**
 * Advances stateTime on every animated entity and releases one-shot locks.
 *
 * FIX 1.4: When anim.minDuration > 0 the system uses that value as the unlock
 * threshold instead of DEFAULT_ONESHOT_DURATION, so callers can specify exact
 * durations via AnimationStateComponent.playOnce(pose, duration).
 */
public class AnimationStateSystem extends IteratingSystem {

    private static final float DEFAULT_ONESHOT_DURATION = 0.5f;

    private ComponentMapper<AnimationStateComponent> mAnim;

    public AnimationStateSystem() {
        super(Aspect.all(AnimationStateComponent.class));
    }

    @Override
    protected void process(int entityId) {
        AnimationStateComponent anim = mAnim.get(entityId);
        anim.tick(world.getDelta());

        if (anim.locked) {
            // FIX 1.4 — use minDuration when set, otherwise fall back to default
            float threshold = anim.minDuration > 0f
                ? anim.minDuration
                : DEFAULT_ONESHOT_DURATION;

            if (anim.stateTime >= threshold) {
                anim.locked      = false;
                anim.minDuration = 0f;
            }
        }
    }
}
