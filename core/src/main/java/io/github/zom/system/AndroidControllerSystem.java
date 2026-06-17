package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.zom.component.PlayerComponent;

/**
 * On-screen touch controls for Android, drawn in screen space.
 *
 * FIX 1 — Joystick per-pointer tracking:
 *   The original code used a single loop where the joystick's `continue`
 *   prevented other buttons from being checked for the same pointer, and
 *   once joyPointer was set all non-joystick pointers skipped button checks.
 *   Rewritten to do two separate passes: one for joystick, one for buttons.
 *
 * FIX 2 — Movement integration in MovementSystem:
 *   AndroidControllerSystem.moveX/Y are now normalised here before being
 *   read by MovementSystem, so diagonal movement isn't faster than axial.
 *
 * FIX 3 — ShapeRenderer projection:
 *   ShapeRenderer must use a screen-space projection (identity/ortho on
 *   screen pixels), NOT the game world camera's combined matrix. Fixed by
 *   keeping a dedicated screen-ortho matrix updated on resize.
 *
 * FIX 4 — justTouched() multi-touch false positives:
 *   Replaced Gdx.input.justTouched() (which fires for ANY pointer) with
 *   per-pointer wasTouched tracking so only the first frame of a new touch
 *   triggers one-shot button presses.
 */
public class AndroidControllerSystem extends IteratingSystem {

    // ── Public static outputs (read by MovementSystem) ────────────────────────
    public static float   moveX            = 0f;
    public static float   moveY            = 0f;
    public static boolean attackPressed    = false;
    public static boolean attackHeld       = false;
    public static boolean interactPressed  = false;
    public static boolean inventoryPressed = false;

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int   MAX_POINTERS    = 5;
    private static final float JOYSTICK_RADIUS = 60f;
    private static final float JOYSTICK_THUMB  = 20f;
    private static final float BUTTON_RADIUS   = 30f;
    private static final float MARGIN          = 20f;

    private static final Color JOYSTICK_BG     = new Color(1f, 1f, 1f, 0.15f);
    private static final Color JOYSTICK_THUMB_C= new Color(1f, 1f, 1f, 0.50f);
    private static final Color BTN_ATTACK      = new Color(1f, 0.3f, 0.3f, 0.35f);
    private static final Color BTN_INTERACT    = new Color(0.3f, 0.7f, 1f, 0.35f);
    private static final Color BTN_INVENTORY   = new Color(0.4f, 1f, 0.4f, 0.35f);
    private static final Color BTN_LABEL       = new Color(1f, 1f, 1f, 0.80f);

    private final boolean isAndroid;

    private ShapeRenderer shapeRenderer;
    private SpriteBatch   uiBatch;
    private BitmapFont    labelFont;

    // FIX 3: dedicated screen-space ortho matrix (not the game camera)
    private final Matrix4 screenMatrix = new Matrix4();

    // Button centres in screen px
    private float joyX, joyY;
    private float atkX, atkY;
    private float intX, intY;
    private float invX, invY;

    // FIX 1: joystick tracks its own pointer separately
    private int   joyPointer = -1;
    private float thumbX, thumbY;

    private int screenW, screenH;

    // FIX 4: previous-frame touch state per pointer for one-shot detection
    private final boolean[] wasTouched = new boolean[MAX_POINTERS];

    public AndroidControllerSystem() {
        super(Aspect.all(PlayerComponent.class));
        isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;
    }

    @Override
    protected void initialize() {
        if (!isAndroid) return;
        shapeRenderer = new ShapeRenderer();
        uiBatch       = new SpriteBatch();
        labelFont     = new BitmapFont();
        labelFont.setColor(BTN_LABEL);
        recalcPositions(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int w, int h) {
        if (!isAndroid) return;
        recalcPositions(w, h);
    }

    private void recalcPositions(int w, int h) {
        screenW = w;
        screenH = h;

        // FIX 3: update screen-space ortho matrix
        screenMatrix.setToOrtho2D(0, 0, w, h);

        joyX = MARGIN + JOYSTICK_RADIUS;
        joyY = MARGIN + JOYSTICK_RADIUS;
        thumbX = joyX;
        thumbY = joyY;

        atkX = w - MARGIN - BUTTON_RADIUS;
        atkY = MARGIN + BUTTON_RADIUS;

        intX = atkX;
        intY = atkY + BUTTON_RADIUS * 2f + 20f;

        invX = w - MARGIN - BUTTON_RADIUS;
        invY = h - MARGIN - BUTTON_RADIUS;
    }

    @Override
    protected void process(int entityId) {
        if (!isAndroid) return;

        // Reset one-shot flags
        attackPressed    = false;
        interactPressed  = false;
        inventoryPressed = false;
        attackHeld       = false;
        moveX            = 0f;
        moveY            = 0f;

        handleTouchInput();
        drawControls();
    }

    private void handleTouchInput() {
        // ── PASS 1: Joystick ─────────────────────────────────────────────────
        // Check if the joystick's locked pointer was released
        if (joyPointer >= 0 && !Gdx.input.isTouched(joyPointer)) {
            joyPointer = -1;
            thumbX = joyX;
            thumbY = joyY;
        }

        for (int i = 0; i < MAX_POINTERS; i++) {
            if (!Gdx.input.isTouched(i)) continue;

            float tx = Gdx.input.getX(i);
            float ty = screenH - Gdx.input.getY(i); // flip Y

            float djx  = tx - joyX;
            float djy  = ty - joyY;
            float dist = (float) Math.sqrt(djx * djx + djy * djy);

            // Claim this pointer for the joystick if it's in range and unclaimed
            if (dist <= JOYSTICK_RADIUS * 1.5f && (joyPointer == -1 || joyPointer == i)) {
                joyPointer = i;
                float clamp = Math.min(dist, JOYSTICK_RADIUS);
                if (dist > 0.001f) {
                    float nx = djx / dist;
                    float ny = djy / dist;
                    thumbX = joyX + nx * clamp;
                    thumbY = joyY + ny * clamp;
                    // FIX 2: normalise so diagonal isn't √2× faster
                    float len = Math.min(dist / JOYSTICK_RADIUS, 1f);
                    moveX = nx * len;
                    moveY = ny * len;
                }
                break; // joystick consumes only one pointer
            }
        }

        // ── PASS 2: Buttons (all non-joystick pointers) ───────────────────────
        for (int i = 0; i < MAX_POINTERS; i++) {
            if (i == joyPointer) continue; // skip joystick pointer

            boolean touched = Gdx.input.isTouched(i);
            boolean justTouchedThisPointer = touched && !wasTouched[i]; // FIX 4
            wasTouched[i] = touched;

            if (!touched) continue;

            float tx = Gdx.input.getX(i);
            float ty = screenH - Gdx.input.getY(i);

            // Held state for auto-fire
            if (inCircle(tx, ty, atkX, atkY, BUTTON_RADIUS)) {
                attackHeld = true;
            }

            // FIX 4: one-shot press only on first frame of contact
            if (justTouchedThisPointer) {
                if (inCircle(tx, ty, atkX, atkY, BUTTON_RADIUS)) {
                    attackPressed = true;
                } else if (inCircle(tx, ty, intX, intY, BUTTON_RADIUS)) {
                    interactPressed = true;
                } else if (inCircle(tx, ty, invX, invY, BUTTON_RADIUS)) {
                    inventoryPressed = true;
                }
            }
        }
    }

    private void drawControls() {
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        // FIX 3: use screen-space matrix, NOT the game camera
        shapeRenderer.setProjectionMatrix(screenMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(JOYSTICK_BG);
        shapeRenderer.circle(joyX, joyY, JOYSTICK_RADIUS, 32);

        shapeRenderer.setColor(JOYSTICK_THUMB_C);
        shapeRenderer.circle(thumbX, thumbY, JOYSTICK_THUMB, 16);

        shapeRenderer.setColor(BTN_ATTACK);
        shapeRenderer.circle(atkX, atkY, BUTTON_RADIUS, 24);

        shapeRenderer.setColor(BTN_INTERACT);
        shapeRenderer.circle(intX, intY, BUTTON_RADIUS, 24);

        shapeRenderer.setColor(BTN_INVENTORY);
        shapeRenderer.circle(invX, invY, BUTTON_RADIUS, 24);

        shapeRenderer.end();

        // FIX 3: uiBatch also uses screen-space matrix
        uiBatch.setProjectionMatrix(screenMatrix);
        uiBatch.begin();
        labelFont.draw(uiBatch, "ATT", atkX - 12f, atkY + 5f);
        labelFont.draw(uiBatch, "F",   intX -  4f, intY + 5f);
        labelFont.draw(uiBatch, "INV", invX - 12f, invY + 5f);
        uiBatch.end();

        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
    }

    private static boolean inCircle(float px, float py, float cx, float cy, float r) {
        float dx = px - cx, dy = py - cy;
        return dx * dx + dy * dy <= r * r;
    }

    @Override
    protected void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (uiBatch       != null) uiBatch.dispose();
        if (labelFont     != null) labelFont.dispose();
    }
}
