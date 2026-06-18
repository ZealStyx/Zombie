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
 * CHANGE (Section 2 / 5.5): Removed the "F" interact button and the
 *   interactPressed static flag. Item pickup is now handled via the
 *   Nearby Loot panel in InventoryUiSystem (drag-from-panel). The
 *   intX/intY fields, BTN_INTERACT color, and all interactPressed
 *   references are gone.
 */
public class AndroidControllerSystem extends IteratingSystem {

    // ── Public static outputs (read by MovementSystem / InventoryUiSystem) ───
    public static float   moveX            = 0f;
    public static float   moveY            = 0f;
    public static boolean attackPressed    = false;
    public static boolean attackHeld       = false;
    // interactPressed REMOVED — pickup now via Nearby Loot panel drag
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
    private static final Color BTN_INVENTORY   = new Color(0.4f, 1f, 0.4f, 0.35f);
    private static final Color BTN_LABEL       = new Color(1f, 1f, 1f, 0.80f);

    private final boolean isAndroid;

    private ShapeRenderer shapeRenderer;
    private SpriteBatch   uiBatch;
    private BitmapFont    labelFont;

    private final Matrix4 screenMatrix = new Matrix4();

    // Button centres in screen px
    private float joyX, joyY;
    private float atkX, atkY;
    private float invX, invY;

    // Joystick tracks its own pointer separately
    private int   joyPointer = -1;
    private float thumbX, thumbY;

    private int screenW, screenH;

    // Previous-frame touch state per pointer for one-shot detection
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

        screenMatrix.setToOrtho2D(0, 0, w, h);

        joyX = MARGIN + JOYSTICK_RADIUS;
        joyY = MARGIN + JOYSTICK_RADIUS;
        thumbX = joyX;
        thumbY = joyY;

        atkX = w - MARGIN - BUTTON_RADIUS;
        atkY = MARGIN + BUTTON_RADIUS;

        // INV button: top-right corner (no interact button below it anymore)
        invX = w - MARGIN - BUTTON_RADIUS;
        invY = h - MARGIN - BUTTON_RADIUS;
    }

    @Override
    protected void process(int entityId) {
        if (!isAndroid) return;

        // Reset one-shot flags
        attackPressed    = false;
        inventoryPressed = false;
        attackHeld       = false;
        moveX            = 0f;
        moveY            = 0f;

        handleTouchInput();
        drawControls();
    }

    private void handleTouchInput() {
        // ── PASS 1: Joystick ─────────────────────────────────────────────────
        if (joyPointer >= 0 && !Gdx.input.isTouched(joyPointer)) {
            joyPointer = -1;
            thumbX = joyX;
            thumbY = joyY;
        }

        for (int i = 0; i < MAX_POINTERS; i++) {
            if (!Gdx.input.isTouched(i)) continue;

            float tx = Gdx.input.getX(i);
            float ty = screenH - Gdx.input.getY(i);

            float djx  = tx - joyX;
            float djy  = ty - joyY;
            float dist = (float) Math.sqrt(djx * djx + djy * djy);

            if (dist <= JOYSTICK_RADIUS * 1.5f && (joyPointer == -1 || joyPointer == i)) {
                joyPointer = i;
                float clamp = Math.min(dist, JOYSTICK_RADIUS);
                if (dist > 0.001f) {
                    float nx = djx / dist;
                    float ny = djy / dist;
                    thumbX = joyX + nx * clamp;
                    thumbY = joyY + ny * clamp;
                    float len = Math.min(dist / JOYSTICK_RADIUS, 1f);
                    moveX = nx * len;
                    moveY = ny * len;
                }
                break;
            }
        }

        // ── PASS 2: Buttons (all non-joystick pointers) ───────────────────────
        for (int i = 0; i < MAX_POINTERS; i++) {
            if (i == joyPointer) continue;

            boolean touched = Gdx.input.isTouched(i);
            boolean justTouchedThisPointer = touched && !wasTouched[i];
            wasTouched[i] = touched;

            if (!touched) continue;

            float tx = Gdx.input.getX(i);
            float ty = screenH - Gdx.input.getY(i);

            if (inCircle(tx, ty, atkX, atkY, BUTTON_RADIUS)) {
                attackHeld = true;
            }

            if (justTouchedThisPointer) {
                if (inCircle(tx, ty, atkX, atkY, BUTTON_RADIUS)) {
                    attackPressed = true;
                } else if (inCircle(tx, ty, invX, invY, BUTTON_RADIUS)) {
                    inventoryPressed = true;
                }
                // No interact/F button anymore
            }
        }
    }

    private void drawControls() {
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(screenMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(JOYSTICK_BG);
        shapeRenderer.circle(joyX, joyY, JOYSTICK_RADIUS, 32);

        shapeRenderer.setColor(JOYSTICK_THUMB_C);
        shapeRenderer.circle(thumbX, thumbY, JOYSTICK_THUMB, 16);

        shapeRenderer.setColor(BTN_ATTACK);
        shapeRenderer.circle(atkX, atkY, BUTTON_RADIUS, 24);

        shapeRenderer.setColor(BTN_INVENTORY);
        shapeRenderer.circle(invX, invY, BUTTON_RADIUS, 24);

        shapeRenderer.end();

        uiBatch.setProjectionMatrix(screenMatrix);
        uiBatch.begin();
        labelFont.draw(uiBatch, "ATT", atkX - 12f, atkY + 5f);
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
