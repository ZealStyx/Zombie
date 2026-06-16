package io.github.zom.component;

import com.artemis.Component;

/**
 * World position, rendered size, and facing direction for any entity.
 *
 * x, y      — bottom-left corner in world units.
 * w, h      — render size in world units (typically 1f × 1f for a character tile).
 * direction — "down" | "left" | "right" | "up"
 */
public class TransformComponent extends Component {

    public float  x         = 0f;
    public float  y         = 0f;
    public float  w         = 1f;
    public float  h         = 1f;
    public String direction = "down";

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}
