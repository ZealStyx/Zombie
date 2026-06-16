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
import com.badlogic.gdx.math.Vector2;

import io.github.zom.component.CombatComponent;
import io.github.zom.component.PlayerComponent;

/**
 * On-screen touch controls for Android devices, rendered with ShapeRenderer.
 *
 * Layout:
 *   - Virtual joystick (bottom-left)
 *   - Attack button (bottom-right)
 *   - Interact button (bottom-right, above attack)
 *   - Inventory button (top-right)
 *
 * On desktop this system is a no-op (isAndroid = false).
 *
 * Other systems read the static output fields to get touch-derived input:
 *   AndroidControllerSystem.moveX / moveY   → movement direction
 *   AndroidControllerSystem.attackPressed    → melee/ranged trigger
 *   AndroidControllerSystem.interactPressed  → F-key equivalent
 *   AndroidControllerSystem.inventoryPressed → Tab-key equivalent
 */
public class AndroidControllerSystem extends IteratingSystem {

    // ── Public static outputs (read by MovementSystem, CombatSystem, etc.) ──
    public static float moveX = 0f;
    public static float moveY = 0f;
    public static boolean attackPressed    = false;
    public static boolean attackHeld       = false;
    public static boolean interactPressed  = false;
    public static boolean inventoryPressed = false;

    // ── Config ───────────────────────────────────────────────────────────────

    private static final float JOYSTICK_RADIUS  = 60f;   // outer circle radius (screen px)
    private static final float JOYSTICK_THUMB   = 20f;   // inner thumb radius
    private static final float BUTTON_RADIUS    = 30f;   // action button radius
    private static final float MARGIN           = 20f;
    private static final Color JOYSTICK_BG      = new Color(1f, 1f, 1f, 0.15f);
    private static final Color JOYSTICK_THUMB_C = new Color(1f, 1f, 1f, 0.5f);
    private static final Color BTN_ATTACK       = new Color(1f, 0.3f, 0.3f, 0.35f);
    private static final Color BTN_INTERACT     = new Color(0.3f, 0.7f, 1f, 0.35f);
    private static final Color BTN_INVENTORY    = new Color(0.4f, 1f, 0.4f, 0.35f);
    private static final Color BTN_LABEL        = new Color(1f, 1f, 1f, 0.8f);

    private final boolean isAndroid;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch uiBatch;
    private BitmapFont labelFont;

    // Screen-space positions (recalculated on resize)
    private float joyX, joyY;         // joystick centre
    private float atkX, atkY;         // attack button centre
    private float intX, intY;         // interact button centre
    private float invX, invY;         // inventory button centre

    // Joystick drag state
    private int joyPointer = -1;      // touch pointer id
    private float thumbX, thumbY;     // current thumb position

    private int screenW, screenH;

    public AndroidControllerSystem() {
        super(Aspect.all(PlayerComponent.class));
        isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;
    }

    @Override
    protected void initialize() {
        if (!isAndroid) return;
        shapeRenderer = new ShapeRenderer();
        uiBatch = new SpriteBatch();
        // Use default font for labels — lightweight
        labelFont = new BitmapFont();
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
        // Joystick bottom-left
        joyX = MARGIN + JOYSTICK_RADIUS;
        joyY = MARGIN + JOYSTICK_RADIUS;
        thumbX = joyX;
        thumbY = joyY;

        // Attack bottom-right
        atkX = w - MARGIN - BUTTON_RADIUS;
        atkY = MARGIN + BUTTON_RADIUS;

        // Interact above attack
        intX = atkX;
        intY = atkY + BUTTON_RADIUS * 2f + 20f;

        // Inventory top-right
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

        handleTouchInput();
        drawControls();
    }

    private void handleTouchInput() {
        moveX = 0f;
        moveY = 0f;
        attackHeld = false;

        for (int i = 0; i < 5; i++) {
            if (!Gdx.input.isTouched(i)) {
                if (i == joyPointer) {
                    joyPointer = -1;
                    thumbX = joyX;
                    thumbY = joyY;
                }
                continue;
            }

            float tx = Gdx.input.getX(i);
            float ty = screenH - Gdx.input.getY(i); // flip Y for screen coords

            // Check joystick
            float djx = tx - joyX;
            float djy = ty - joyY;
            float distJoy = (float) Math.sqrt(djx * djx + djy * djy);
            if (distJoy <= JOYSTICK_RADIUS * 1.5f && (joyPointer == -1 || joyPointer == i)) {
                joyPointer = i;
                float clampDist = Math.min(distJoy, JOYSTICK_RADIUS);
                if (distJoy > 0.001f) {
                    thumbX = joyX + (djx / distJoy) * clampDist;
                    thumbY = joyY + (djy / distJoy) * clampDist;
                    moveX = djx / JOYSTICK_RADIUS;
                    moveY = djy / JOYSTICK_RADIUS;
                    // Clamp to -1..1
                    float len = (float) Math.sqrt(moveX * moveX + moveY * moveY);
                    if (len > 1f) { moveX /= len; moveY /= len; }
                }
                continue;
            }

            // Check if attack button is held
            if (inCircle(tx, ty, atkX, atkY, BUTTON_RADIUS)) {
                attackHeld = true;
            }

            // Check button presses (just pressed check)
            if (Gdx.input.justTouched()) {
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

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Joystick background
        shapeRenderer.setColor(JOYSTICK_BG);
        shapeRenderer.circle(joyX, joyY, JOYSTICK_RADIUS, 32);

        // Joystick thumb
        shapeRenderer.setColor(JOYSTICK_THUMB_C);
        shapeRenderer.circle(thumbX, thumbY, JOYSTICK_THUMB, 16);

        // Attack button
        shapeRenderer.setColor(BTN_ATTACK);
        shapeRenderer.circle(atkX, atkY, BUTTON_RADIUS, 24);

        // Interact button
        shapeRenderer.setColor(BTN_INTERACT);
        shapeRenderer.circle(intX, intY, BUTTON_RADIUS, 24);

        // Inventory button
        shapeRenderer.setColor(BTN_INVENTORY);
        shapeRenderer.circle(invX, invY, BUTTON_RADIUS, 24);

        shapeRenderer.end();

        // Draw labels
        uiBatch.begin();
        labelFont.draw(uiBatch, "ATT", atkX - 12f, atkY + 5f);
        labelFont.draw(uiBatch, "F",   intX - 4f,  intY + 5f);
        labelFont.draw(uiBatch, "INV", invX - 12f, invY + 5f);
        uiBatch.end();

        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
    }

    private static boolean inCircle(float px, float py, float cx, float cy, float r) {
        float dx = px - cx;
        float dy = py - cy;
        return dx * dx + dy * dy <= r * r;
    }

    @Override
    protected void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (uiBatch != null) uiBatch.dispose();
        if (labelFont != null) labelFont.dispose();
    }
}
