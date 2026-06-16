package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.systems.IteratingSystem;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import io.github.zom.component.AnimationStateComponent;
import io.github.zom.component.CollisionComponent;
import io.github.zom.component.HealthComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.component.ZedAIComponent;
import io.github.zom.component.ZedAIComponent.State;
import io.github.zom.component.ZedComponent;
import io.github.zom.world.WorldCollision;

/**
 * Zed AI state machine: WANDER → CHASE → ATTACK (and back).
 *
 * Steering behaviors (no A*, no navigation grid):
 *   WANDER  — drift in a random direction; change every wanderInterval seconds
 *   CHASE   — normalize vector to player, move at full speed, face player
 *   ATTACK  — stop, play attack animation on cooldown, deal damage via HealthComponent
 *
 * Obstacle avoidance: given a candidate move, test the feet box at the new
 * position. If blocked, try perpendicular (CW then CCW). If both blocked, stand still.
 */
public class ZedAISystem extends IteratingSystem {

    private ComponentMapper<ZedComponent>            mZed;
    private ComponentMapper<ZedAIComponent>          mAI;
    private ComponentMapper<TransformComponent>      mTransform;
    private ComponentMapper<AnimationStateComponent> mAnim;
    private ComponentMapper<HealthComponent>         mHealth;
    private ComponentMapper<CollisionComponent>      mCollision;
    private ComponentMapper<PlayerComponent>         mPlayer;

    private EntitySubscription playerSub;
    private EntitySubscription zedSub;

    public ZedAISystem() {
        super(Aspect.all(
            ZedComponent.class,
            ZedAIComponent.class,
            TransformComponent.class,
            AnimationStateComponent.class,
            HealthComponent.class
        ));
    }

    @Override
    protected void initialize() {
        playerSub = world.getAspectSubscriptionManager()
            .get(Aspect.all(PlayerComponent.class, TransformComponent.class, HealthComponent.class));
        zedSub = world.getAspectSubscriptionManager()
            .get(Aspect.all(ZedComponent.class, TransformComponent.class));
    }

    @Override
    protected void process(int entityId) {
        ZedComponent            zed       = mZed.get(entityId);
        ZedAIComponent          ai        = mAI.get(entityId);
        TransformComponent      transform = mTransform.get(entityId);
        AnimationStateComponent anim      = mAnim.get(entityId);
        HealthComponent         health    = mHealth.get(entityId);

        if (!zed.alive || health.dead) return;

        float delta = world.getDelta();

        // ── Find nearest player ───────────────────────────────────────────────
        int    playerId  = findPlayer();
        float  distToPlayer = Float.MAX_VALUE;
        float  toDirX = 0f, toDirY = 0f;
        float  px = 0f, py = 0f;

        if (playerId >= 0) {
            TransformComponent pt = mTransform.get(playerId);
            px = pt.x + pt.w * 0.5f;
            py = pt.y + pt.h * 0.5f;
            float zx = transform.x + transform.w * 0.5f;
            float zy = transform.y + transform.h * 0.5f;
            float dx = px - zx, dy = py - zy;
            distToPlayer = (float) Math.sqrt(dx * dx + dy * dy);
            if (distToPlayer > 0f) {
                toDirX = dx / distToPlayer;
                toDirY = dy / distToPlayer;
            }
            ai.lastPlayerX = px;
            ai.lastPlayerY = py;
        }

        // ── State machine ─────────────────────────────────────────────────────
        switch (ai.state) {
            case IDLE:
                anim.setPose("idle");
                break;

            case WANDER:
                doWander(ai, transform, anim, delta);
                if (playerId >= 0 && isInVisionCone(transform, px, py, ai.detectionRange)) {
                    ai.state = State.CHASE;
                }
                break;

            case CHASE:
                doChase(ai, transform, anim, delta, toDirX, toDirY);
                if (distToPlayer < ai.attackRange) {
                    ai.state = State.ATTACK;
                } else if (distToPlayer > ai.deaggroRange) {
                    ai.state = State.WANDER;
                }
                break;

            case ATTACK:
                doAttack(ai, anim, delta, playerId, distToPlayer);
                if (distToPlayer > ai.attackRange * 1.5f) {
                    ai.state = distToPlayer < ai.deaggroRange ? State.CHASE : State.WANDER;
                }
                break;
        }
    }

    private boolean isInVisionCone(TransformComponent transform, float px, float py, float detectionRange) {
        float zx = transform.x + transform.w * 0.5f;
        float zy = transform.y + transform.h * 0.5f;
        float dx = px - zx;
        float dy = py - zy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > detectionRange) return false;

        float facingAngle = directionToAngle(transform.direction);
        float angleToPlayer = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        if (angleToPlayer < 0f) {
            angleToPlayer += 360f;
        }

        float diff = Math.abs(angleToPlayer - facingAngle);
        if (diff > 180f) {
            diff = 360f - diff;
        }

        return diff <= 45f;
    }

    private static float directionToAngle(String direction) {
        if (direction == null) return 270f;
        switch (direction) {
            case "up":    return 90f;
            case "down":  return 270f;
            case "left":  return 180f;
            case "right": return 0f;
            default:      return 270f;
        }
    }

    // ── WANDER ────────────────────────────────────────────────────────────────

    private void doWander(ZedAIComponent ai, TransformComponent t,
                          AnimationStateComponent anim, float delta) {
        ai.wanderTimer += delta;
        if (ai.wanderTimer >= ai.wanderInterval) {
            ai.wanderTimer = 0f;
            float angle = MathUtils.random(0f, MathUtils.PI2);
            ai.wanderDirX = MathUtils.cos(angle);
            ai.wanderDirY = MathUtils.sin(angle);
        }

        float speed  = ai.speed * 0.4f;
        float moveX  = ai.wanderDirX * speed * delta;
        float moveY  = ai.wanderDirY * speed * delta;

        applyMoveWithAvoidance(t, moveX, moveY);
        setFacingFromDir(t, ai.wanderDirX, ai.wanderDirY);
        if (!anim.locked) anim.setPose("walk");
    }

    // ── CHASE ─────────────────────────────────────────────────────────────────

    private void doChase(ZedAIComponent ai, TransformComponent t,
                         AnimationStateComponent anim, float delta,
                         float toDirX, float toDirY) {
        float moveX = toDirX * ai.speed * delta;
        float moveY = toDirY * ai.speed * delta;

        applyMoveWithAvoidance(t, moveX, moveY);
        setFacingFromDir(t, toDirX, toDirY);
        if (!anim.locked) anim.setPose("run");
    }

    // ── ATTACK ────────────────────────────────────────────────────────────────

    private void doAttack(ZedAIComponent ai, AnimationStateComponent anim,
                          float delta, int playerId, float distToPlayer) {
        ai.attackTimer += delta;
        if (!anim.locked) anim.playOnce("attack", 0.6f);

        if (ai.attackTimer >= ai.attackCooldown) {
            ai.attackTimer = 0f;
            if (playerId >= 0 && distToPlayer <= ai.attackRange * 1.2f) {
                HealthComponent playerHealth = mHealth.get(playerId);
                if (playerHealth != null && !playerHealth.dead) {
                    playerHealth.queueDamage(ai.attackDamage, false);
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Move by (dx, dy). If the feet box at the new position overlaps a world obstacle,
     * try perpendicular directions (CW then CCW). If both blocked, don't move.
     */
    private void applyMoveWithAvoidance(TransformComponent t, float dx, float dy) {
        if (tryMove(t, dx, dy)) return;

        // Perpendicular CW: (dy, -dx)
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0f) return;
        float nx = dy / len, ny = -dx / len;
        if (tryMove(t, nx * len, ny * len)) return;

        // Perpendicular CCW: (-dy, dx)
        tryMove(t, -nx * len, -ny * len);
    }

    private boolean tryMove(TransformComponent t, float dx, float dy) {
        float nx = t.x + dx;
        float ny = t.y + dy;

        // Cheap feet-box test at new position (use same proportions as CollisionComponent)
        float feetInset = 0.25f;
        float feetH     = t.h * 0.20f;
        float fx = nx + feetInset * t.w;
        float fy = ny;
        float fw = t.w * (1f - feetInset * 2f);

        Array<Rectangle> obstacles = WorldCollision.getObstacles();
        for (int i = 0, n = obstacles.size; i < n; i++) {
            Rectangle obs = obstacles.get(i);
            if (fx < obs.x + obs.width  && fx + fw > obs.x &&
                fy < obs.y + obs.height && fy + feetH > obs.y) {
                return false;
            }
        }
        t.x = nx;
        t.y = ny;
        return true;
    }

    private static void setFacingFromDir(TransformComponent t, float dx, float dy) {
        if (Math.abs(dy) >= Math.abs(dx)) {
            t.direction = dy > 0f ? "up" : "down";
        } else {
            t.direction = dx > 0f ? "right" : "left";
        }
    }

    private int findPlayer() {
        IntBag bag = playerSub.getEntities();
        return bag.size() > 0 ? bag.getData()[0] : -1;
    }
}
