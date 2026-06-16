package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import io.github.zom.component.CollisionComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.world.WorldCollision;

/**
 * Updates feet/body/head collision boxes each frame and resolves
 * feet-box penetration against static world obstacles.
 */
public class CollisionSystem extends IteratingSystem {

    private ComponentMapper<CollisionComponent> mCollision;
    private ComponentMapper<TransformComponent> mTransform;

    public CollisionSystem() {
        super(Aspect.all(CollisionComponent.class, TransformComponent.class));
    }

    @Override
    protected void process(int entityId) {
        CollisionComponent collision = mCollision.get(entityId);
        TransformComponent transform = mTransform.get(entityId);

        collision.recalculate(transform.x, transform.y, transform.w, transform.h);
        resolveFeetCollision(collision, transform);
        collision.recalculate(transform.x, transform.y, transform.w, transform.h);
    }

    private void resolveFeetCollision(CollisionComponent collision, TransformComponent transform) {
        Array<Rectangle> obstacles = WorldCollision.getObstacles();
        Rectangle feet = collision.feetBox;

        for (int i = 0; i < obstacles.size; i++) {
            Rectangle obstacle = obstacles.get(i);
            if (!feet.overlaps(obstacle)) continue;

            float overlapLeft   = feet.x + feet.width  - obstacle.x;
            float overlapRight  = obstacle.x + obstacle.width  - feet.x;
            float overlapBottom = feet.y + feet.height - obstacle.y;
            float overlapTop    = obstacle.y + obstacle.height - feet.y;

            float minOverlapX = Math.min(overlapLeft, overlapRight);
            float minOverlapY = Math.min(overlapBottom, overlapTop);

            if (minOverlapX < minOverlapY) {
                if (overlapLeft < overlapRight) {
                    transform.x -= minOverlapX;
                } else {
                    transform.x += minOverlapX;
                }
            } else {
                if (overlapBottom < overlapTop) {
                    transform.y -= minOverlapY;
                } else {
                    transform.y += minOverlapY;
                }
            }

            collision.recalculate(transform.x, transform.y, transform.w, transform.h);
        }
    }
}
