package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;

import io.github.zom.component.AnimationStateComponent;
import io.github.zom.component.CombatComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDef;

/**
 * WASD / joystick → player position, direction, animation pose.
 * Also sets CombatComponent request flags from mouse/touch.
 *
 * FIX: When idle and not locked, determine the correct stance pose from the
 * equipped weapon type ("twohand" for primary, "pistol" for secondary, "axe"
 * for melee) instead of always reverting to "idle". Fixes the bug where hands
 * appear at the sides while holding a weapon.
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

        boolean isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;

        // ── Combat input ──────────────────────────────────────────────────────
        if (mCombat.has(entityId)) {
            CombatComponent combat = mCombat.get(entityId);
            if (isAndroid) {
                combat.isAutoFiring = AndroidControllerSystem.attackHeld && combat.hasRanged;
                if (AndroidControllerSystem.attackPressed) {
                    if (combat.hasRanged) combat.rangedRequested = true;
                    else                  combat.meleeRequested  = true;
                }
            } else {
                combat.isAutoFiring = Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && combat.hasRanged;
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))                    combat.meleeRequested  = true;
                if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT) && combat.hasRanged) combat.rangedRequested = true;
                if (Gdx.input.isKeyJustPressed(Input.Keys.R))                             combat.reloadRequested = true;
            }
        }

        // ── Movement input ────────────────────────────────────────────────────
        moveDir.setZero();
        if (isAndroid) {
            moveDir.x += AndroidControllerSystem.moveX;
            moveDir.y += AndroidControllerSystem.moveY;
        } else {
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    moveDir.y += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))  moveDir.y -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))  moveDir.x -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveDir.x += 1f;
        }

        boolean moving = moveDir.len() > 0.05f;

        if (moving && !anim.locked) {
            if (moveDir.len() > 1f) moveDir.nor();
            float dist = player.speed * world.getDelta();
            transform.x += moveDir.x * dist;
            transform.y += moveDir.y * dist;

            if (Math.abs(moveDir.y) >= Math.abs(moveDir.x))
                transform.direction = moveDir.y > 0f ? "up" : "down";
            else
                transform.direction = moveDir.x > 0f ? "right" : "left";

            anim.setPose("run");

        } else if (!anim.locked) {
            // FIX: use weapon-specific stance pose instead of always "idle"
            anim.setPose(resolveIdlePose(player));
        }

        // ── Camera follow ─────────────────────────────────────────────────────
        camera.position.set(
            transform.x + transform.w * 0.5f,
            transform.y + transform.h * 0.5f,
            0f
        );
    }

    /**
     * Returns the correct idle/stance pose name for the player's equipped weapon.
     * "twohand" → primary rifles/shotguns
     * "pistol"  → secondary handguns
     * "axe"     → melee weapons
     * "idle"    → unarmed
     */
    private static String resolveIdlePose(PlayerComponent player) {
        if (player.heldItemId <= 0) return "idle";
        ItemDef def = ConfigLoader.getItemDatabase().get(player.heldItemId);
        if (def == null) return "idle";
        switch (def.type) {
            case "primary":   return "twohand";
            case "secondary": return "pistol";
            case "melee":     return "axe";
            default:          return "idle";
        }
    }
}
