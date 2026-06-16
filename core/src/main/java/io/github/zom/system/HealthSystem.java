package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.systems.IteratingSystem;

import io.github.zom.component.AnimationStateComponent;
import io.github.zom.component.CombatComponent;
import io.github.zom.component.HealthComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.ZedComponent;
import io.github.zom.component.ZedRendererComponent;
import io.github.zom.rendering.ZedRenderer;

/**
 * Processes HealthComponent.pendingDamage queues for all entities.
 *
 * Each frame:
 *   1. Tick down iFrameTimer
 *   2. Drain pendingDamage:
 *      - Skip if entity is invulnerable (iFrameTimer > 0)
 *      - Apply 2× multiplier for headshots
 *      - Clamp hp to 0, start i-frame timer
 *   3. If hp <= 0 and not yet dead: trigger death
 *      - ZedComponent → zed.die(variant), play die animation
 *      - PlayerComponent → play dead animation
 *
 * God mode: CombatComponent.godMode skips damage application (not i-frames).
 */
public class HealthSystem extends IteratingSystem {

    private static final float HEADSHOT_MULTIPLIER = 2.0f;

    private ComponentMapper<HealthComponent>         mHealth;
    private ComponentMapper<ZedComponent>            mZed;
    private ComponentMapper<PlayerComponent>         mPlayer;
    private ComponentMapper<AnimationStateComponent> mAnim;
    private ComponentMapper<CombatComponent>         mCombat;
    private ComponentMapper<ZedRendererComponent>    mRenderer;

    public HealthSystem() {
        super(Aspect.all(HealthComponent.class));
    }

    @Override
    protected void process(int entityId) {
        HealthComponent health = mHealth.get(entityId);
        if (health.dead) return;

        float delta = world.getDelta();

        // ── Tick i-frame timer ────────────────────────────────────────────────
        if (health.iFrameTimer > 0f) {
            health.iFrameTimer = Math.max(0f, health.iFrameTimer - delta);
        }

        // ── God mode check ────────────────────────────────────────────────────
        boolean godMode = mCombat.has(entityId) && mCombat.get(entityId).godMode;

        // ── Drain pending damage ──────────────────────────────────────────────
        if (!health.pendingDamage.isEmpty()) {
            if (!health.isInvulnerable() && !godMode) {
                for (float[] entry : health.pendingDamage) {
                    float  damage    = entry[0];
                    boolean headshot = entry[1] > 0f;
                    if (headshot) damage *= HEADSHOT_MULTIPLIER;
                    health.hp -= damage;
                }
                health.iFrameTimer = health.iFrameDuration;
                health.hp = Math.max(0f, health.hp);
            }
            health.pendingDamage.clear();
        }

        // ── Death trigger ─────────────────────────────────────────────────────
        if (health.hp <= 0f) {
            health.dead = true;
            triggerDeath(entityId);
        }
    }

    private void triggerDeath(int entityId) {
        AnimationStateComponent anim = mAnim.has(entityId) ? mAnim.get(entityId) : null;

        if (mZed.has(entityId)) {
            ZedComponent zed = mZed.get(entityId);
            ZedRendererComponent rendComp = mRenderer.has(entityId) ? mRenderer.get(entityId) : null;

            // Make sure the renderer is initialized so we can check its dead pose capability
            if (rendComp != null && rendComp.renderer == null) {
                rendComp.renderer = new ZedRenderer(zed.zedType, zed.skinName, zed.deadSkinName);
                zed.dirty = false;
            }

            // Dynamically select an available death animation variant
            String variant = "default";
            if (rendComp != null && rendComp.renderer != null) {
                boolean hasDie1 = rendComp.renderer.hasDeadPose("die1");
                boolean hasDie2 = rendComp.renderer.hasDeadPose("die2");

                if (hasDie1 && hasDie2) {
                    variant = (entityId % 2 == 0) ? "die1" : "die2";
                } else if (hasDie1) {
                    variant = "die1";
                } else if (hasDie2) {
                    variant = "die2";
                } else if (rendComp.renderer.hasDeadPose("default")) {
                    variant = "default";
                }
            } else {
                variant = (entityId % 2 == 0) ? "die1" : "die2";
            }

            zed.die(variant);
            if (anim != null) anim.playOnce(variant, 1.5f);

        } else if (mPlayer.has(entityId)) {
            if (anim != null) anim.playOnce("dead", 2.0f);
        }
    }
}
