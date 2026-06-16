package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.systems.IteratingSystem;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.math.Rectangle;

import io.github.zom.component.AnimationStateComponent;
import io.github.zom.component.CollisionComponent;
import io.github.zom.component.CombatComponent;
import io.github.zom.component.HealthComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.component.ZedComponent;

/**
 * Processes player attack requests (set by MovementSystem from mouse/touch input).
 *
 * Melee  — builds a hit rectangle in front of the player, tests it against zed
 *           headBox first (headshot), then bodyBox. Queues damage into HealthComponent.
 *
 * Ranged — hitscan ray from player centre in facing direction, steps 4px at a time,
 *           tests zed headBox first then bodyBox, stops at first hit or rangedRange.
 *
 * All distances in pixels (PPU=1).
 */
public class CombatSystem extends IteratingSystem {

    private ComponentMapper<PlayerComponent>         mPlayer;
    private ComponentMapper<TransformComponent>      mTransform;
    private ComponentMapper<CollisionComponent>      mCollision;
    private ComponentMapper<AnimationStateComponent> mAnim;
    private ComponentMapper<CombatComponent>         mCombat;
    private ComponentMapper<HealthComponent>         mHealth;
    private ComponentMapper<ZedComponent>            mZed;

    private EntitySubscription zedSub;

    public CombatSystem() {
        super(Aspect.all(
            PlayerComponent.class,
            TransformComponent.class,
            CollisionComponent.class,
            AnimationStateComponent.class,
            CombatComponent.class
        ));
    }

    @Override
    protected void initialize() {
        zedSub = world.getAspectSubscriptionManager()
            .get(Aspect.all(ZedComponent.class, CollisionComponent.class, HealthComponent.class));
    }

    @Override
    protected void process(int entityId) {
        CombatComponent         combat    = mCombat.get(entityId);
        TransformComponent      transform = mTransform.get(entityId);
        AnimationStateComponent anim      = mAnim.get(entityId);
        float delta = world.getDelta();

        // Tick cooldown timers
        combat.meleeTimer  = Math.max(0f, combat.meleeTimer  - delta);
        combat.rangedTimer = Math.max(0f, combat.rangedTimer - delta);

        // ── Melee ─────────────────────────────────────────────────────────────
        if (combat.meleeRequested) {
            combat.meleeRequested = false;
            if (combat.meleeTimer <= 0f) {
                combat.meleeTimer = combat.meleeCooldown;
                anim.playOnce("axe", combat.meleeCooldown);
                doMelee(transform, combat);
            }
        }

        // ── Ranged ────────────────────────────────────────────────────────────
        if (combat.rangedRequested && combat.hasRanged) {
            combat.rangedRequested = false;
            if (combat.rangedTimer <= 0f) {
                combat.rangedTimer = combat.rangedCooldown;
                // pose depends on weapon type; "pistol" for secondary, "twohand" for primary
                anim.playOnce("pistol", combat.rangedCooldown);
                doRanged(transform, combat);
            }
        }
    }

    // ── Melee implementation ─────────────────────────────────────────────────

    private void doMelee(TransformComponent t, CombatComponent combat) {
        float cx = t.x + t.w * 0.5f;
        float cy = t.y + t.h * 0.5f;

        // Build hit rect in front of player based on facing direction
        float range = combat.meleeRange;
        float hw    = combat.meleeHalfWidth;

        Rectangle hitRect = buildHitRect(cx, cy, t.direction, range, hw);

        IntBag zeds = zedSub.getEntities();
        int[]  data = zeds.getData();
        for (int i = 0, n = zeds.size(); i < n; i++) {
            int zId = data[i];
            ZedComponent zed = mZed.get(zId);
            if (!zed.alive) continue;

            CollisionComponent col    = mCollision.get(zId);
            HealthComponent    health = mHealth.get(zId);

            if (hitRect.overlaps(col.headBox)) {
                health.queueDamage(combat.meleeDamage, true);
            } else if (hitRect.overlaps(col.bodyBox)) {
                health.queueDamage(combat.meleeDamage, false);
            }
        }
    }

    private static Rectangle buildHitRect(float cx, float cy, String dir,
                                          float range, float hw) {
        switch (dir) {
            case "up":    return new Rectangle(cx - hw, cy,         hw * 2f, range);
            case "down":  return new Rectangle(cx - hw, cy - range, hw * 2f, range);
            case "right": return new Rectangle(cx,       cy - hw,  range,    hw * 2f);
            case "left":  return new Rectangle(cx - range, cy - hw, range,   hw * 2f);
            default:      return new Rectangle(cx - hw, cy,         hw * 2f, range);
        }
    }

    // ── Ranged / hitscan implementation ──────────────────────────────────────

    private void doRanged(TransformComponent t, CombatComponent combat) {
        float cx = t.x + t.w * 0.5f;
        float cy = t.y + t.h * 0.5f;

        float dx = 0f, dy = 0f;
        switch (t.direction) {
            case "up":    dy =  1f; break;
            case "down":  dy = -1f; break;
            case "right": dx =  1f; break;
            case "left":  dx = -1f; break;
        }

        float step       = 4f;  // px per sample
        int   steps      = (int)(combat.rangedRange / step);
        float rx = cx, ry = cy;

        IntBag zeds = zedSub.getEntities();
        int[]  data = zeds.getData();

        for (int s = 0; s < steps; s++) {
            rx += dx * step;
            ry += dy * step;

            for (int i = 0, n = zeds.size(); i < n; i++) {
                int zId = data[i];
                ZedComponent zed = mZed.get(zId);
                if (!zed.alive) continue;

                CollisionComponent col    = mCollision.get(zId);
                HealthComponent    health = mHealth.get(zId);

                if (col.headBox.contains(rx, ry)) {
                    health.queueDamage(combat.rangedDamage, true);
                    return; // hitscan stops at first hit
                }
                if (col.bodyBox.contains(rx, ry)) {
                    health.queueDamage(combat.rangedDamage, false);
                    return;
                }
            }
        }
    }
}
