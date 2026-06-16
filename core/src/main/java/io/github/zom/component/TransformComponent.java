package io.github.zom.component;

import com.artemis.Component;

/**
 * World position (in pixels, PPU=1) and rendered size for any entity.
 *
 * x, y      — bottom-left corner in pixels.
 * w, h      — render size in pixels (set from actual texture dimensions).
 * direction — "down" | "left" | "right" | "up"
 *
 * Sprites render at their native pixel size: a 30×30 player sprite gets w=30, h=30.
 * A 8×8 on-ground item gets w=8, h=8. No artificial scaling is applied.
 */
public class TransformComponent extends Component {

    public float  x         = 0f;
    public float  y         = 0f;
    public float  w         = 30f;
    public float  h         = 30f;
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
