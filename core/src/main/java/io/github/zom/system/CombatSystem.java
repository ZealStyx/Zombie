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
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDataDef;
import io.github.zom.config.ItemDef;
import io.github.zom.util.ItemInstance;

/**
 * Processes player attack requests set by MovementSystem.
 *
 * CHANGES:
 * - syncHeldWeapon() reads stats from ItemDataConfig (not ItemGridDef) and writes
 *   currentAmmo back to player.equippedHeld.currentAmmo so ammo persists across
 *   equip/unequip cycles.
 * - doRanged() picks the correct fire animation ("twohand" for primary, "pistol"
 *   for secondary) instead of hardcoding "pistol".
 * - player.dirty is consumed (set false) AFTER syncHeldWeapon, so GameRenderSystem
 *   still sees it for PlayerRenderer.rebuild().
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
        PlayerComponent         player    = mPlayer.get(entityId);
        CombatComponent         combat    = mCombat.get(entityId);
        TransformComponent      transform = mTransform.get(entityId);
        AnimationStateComponent anim      = mAnim.get(entityId);
        float delta = world.getDelta();

        // Sync weapon stats whenever equipment changed
        if (player.dirty) {
            syncHeldWeapon(player, combat);
            // Note: we do NOT clear player.dirty here — GameRenderSystem needs it
            // to trigger PlayerRenderer.rebuild(). It is cleared there.
        }

        // Tick cooldown timers
        combat.meleeTimer  = Math.max(0f, combat.meleeTimer  - delta);
        combat.rangedTimer = Math.max(0f, combat.rangedTimer - delta);

        // Tick reload timer
        if (combat.reloading) {
            combat.reloadTimer -= delta;
            if (combat.reloadTimer <= 0f) {
                combat.reloading = false;
                completeReload(entityId, combat, player);
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
            } else if (combat.isAutoFiring
                && ("auto".equals(combat.fireMode) || "both".equals(combat.fireMode))) {
                wantsToFire = true;
            }
        }
        combat.rangedRequested = false;

        if (wantsToFire && combat.rangedTimer <= 0f) {
            if (combat.currentAmmo > 0) {
                combat.currentAmmo--;
                // Persist ammo into the equipped instance immediately
                if (player.equippedHeld != null) {
                    player.equippedHeld.currentAmmo = combat.currentAmmo;
                }
                combat.rangedTimer = combat.rangedCooldown;
                // Fire animation: twohand for primary, pistol for secondary
                String fireAnim = resolveFireAnim(player);
                anim.playOnce(fireAnim, combat.rangedCooldown);
                doRanged(transform, combat);
            } else {
                combat.reloadRequested = true;
            }
        }

        // ── Reload request ────────────────────────────────────────────────────
        if (combat.reloadRequested) {
            combat.reloadRequested = false;
            if (!combat.reloading && combat.currentAmmo < combat.clipSize && combat.hasRanged) {
                int found = countAmmoInInventory(entityId, combat.ammoItemId);
                if (found > 0) {
                    combat.reloading    = true;
                    combat.reloadTimer  = combat.reloadDuration;
                    com.badlogic.gdx.Gdx.app.log("Combat", "Reloading… need "
                        + (combat.clipSize - combat.currentAmmo) + ", found " + found);
                } else {
                    com.badlogic.gdx.Gdx.app.log("Combat", "No ammo of type " + combat.ammoItemId);
                }
            }
        }
    }

    // ── Weapon sync ───────────────────────────────────────────────────────────

    /**
     * Copies stats from ItemDataConfig into CombatComponent.
     * Also restores currentAmmo from the ItemInstance so reloaded rounds survive
     * holster/un-holster cycles.
     */
    private void syncHeldWeapon(PlayerComponent player, CombatComponent combat) {
        ItemInstance held = player.equippedHeld;
        if (held == null) {
            setFistDefaults(combat);
            return;
        }

        ItemDataDef dd = ConfigLoader.getItemDataConfig().get(held.itemId);
        if (dd == null) {
            setFistDefaults(combat);
            return;
        }

        if (dd.isGun()) {
            combat.hasRanged      = true;
            combat.rangedDamage   = dd.damage;
            combat.rangedRange    = dd.range;
            combat.clipSize       = dd.clipSize;
            combat.rangedCooldown = dd.fireCooldown;
            combat.reloadDuration = dd.reloadTime;
            combat.ammoItemId     = dd.ammoItemId;
            combat.fireMode       = dd.fireMode != null ? dd.fireMode : "semi";
            // Restore ammo from the instance — preserves count across holster cycles
            combat.currentAmmo    = held.currentAmmo;
        } else {
            // Melee weapon
            combat.hasRanged     = false;
            combat.meleeDamage   = dd.damage > 0 ? dd.damage : 20f;
            combat.meleeRange    = dd.range  > 0 ? dd.range  : 36f;
        }
    }

    private void setFistDefaults(CombatComponent combat) {
        combat.hasRanged     = false;
        combat.meleeDamage   = 15f;
        combat.meleeRange    = 28f;
        combat.meleeCooldown = 0.5f;
    }

    private String resolveFireAnim(PlayerComponent player) {
        if (player.equippedHeld == null) return "pistol";
        ItemDef def = ConfigLoader.getItemDatabase().get(player.equippedHeld.itemId);
        if (def != null && "primary".equals(def.type)) return "twohand";
        return "pistol";
    }

    // ── Reload completion ────────────────────────────────────────────────────

    private void completeReload(int entityId, CombatComponent combat, PlayerComponent player) {
        int needed   = combat.clipSize - combat.currentAmmo;
        if (needed <= 0) return;
        int consumed = consumeAmmoFromInventory(entityId, combat.ammoItemId, needed);
        combat.currentAmmo += consumed;
        // Persist into equipped instance
        if (player.equippedHeld != null) {
            player.equippedHeld.currentAmmo = combat.currentAmmo;
        }
        com.badlogic.gdx.Gdx.app.log("Combat", "Reload done: " + consumed
            + " rounds. Total: " + combat.currentAmmo);
    }

    // ── Ammo inventory helpers ────────────────────────────────────────────────

    private int countAmmoInInventory(int entityId, int ammoItemId) {
        int count = 0;
        if (mInventory.has(entityId)) {
            InventoryComponent inv = mInventory.get(entityId);
            if (inv.inventory != null) count += inv.inventory.count(ammoItemId);
        }
        if (mPlayer.has(entityId)) {
            PlayerComponent p = mPlayer.get(entityId);
            if (p.equippedBackpack != null && p.equippedBackpack.container != null)
                count += p.equippedBackpack.container.count(ammoItemId);
            if (p.equippedSlingBag != null && p.equippedSlingBag.container != null)
                count += p.equippedSlingBag.container.count(ammoItemId);
        }
        return count;
    }

    private int consumeAmmoFromInventory(int entityId, int ammoItemId, int amount) {
        int remaining = amount;
        if (mInventory.has(entityId)) {
            InventoryComponent inv = mInventory.get(entityId);
            if (inv.inventory != null) {
                int take = Math.min(inv.inventory.count(ammoItemId), remaining);
                if (take > 0) { inv.inventory.remove(ammoItemId, take); remaining -= take; }
            }
        }
        if (remaining > 0 && mPlayer.has(entityId)) {
            PlayerComponent p = mPlayer.get(entityId);
            if (p.equippedBackpack != null && p.equippedBackpack.container != null) {
                int take = Math.min(p.equippedBackpack.container.count(ammoItemId), remaining);
                if (take > 0) { p.equippedBackpack.container.remove(ammoItemId, take); remaining -= take; }
            }
            if (remaining > 0 && p.equippedSlingBag != null && p.equippedSlingBag.container != null) {
                int take = Math.min(p.equippedSlingBag.container.count(ammoItemId), remaining);
                if (take > 0) { p.equippedSlingBag.container.remove(ammoItemId, take); remaining -= take; }
            }
        }
        return amount - remaining;
    }

    // ── Melee ─────────────────────────────────────────────────────────────────

    private void doMelee(TransformComponent t, CombatComponent combat) {
        float cx = t.x + t.w * 0.5f;
        float cy = t.y + t.h * 0.5f;
        Rectangle hitRect = buildHitRect(cx, cy, t.direction, combat.meleeRange, combat.meleeHalfWidth);

        IntBag zeds = zedSub.getEntities();
        int[]  data = zeds.getData();
        for (int i = 0, n = zeds.size(); i < n; i++) {
            int zId = data[i];
            ZedComponent zed = mZed.get(zId);
            if (!zed.alive) continue;
            CollisionComponent col    = mCollision.get(zId);
            HealthComponent    health = mHealth.get(zId);
            if      (hitRect.overlaps(col.headBox)) health.queueDamage(combat.meleeDamage, true);
            else if (hitRect.overlaps(col.bodyBox)) health.queueDamage(combat.meleeDamage, false);
        }
    }

    private static Rectangle buildHitRect(float cx, float cy, String dir,
                                          float range, float hw) {
        switch (dir) {
            case "up":    return new Rectangle(cx - hw, cy,         hw * 2f, range);
            case "down":  return new Rectangle(cx - hw, cy - range, hw * 2f, range);
            case "right": return new Rectangle(cx,       cy - hw,   range,   hw * 2f);
            case "left":  return new Rectangle(cx-range, cy - hw,   range,   hw * 2f);
            default:      return new Rectangle(cx - hw, cy,         hw * 2f, range);
        }
    }

    // ── Ranged / hitscan ─────────────────────────────────────────────────────

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
        float step  = 4f;
        int   steps = (int)(combat.rangedRange / step);
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
                if (col.headBox.contains(rx, ry)) { health.queueDamage(combat.rangedDamage, true);  return; }
                if (col.bodyBox.contains(rx, ry)) { health.queueDamage(combat.rangedDamage, false); return; }
            }
        }
    }
}
