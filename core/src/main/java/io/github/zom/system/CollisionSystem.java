package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.systems.IteratingSystem;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import io.github.zom.component.CollisionComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.component.ZedComponent;
import io.github.zom.world.WorldCollision;

/**
 * Recalculates feet/body/head collision boxes for every entity and
 * resolves feet-box penetration against static world obstacles (WorldCollision)
 * and active zombies against other active zombies.
 *
 * Only the feet box participates in environment collision — body and head are
 * read-only by CombatSystem / ZedAISystem for hit detection.
 */
public class CollisionSystem extends IteratingSystem {

    private ComponentMapper<CollisionComponent> mCollision;
    private ComponentMapper<TransformComponent> mTransform;
    private ComponentMapper<ZedComponent>            mZed;

    private EntitySubscription zedSub;

    public CollisionSystem() {
        super(Aspect.all(CollisionComponent.class, TransformComponent.class));
    }

    @Override
    protected void initialize() {
        zedSub = world.getAspectSubscriptionManager()
            .get(Aspect.all(ZedComponent.class, CollisionComponent.class, TransformComponent.class));
    }

    @Override
    protected void process(int entityId) {
        CollisionComponent col       = mCollision.get(entityId);
        TransformComponent transform = mTransform.get(entityId);

        col.recalculate(transform.x, transform.y, transform.w, transform.h);
        resolveFeet(col, transform);

        if (mZed.has(entityId) && mZed.get(entityId).alive) {
            resolveZombieVsZombie(entityId, col, transform);
        }

        // Recalculate once more after push-back so body/head are accurate this frame
        col.recalculate(transform.x, transform.y, transform.w, transform.h);
    }

    private void resolveFeet(CollisionComponent col, TransformComponent t) {
        Array<Rectangle> obstacles = WorldCollision.getObstacles();
        Rectangle feet = col.feetBox;

        for (int i = 0, n = obstacles.size; i < n; i++) {
            Rectangle obs = obstacles.get(i);
            if (!feet.overlaps(obs)) continue;

            float overlapL = feet.x + feet.width  - obs.x;
            float overlapR = obs.x  + obs.width   - feet.x;
            float overlapB = feet.y + feet.height - obs.y;
            float overlapT = obs.y  + obs.height  - feet.y;

            float minX = Math.min(overlapL, overlapR);
            float minY = Math.min(overlapB, overlapT);

            if (minX <= minY) {
                t.x += (overlapL < overlapR) ? -minX : minX;
            } else {
                t.y += (overlapB < overlapT) ? -minY : minY;
            }

            col.recalculate(t.x, t.y, t.w, t.h);
        }
    }

    private void resolveZombieVsZombie(int entityId, CollisionComponent col, TransformComponent t) {
        IntBag zeds = zedSub.getEntities();
        int[] ids = zeds.getData();
        Rectangle feet = col.feetBox;

        for (int i = 0, n = zeds.size(); i < n; i++) {
            int otherId = ids[i];
            if (otherId == entityId) continue;

            if (!mZed.has(otherId) || !mZed.get(otherId).alive) continue;

            CollisionComponent otherCol = mCollision.get(otherId);
            Rectangle otherFeet = otherCol.feetBox;

            if (feet.overlaps(otherFeet)) {
                float overlapL = feet.x + feet.width  - otherFeet.x;
                float overlapR = otherFeet.x  + otherFeet.width   - feet.x;
                float overlapB = feet.y + feet.height - otherFeet.y;
                float overlapT = otherFeet.y  + otherFeet.height  - feet.y;

                float minX = Math.min(overlapL, overlapR);
                float minY = Math.min(overlapB, overlapT);

                // Push this entity out by 0.5f of the overlap
                if (minX <= minY) {
                    float pushX = minX * 0.5f;
                    t.x += (overlapL < overlapR) ? -pushX : pushX;
                } else {
                    float pushY = minY * 0.5f;
                    t.y += (overlapB < overlapT) ? -pushY : pushY;
                }

                col.recalculate(t.x, t.y, t.w, t.h);
            }
        }
    }
}
