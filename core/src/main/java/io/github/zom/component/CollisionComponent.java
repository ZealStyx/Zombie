package io.github.zom.component;

import com.artemis.Component;
import com.badlogic.gdx.math.Rectangle;

/**
 * Three axis-aligned collision rectangles for one entity, all in world units (pixels at PPU=1).
 *
 * ┌─────────────┐  ← top of sprite (y + h)
 * │  HEAD BOX   │  headBox  — hit detection (headshots, aimed attacks)
 * ├─────────────┤
 * │  BODY BOX   │  bodyBox  — hit detection (melee swings, projectiles)
 * ├─────────────┤
 * │  FEET BOX   │  feetBox  — environment / obstacle collision ONLY
 * └─────────────┘  ← y (bottom of sprite)
 *
 * FIX 1.2: All vertical offsets and heights are now proportions of entity h,
 * not absolute world units, so entities of different sprite sizes get correct boxes.
 *
 * CollisionSystem recalculates all three rectangles every frame.
 */
public class CollisionComponent extends Component {

    // ── Live rectangles (written by CollisionSystem each frame) ───────────────
    public final Rectangle feetBox = new Rectangle();
    public final Rectangle bodyBox = new Rectangle();
    public final Rectangle headBox = new Rectangle();

    // ── Proportions (fractions of entity w / h) ───────────────────────────────

    // Feet box
    public float feetInsetXFrac  = 0.25f;  // inset from each side: feetBox.w = w * (1 - 2*frac)
    public float feetOffsetYFrac = 0.00f;  // from entity bottom
    public float feetHeightFrac  = 0.20f;  // height = h * frac

    // Body box
    public float bodyInsetXFrac  = 0.15f;
    public float bodyOffsetYFrac = 0.20f;
    public float bodyHeightFrac  = 0.50f;

    // Head box
    public float headInsetXFrac  = 0.20f;
    public float headOffsetYFrac = 0.70f;
    public float headHeightFrac  = 0.25f;

    /**
     * Recalculate all three rectangles from the entity's current position and size.
     * Called by CollisionSystem every frame.
     *
     * FIX 1.2: uses h-proportional fractions so a 30-px player and an 8-px item
     * each get boxes that fit their actual sprite height.
     */
    public void recalculate(float x, float y, float w, float h) {
        feetBox.set(
            x + feetInsetXFrac * w,
            y + feetOffsetYFrac * h,
            w * (1f - feetInsetXFrac * 2f),
            h * feetHeightFrac
        );
        bodyBox.set(
            x + bodyInsetXFrac * w,
            y + bodyOffsetYFrac * h,
            w * (1f - bodyInsetXFrac * 2f),
            h * bodyHeightFrac
        );
        headBox.set(
            x + headInsetXFrac * w,
            y + headOffsetYFrac * h,
            w * (1f - headInsetXFrac * 2f),
            h * headHeightFrac
        );
    }
}
