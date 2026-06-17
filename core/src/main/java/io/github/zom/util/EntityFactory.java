package io.github.zom.util;

import com.artemis.World;

import io.github.zom.component.AnimationStateComponent;
import io.github.zom.component.CombatComponent;
import io.github.zom.component.CollisionComponent;
import io.github.zom.component.HealthComponent;
import io.github.zom.component.InventoryComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.PlayerRendererComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.component.WorldItemComponent;
import io.github.zom.component.ZedAIComponent;
import io.github.zom.component.ZedComponent;
import io.github.zom.component.ZedRendererComponent;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDef;
import io.github.zom.config.PlayerConfig;
import io.github.zom.config.ZedConfig;
import io.github.zom.rendering.TextureCache;

public final class EntityFactory {

    private static final float DEFAULT_PLAYER_SIZE = 30f;
    private static final float DEFAULT_ZED_SIZE    = 30f;
    private static final float DEFAULT_ITEM_SIZE   = 8f;

    private EntityFactory() {}

    // ── Player ────────────────────────────────────────────────────────────────

    public static int createPlayer(World world, float x, float y) {
        int id = world.create();

        float[] wh = resolvePlayerSize("player_skin_def_1");

        TransformComponent transform = world.edit(id).create(TransformComponent.class);
        transform.set(x, y, wh[0], wh[1]);
        transform.direction = "down";

        world.edit(id).create(AnimationStateComponent.class);
        world.edit(id).create(PlayerComponent.class);
        world.edit(id).create(PlayerRendererComponent.class);

        // FIX: base inventory is 4 rows × 6 cols per spec (was 5×10)
        InventoryComponent inv = world.edit(id).create(InventoryComponent.class);
        inv.init(4, 6);

        HealthComponent health = world.edit(id).create(HealthComponent.class);
        health.maxHp = 100f;
        health.hp    = 100f;

        world.edit(id).create(CombatComponent.class);
        world.edit(id).create(CollisionComponent.class);

        return id;
    }

    // ── Zed ──────────────────────────────────────────────────────────────────

    public static int createZed(World world, float x, float y,
                                String zedType, String skinName, String deadSkinName) {
        int id = world.create();

        float[] wh = resolveZedSize(zedType, skinName);

        TransformComponent transform = world.edit(id).create(TransformComponent.class);
        transform.set(x, y, wh[0], wh[1]);
        transform.direction = "down";

        AnimationStateComponent anim = world.edit(id).create(AnimationStateComponent.class);
        anim.setPose("idle");

        if (deadSkinName == null || deadSkinName.trim().isEmpty()) {
            ZedConfig cfg = ConfigLoader.getZedConfig();
            if (cfg != null) deadSkinName = cfg.getMatchingDeadSkinName(zedType, skinName);
        }

        ZedComponent zed = world.edit(id).create(ZedComponent.class);
        zed.zedType      = zedType;
        zed.skinName     = skinName;
        zed.deadSkinName = deadSkinName;
        zed.alive        = true;
        zed.dirty        = true;

        HealthComponent health = world.edit(id).create(HealthComponent.class);
        ZedAIComponent  ai     = world.edit(id).create(ZedAIComponent.class);
        applyZedTypeStats(zedType, health, ai);

        world.edit(id).create(ZedRendererComponent.class);
        world.edit(id).create(CollisionComponent.class);

        return id;
    }

    // ── World item ────────────────────────────────────────────────────────────

    public static int createWorldItem(World world, float x, float y, int itemId, int quantity) {
        int id = world.create();

        float[] wh = resolveWorldItemSize(itemId);

        TransformComponent transform = world.edit(id).create(TransformComponent.class);
        transform.set(x, y, wh[0], wh[1]);

        WorldItemComponent item = world.edit(id).create(WorldItemComponent.class);
        item.itemId   = itemId;
        item.quantity = quantity;

        return id;
    }

    // ── Size resolution ───────────────────────────────────────────────────────

    private static float[] resolvePlayerSize(String skinName) {
        PlayerConfig cfg = ConfigLoader.getPlayerConfig();
        if (cfg == null) return new float[]{DEFAULT_PLAYER_SIZE, DEFAULT_PLAYER_SIZE};
        PlayerConfig.SkinEntry skin = cfg.getSkin(skinName);
        String path = firstPath(
            skin != null ? skin.getFrames("idle", "down") : null,
            skin != null ? skin.getFrames("run", "down")  : null,
            skin != null ? skin.dead : null
        );
        return readSize(path, DEFAULT_PLAYER_SIZE, DEFAULT_PLAYER_SIZE);
    }

    private static float[] resolveZedSize(String zedType, String skinName) {
        ZedConfig cfg = ConfigLoader.getZedConfig();
        if (cfg == null) return new float[]{DEFAULT_ZED_SIZE, DEFAULT_ZED_SIZE};
        ZedConfig.SkinEntry skin = cfg.getSkin(zedType, skinName);
        String path = firstPath(
            skin != null ? skin.getFrames("idle",   "down")   : null,
            skin != null ? skin.getFrames("walk",   "down")   : null,
            skin != null ? skin.getFrames("run",    "down")   : null,
            skin != null ? skin.getFrames("attack", "down")   : null,
            skin != null ? skin.getFrames("idle",   "frames") : null
        );
        return readSize(path, DEFAULT_ZED_SIZE, DEFAULT_ZED_SIZE);
    }

    private static float[] resolveWorldItemSize(int itemId) {
        ItemDef def = ConfigLoader.getItemDatabase().get(itemId);
        if (def == null || def.sprite == null || def.sprite.on_ground == null) {
            return new float[]{DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE};
        }
        return readSize(def.sprite.on_ground, DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE);
    }

    @SafeVarargs
    private static String firstPath(com.badlogic.gdx.utils.Array<String>... arrays) {
        for (com.badlogic.gdx.utils.Array<String> a : arrays) {
            if (a != null && a.size > 0 && a.get(0) != null) return a.get(0);
        }
        return null;
    }

    private static float[] readSize(String path, float fallbackW, float fallbackH) {
        if (path == null) return new float[]{fallbackW, fallbackH};
        TextureCache cache = TextureCache.get();
        return new float[]{cache.pixelWidth(path), cache.pixelHeight(path)};
    }

    private static void applyZedTypeStats(String zedType, HealthComponent health, ZedAIComponent ai) {
        float hp;
        switch (zedType) {
            case "fast":     hp=40f;  ai.speed=80f;  ai.detectionRange=240f; ai.attackDamage=10f;  ai.attackCooldown=0.8f;  break;
            case "army":     hp=80f;  ai.speed=55f;  ai.detectionRange=220f; ai.attackDamage=20f;  ai.attackCooldown=1.0f;  break;
            case "tank":     hp=200f; ai.speed=24f;  ai.detectionRange=160f; ai.attackDamage=35f;  ai.attackCooldown=2.0f;  break;
            case "screamer": hp=50f;  ai.speed=32f;  ai.detectionRange=280f; ai.attackDamage=5f;   ai.attackCooldown=1.5f;  break;
            case "shooter":  hp=60f;  ai.speed=36f;  ai.detectionRange=300f; ai.attackDamage=12f;  ai.attackCooldown=2.0f;  break;
            case "jumper":   hp=50f;  ai.speed=70f;  ai.detectionRange=200f; ai.attackDamage=20f;  ai.attackCooldown=1.5f;  break;
            case "buried":   hp=60f;  ai.speed=0f;   ai.state=ZedAIComponent.State.IDLE;
                ai.detectionRange=120f; ai.attackDamage=15f; ai.attackCooldown=1.2f; break;
            default:         hp=60f;  ai.speed=40f;  ai.detectionRange=200f; ai.attackDamage=15f;  ai.attackCooldown=1.2f;  break;
        }
        health.maxHp = hp;
        health.hp    = hp;
    }
}
