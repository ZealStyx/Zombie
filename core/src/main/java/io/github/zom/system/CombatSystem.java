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
import io.github.zom.component.InventoryComponent;
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
    private ComponentMapper<InventoryComponent>      mInventory;

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

        // Tick reload timer
        if (combat.reloading) {
            combat.reloadTimer -= delta;
            if (combat.reloadTimer <= 0f) {
                combat.reloading = false;
                completeReload(entityId, combat);
            }
        }

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
        boolean wantsToFire = false;
        if (combat.hasRanged && !combat.reloading) {
            if (combat.rangedRequested) {
                wantsToFire = true;
            } else if (combat.isAutoFiring && ("auto".equals(combat.fireMode) || "both".equals(combat.fireMode))) {
                wantsToFire = true;
            }
        }
        combat.rangedRequested = false;

        if (wantsToFire && combat.rangedTimer <= 0f) {
            if (combat.currentAmmo > 0) {
                combat.currentAmmo--;
                combat.rangedTimer = combat.rangedCooldown;
                // pose depends on weapon type; "pistol" for secondary, "twohand" for primary
                anim.playOnce("pistol", combat.rangedCooldown);
                doRanged(transform, combat);
            } else {
                combat.reloadRequested = true;
            }
        }

        // ── Reload request ────────────────────────────────────────────────────
        if (combat.reloadRequested) {
            combat.reloadRequested = false;
            if (!combat.reloading && combat.currentAmmo < combat.clipSize && combat.hasRanged) {
                int needed = combat.clipSize - combat.currentAmmo;
                int found = countAmmoInInventory(entityId, combat.ammoItemId);
                if (found > 0) {
                    combat.reloading = true;
                    combat.reloadTimer = combat.reloadDuration;
                    com.badlogic.gdx.Gdx.app.log("Combat", "Started reloading... Needs " + needed + ", found " + found);
                } else {
                    com.badlogic.gdx.Gdx.app.log("Combat", "Cannot reload: Out of ammo of type " + combat.ammoItemId);
                }
            }
        }
    }

    private void completeReload(int entityId, CombatComponent combat) {
        int needed = combat.clipSize - combat.currentAmmo;
        if (needed <= 0) return;

        int consumed = consumeAmmoFromInventory(entityId, combat.ammoItemId, needed);
        combat.currentAmmo += consumed;
        com.badlogic.gdx.Gdx.app.log("Combat", "Reload completed! Loaded " + consumed + " rounds. Total: " + combat.currentAmmo);
    }

    private int countAmmoInInventory(int entityId, int ammoItemId) {
        int count = 0;
        if (mInventory.has(entityId)) {
            InventoryComponent invComp = mInventory.get(entityId);
            if (invComp.inventory != null) {
                count += invComp.inventory.count(ammoItemId);
            }
        }
        if (mPlayer.has(entityId)) {
            PlayerComponent player = mPlayer.get(entityId);
            if (player.equippedBackpack != null && player.equippedBackpack.container != null) {
                count += player.equippedBackpack.container.count(ammoItemId);
            }
            if (player.equippedSlingBag != null && player.equippedSlingBag.container != null) {
                count += player.equippedSlingBag.container.count(ammoItemId);
            }
        }
        return count;
    }

    private int consumeAmmoFromInventory(int entityId, int ammoItemId, int amount) {
        int remainingToConsume = amount;

        // 1. Consume from base inventory first
        if (mInventory.has(entityId)) {
            InventoryComponent invComp = mInventory.get(entityId);
            if (invComp.inventory != null) {
                int available = invComp.inventory.count(ammoItemId);
                int take = Math.min(available, remainingToConsume);
                if (take > 0) {
                    invComp.inventory.remove(ammoItemId, take);
                    remainingToConsume -= take;
                }
            }
        }

        // 2. Consume from backpack
        if (remainingToConsume > 0 && mPlayer.has(entityId)) {
            PlayerComponent player = mPlayer.get(entityId);
            if (player.equippedBackpack != null && player.equippedBackpack.container != null) {
                int available = player.equippedBackpack.container.count(ammoItemId);
                int take = Math.min(available, remainingToConsume);
                if (take > 0) {
                    player.equippedBackpack.container.remove(ammoItemId, take);
                    remainingToConsume -= take;
                }
            }
        }

        // 3. Consume from sling bag
        if (remainingToConsume > 0 && mPlayer.has(entityId)) {
            PlayerComponent player = mPlayer.get(entityId);
            if (player.equippedSlingBag != null && player.equippedSlingBag.container != null) {
                int available = player.equippedSlingBag.container.count(ammoItemId);
                int take = Math.min(available, remainingToConsume);
                if (take > 0) {
                    player.equippedSlingBag.container.remove(ammoItemId, take);
                    remainingToConsume -= take;
                }
            }
        }

        return amount - remainingToConsume;
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
