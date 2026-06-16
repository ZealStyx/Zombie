package io.github.zom.component;

import com.artemis.Component;
import com.badlogic.gdx.math.Rectangle;

/**
 * Three axis-aligned collision rectangles for one entity, all in world units.
 *
 * ┌─────────────┐  ← top of sprite (h)
 * │  HEAD BOX   │  headBox   — hit detection only (bullets, headshots)
 * │─────────────│
 * │  BODY BOX   │  bodyBox   — hit detection (melee swings, projectiles)
 * │─────────────│
 * │  FEET BOX   │  feetBox   — environment / obstacle collision only
 * └─────────────┘  ← y=0 (bottom of sprite)
 *
 * feetBox  — narrow rectangle at the bottom of the sprite used for wall/obstacle
 *            collision. CollisionSystem moves it with the entity and resolves
 *            penetration against the world geometry.
 *
 * bodyBox  — mid-height rectangle. Used by combat systems to test whether
 *            a swing or projectile hits this entity.
 *
 * headBox  — small rectangle near the top of the sprite. Used to detect headshots
 *            or attacks aimed high.
 *
 * All rectangles are updated every frame by CollisionSystem to track
 * TransformComponent.x / y.  Game code may read them freely; only
 * CollisionSystem writes them.
 *
 * Offsets are relative to the entity's (x, y) bottom-left.
 * Default proportions work well for a 1×1 world-unit character sprite.
 */
public class CollisionComponent extends Component {

    // ── Rectangles (updated each frame by CollisionSystem) ───────────────────
    public final Rectangle feetBox = new Rectangle();
    public final Rectangle bodyBox = new Rectangle();
    public final Rectangle headBox = new Rectangle();

    // ── Feet box proportions (relative to sprite bottom-left) ────────────────
    /** Horizontal inset from each side of the sprite. */
    public float feetInsetX      = 0.25f;
    /** Height of the feet box in world units. */
    public float feetHeight      = 0.20f;
    /** Vertical offset from the very bottom of the sprite. */
    public float feetOffsetY     = 0.00f;

    // ── Body box proportions ─────────────────────────────────────────────────
    public float bodyInsetX      = 0.15f;
    /** Where the body box starts, measured from sprite bottom. */
    public float bodyOffsetY     = 0.20f;
    /** Height of the body box in world units. */
    public float bodyHeight      = 0.50f;

    // ── Head box proportions ─────────────────────────────────────────────────
    public float headInsetX      = 0.20f;
    /** Where the head box starts, measured from sprite bottom. */
    public float headOffsetY     = 0.70f;
    /** Height of the head box in world units. */
    public float headHeight      = 0.25f;

    /**
     * Recalculate all three rectangles from the entity's current position and size.
     * Called by CollisionSystem every frame.
     */
    public void recalculate(float x, float y, float w, float h) {
        feetBox.set(x + feetInsetX * w,
            y + feetOffsetY,
            w - feetInsetX * w * 2f,
            feetHeight);

        bodyBox.set(x + bodyInsetX * w,
            y + bodyOffsetY,
            w - bodyInsetX * w * 2f,
            bodyHeight);

        headBox.set(x + headInsetX * w,
            y + headOffsetY,
            w - headInsetX * w * 2f,
            headHeight);
    }
}
