package io.github.zom.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Static axis-aligned obstacle rectangles in world units.
 * CollisionSystem resolves feet-box penetration against these.
 */
public final class WorldCollision {

    private static final Array<Rectangle> obstacles = new Array<>();

    private WorldCollision() {}

    public static void clear() {
        obstacles.clear();
    }

    public static void add(float x, float y, float w, float h) {
        obstacles.add(new Rectangle(x, y, w, h));
    }

    public static Array<Rectangle> getObstacles() {
        return obstacles;
    }
}
