package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import io.github.zom.component.AnimationStateComponent;
import io.github.zom.component.CombatComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.TransformComponent;

/**
 * Reads WASD / arrow-key input → updates player position, direction, animation pose.
 * Also reads mouse buttons and sets CombatComponent request flags for CombatSystem.
 * Centers the camera on the player each frame.
 *
 * Android note: meleeRequested/rangedRequested flags are set here from mouse input
 * for desktop; the same flags will be set by on-screen buttons on Android.
 */
public class MovementSystem extends IteratingSystem {

    private ComponentMapper<PlayerComponent>         mPlayer;
    private ComponentMapper<TransformComponent>      mTransform;
    private ComponentMapper<AnimationStateComponent> mAnim;
    private ComponentMapper<CombatComponent>         mCombat;

    private final OrthographicCamera camera;
    private final Vector2            moveDir = new Vector2();

    public MovementSystem(OrthographicCamera camera) {
        super(Aspect.all(
            PlayerComponent.class,
            TransformComponent.class,
            AnimationStateComponent.class
        ));
        this.camera = camera;
    }

    @Override
    protected void process(int entityId) {
        PlayerComponent         player    = mPlayer.get(entityId);
        TransformComponent      transform = mTransform.get(entityId);
        AnimationStateComponent anim      = mAnim.get(entityId);

        // ── Combat input ─────────────────────────────────────────────────────
        if (mCombat.has(entityId)) {
            CombatComponent combat = mCombat.get(entityId);
            boolean isAndroid = Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android;
            if (isAndroid) {
                combat.isAutoFiring = AndroidControllerSystem.attackHeld && combat.hasRanged;
                if (AndroidControllerSystem.attackPressed) {
                    if (combat.hasRanged) {
                        combat.rangedRequested = true;
                    } else {
                        combat.meleeRequested = true;
                    }
                }
            } else {
                combat.isAutoFiring = Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && combat.hasRanged;
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    combat.meleeRequested = true;
                }
                if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT) && combat.hasRanged) {
                    combat.rangedRequested = true;
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    combat.reloadRequested = true;
                }
            }
        }

        // ── Movement input ────────────────────────────────────────────────────
        moveDir.setZero();
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    moveDir.y += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))  moveDir.y -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))  moveDir.x -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveDir.x += 1f;

        boolean moving = !moveDir.isZero();

        if (moving && !anim.locked) {
            moveDir.nor();
            float dist = player.speed * world.getDelta();
            transform.x += moveDir.x * dist;
            transform.y += moveDir.y * dist;

            if (Math.abs(moveDir.y) >= Math.abs(moveDir.x)) {
                transform.direction = moveDir.y > 0f ? "up" : "down";
            } else {
                transform.direction = moveDir.x > 0f ? "right" : "left";
            }
            anim.setPose("run");
        } else if (!anim.locked) {
            anim.setPose("idle");
        }

        // ── Camera follow ─────────────────────────────────────────────────────
        camera.position.set(
            transform.x + transform.w * 0.5f,
            transform.y + transform.h * 0.5f,
            0f
        );
    }
}
