package io.github.zom.system;

import com.artemis.Aspect;
import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.EntitySubscription;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import io.github.zom.component.AnimationStateComponent;
import io.github.zom.component.CollisionComponent;
import io.github.zom.component.PlayerComponent;
import io.github.zom.component.PlayerRendererComponent;
import io.github.zom.component.TransformComponent;
import io.github.zom.component.WorldItemComponent;
import io.github.zom.component.ZedAIComponent;
import io.github.zom.component.ZedComponent;
import io.github.zom.component.ZedRendererComponent;
import io.github.zom.config.ConfigLoader;
import io.github.zom.config.ItemDef;
import io.github.zom.rendering.PlayerRenderer;
import io.github.zom.rendering.SpriteRenderState;
import io.github.zom.rendering.TextureCache;
import io.github.zom.rendering.WorldItemLabelRenderer;
import io.github.zom.rendering.ZedRenderer;
import io.github.zom.world.WorldCollision;

/**
 * Unified render system that draws all entities sorted by their feet Y-coordinate.
 * Layering order:
 *  1. Dead zombies (background/ground layer) sorted by Y
 *  2. World items (always on top of dead zombies, walk-overable) sorted by Y
 *  3. Alive entities (player + alive zombies) sorted by Y
 */
public class GameRenderSystem extends BaseSystem {

    private ComponentMapper<PlayerComponent>         mPlayer;
    private ComponentMapper<PlayerRendererComponent> mPlayerRenderer;
    private ComponentMapper<ZedComponent>            mZed;
    private ComponentMapper<ZedRendererComponent>    mZedRenderer;
    private ComponentMapper<WorldItemComponent>      mItem;
    private ComponentMapper<TransformComponent>      mTransform;
    private ComponentMapper<AnimationStateComponent> mAnim;
    private ComponentMapper<CollisionComponent>      mCollision;
    private ComponentMapper<ZedAIComponent>          mAI;

    private EntitySubscription playerSub;
    private EntitySubscription zedSub;
    private EntitySubscription itemSub;

    private final SpriteBatch batch;
    private final OrthographicCamera camera;

    /** Renders floating item-name labels above nearby ground items. */
    private WorldItemLabelRenderer labelRenderer;

    // Sprite state holders for drawing
    private final SpriteRenderState zedRenderState = new SpriteRenderState();
    private final SpriteRenderState playerRenderState = new SpriteRenderState();

    // Lazy-created ShapeRenderer for debug drawing
    private ShapeRenderer shapeRenderer;

    // Sort buffers to avoid frame allocations
    private final Array<Integer> deadZeds = new Array<>();
    private final Array<Integer> items = new Array<>();
    private final Array<Integer> aliveEntities = new Array<>();

    private final java.util.Comparator<Integer> yComparator = new java.util.Comparator<Integer>() {
        @Override
        public int compare(Integer id1, Integer id2) {
            float y1 = getFeetY(id1);
            float y2 = getFeetY(id2);
            // Sort in descending order: higher Y (back) first, lower Y (front) last
            return Float.compare(y2, y1);
        }
    };

    public GameRenderSystem(SpriteBatch batch, OrthographicCamera camera) {
        this.batch = batch;
        this.camera = camera;
    }

    @Override
    protected void initialize() {
        playerSub = world.getAspectSubscriptionManager().get(Aspect.all(
            PlayerComponent.class, PlayerRendererComponent.class, TransformComponent.class, AnimationStateComponent.class
        ));
        zedSub = world.getAspectSubscriptionManager().get(Aspect.all(
            ZedComponent.class, ZedRendererComponent.class, TransformComponent.class, AnimationStateComponent.class
        ));
        itemSub = world.getAspectSubscriptionManager().get(Aspect.all(
            WorldItemComponent.class, TransformComponent.class
        ));
        // Label renderer created here so it can register its own subscriptions
        labelRenderer = new WorldItemLabelRenderer(world);
    }

    @Override
    protected void processSystem() {
        deadZeds.clear();
        items.clear();
        aliveEntities.clear();

        // 1. Gather players
        IntBag players = playerSub.getEntities();
        int[] playerIds = players.getData();
        for (int i = 0, n = players.size(); i < n; i++) {
            aliveEntities.add(playerIds[i]);
        }

        // 2. Gather zeds
        IntBag zeds = zedSub.getEntities();
        int[] zedIds = zeds.getData();
        for (int i = 0, n = zeds.size(); i < n; i++) {
            int zid = zedIds[i];
            if (mZed.has(zid)) {
                ZedComponent z = mZed.get(zid);
                if (z.alive) {
                    aliveEntities.add(zid);
                } else {
                    deadZeds.add(zid);
                }
            }
        }

        // 3. Gather items
        IntBag itemBag = itemSub.getEntities();
        int[] itemIds = itemBag.getData();
        for (int i = 0, n = itemBag.size(); i < n; i++) {
            items.add(itemIds[i]);
        }

        // 4. Sort arrays descending
        deadZeds.sort(yComparator);
        items.sort(yComparator);
        aliveEntities.sort(yComparator);

        // 5. Draw layers
        // Layer 1: Dead zombies
        for (int i = 0, n = deadZeds.size; i < n; i++) {
            drawDeadZed(deadZeds.get(i));
        }

        // Layer 2: World items (always above dead zombies)
        for (int i = 0, n = items.size; i < n; i++) {
            drawItem(items.get(i));
        }

        // Layer 3: Alive entities (players and alive zombies, sorted relative to each other)
        for (int i = 0, n = aliveEntities.size; i < n; i++) {
            int eid = aliveEntities.get(i);
            if (mPlayer.has(eid)) {
                drawPlayer(eid);
            } else {
                drawAliveZed(eid);
            }
        }

        // Proximity item labels — find player centre first
        IntBag playerBag = playerSub.getEntities();
        if (labelRenderer != null && playerBag.size() > 0) {
            int pid = playerBag.getData()[0];
            if (mTransform.has(pid)) {
                TransformComponent ptf = mTransform.get(pid);
                float pcx = ptf.x + ptf.w * 0.5f;
                float pcy = ptf.y + ptf.h * 0.5f;
                labelRenderer.draw(batch, pcx, pcy);
            }
        }

        // 6. Draw debug lines
        drawDebugOverlays();
    }

    private float getFeetY(int entityId) {
        if (mCollision.has(entityId)) {
            return mCollision.get(entityId).feetBox.y;
        }
        if (mTransform.has(entityId)) {
            return mTransform.get(entityId).y;
        }
        return 0f;
    }

    private void drawDeadZed(int entityId) {
        ZedComponent zed = mZed.get(entityId);
        ZedRendererComponent rendComp = mZedRenderer.get(entityId);
        TransformComponent transform = mTransform.get(entityId);

        if (rendComp.renderer == null || zed.dirty) {
            rendComp.renderer = new ZedRenderer(zed.zedType, zed.skinName, zed.deadSkinName);
            zed.dirty = false;
        }

        rendComp.deadStateTime += world.getDelta();
        rendComp.renderer.drawDead(batch, zed.dieVariant, rendComp.deadStateTime,
            transform.x, transform.y, transform.w, transform.h);
    }

    private void drawAliveZed(int entityId) {
        ZedComponent zed = mZed.get(entityId);
        ZedRendererComponent rendComp = mZedRenderer.get(entityId);
        TransformComponent transform = mTransform.get(entityId);
        AnimationStateComponent anim = mAnim.get(entityId);

        if (rendComp.renderer == null || zed.dirty) {
            rendComp.renderer = new ZedRenderer(zed.zedType, zed.skinName, zed.deadSkinName);
            zed.dirty = false;
        }

        float x = transform.x;
        float y = transform.y;
        float w = transform.w;
        float h = transform.h;

        String pose = anim.pose;
        if (isSpawnFxPose(pose)) {
            zedRenderState.pose = pose;
            zedRenderState.direction = transform.direction;
            zedRenderState.stateTime = anim.stateTime;
            rendComp.renderer.drawSpawnFx(batch, zedRenderState, x, y, w, h);
            return;
        }

        zedRenderState.pose = pose;
        zedRenderState.direction = transform.direction;
        zedRenderState.stateTime = anim.stateTime;
        rendComp.renderer.drawAlive(batch, zedRenderState, x, y, w, h);
    }

    private void drawPlayer(int entityId) {
        PlayerComponent player = mPlayer.get(entityId);
        PlayerRendererComponent rendComp = mPlayerRenderer.get(entityId);
        TransformComponent transform = mTransform.get(entityId);
        AnimationStateComponent anim = mAnim.get(entityId);

        if (rendComp.renderer == null) {
            rendComp.renderer = new PlayerRenderer();
            player.dirty = true;
        }
        if (player.dirty) {
            rendComp.renderer.rebuild(
                player.skinName, player.handsName,
                player.heldItemId, player.holsteredItemId,
                player.vestId, player.helmetId, player.pantsId,
                player.topId, player.backpackId, player.footwearId
            );
            player.dirty = false;
        }

        playerRenderState.pose = anim.pose;
        playerRenderState.direction = transform.direction;
        playerRenderState.stateTime = anim.stateTime;
        playerRenderState.locked = anim.locked;

        rendComp.renderer.draw(batch, playerRenderState,
            transform.x, transform.y, transform.w, transform.h);
    }

    private void drawItem(int entityId) {
        WorldItemComponent item = mItem.get(entityId);
        TransformComponent transform = mTransform.get(entityId);

        if (item.itemId <= 0) return;

        ItemDef def = ConfigLoader.getItemDatabase().get(item.itemId);
        if (def == null || def.sprite == null || def.sprite.on_ground == null) return;

        if (!isVisible(transform)) return;

        TextureRegion region = TextureCache.get().region(def.sprite.on_ground);
        batch.draw(region, transform.x, transform.y, transform.w, transform.h);
    }

    private void drawDebugOverlays() {
        boolean drawCone   = DebugConsoleSystem.showZedVisionCone;
        boolean drawRanges = DebugConsoleSystem.showZedRanges;
        boolean drawPlayer = DebugConsoleSystem.showPlayerCollision;
        boolean drawWorld  = DebugConsoleSystem.showWorldCollision;

        if (!drawCone && !drawRanges && !drawPlayer && !drawWorld) return;

        batch.end();

        if (shapeRenderer == null) {
            shapeRenderer = new ShapeRenderer();
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // --- Line mode ---
        if (drawRanges || drawPlayer || drawWorld) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            // World obstacles
            if (drawWorld) {
                shapeRenderer.setColor(1f, 0.3f, 0.3f, 0.5f);
                Array<Rectangle> obstacles = WorldCollision.getObstacles();
                for (int i = 0, n = obstacles.size; i < n; i++) {
                    drawRect(obstacles.get(i));
                }
            }

            // Player collision boxes
            if (drawPlayer) {
                IntBag players = playerSub.getEntities();
                int[] playerIds = players.getData();
                for (int i = 0, n = players.size(); i < n; i++) {
                    int pid = playerIds[i];
                    if (!mCollision.has(pid)) continue;
                    CollisionComponent col = mCollision.get(pid);

                    shapeRenderer.setColor(0.2f, 0.4f, 1f, 0.9f); // feet - blue
                    drawRect(col.feetBox);

                    shapeRenderer.setColor(0.2f, 1f, 0.3f, 0.9f); // body - green
                    drawRect(col.bodyBox);

                    shapeRenderer.setColor(1f, 0.2f, 0.2f, 0.9f); // head - red
                    drawRect(col.headBox);
                }
            }

            // Zed range circles
            if (drawRanges) {
                IntBag zeds = zedSub.getEntities();
                int[] zedIds = zeds.getData();
                for (int i = 0, n = zeds.size(); i < n; i++) {
                    int zid = zedIds[i];
                    if (!mAI.has(zid) || !mZed.has(zid)) continue;
                    ZedComponent zed = mZed.get(zid);
                    if (!zed.alive) continue;

                    TransformComponent tf = mTransform.get(zid);
                    ZedAIComponent ai = mAI.get(zid);

                    float cx = tf.x + tf.w / 2f;
                    float cy = tf.y + tf.h / 2f;

                    shapeRenderer.setColor(1f, 1f, 0f, 0.6f); // Detection - yellow
                    shapeRenderer.circle(cx, cy, ai.detectionRange, 48);

                    shapeRenderer.setColor(0.8f, 0.6f, 0f, 0.3f); // De-aggro - dark yellow
                    shapeRenderer.circle(cx, cy, ai.deaggroRange, 48);

                    shapeRenderer.setColor(1f, 0.2f, 0.2f, 0.8f); // Attack - red
                    shapeRenderer.circle(cx, cy, ai.attackRange, 24);
                }
            }

            shapeRenderer.end();
        }

        // --- Filled mode (Vision Cones) ---
        if (drawCone) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            IntBag zeds = zedSub.getEntities();
            int[] zedIds = zeds.getData();
            for (int i = 0, n = zeds.size(); i < n; i++) {
                int zid = zedIds[i];
                if (!mAI.has(zid) || !mZed.has(zid)) continue;
                ZedComponent zed = mZed.get(zid);
                if (!zed.alive) continue;

                TransformComponent tf = mTransform.get(zid);
                ZedAIComponent ai = mAI.get(zid);

                float cx = tf.x + tf.w / 2f;
                float cy = tf.y + tf.h / 2f;

                float facingAngle = directionToAngle(tf.direction);
                float coneHalf = 45f;
                float coneRadius = ai.detectionRange;

                Color coneColor;
                switch (ai.state) {
                    case CHASE:
                        coneColor = new Color(1f, 0.3f, 0f, 0.15f);
                        break;
                    case ATTACK:
                        coneColor = new Color(1f, 0f, 0f, 0.2f);
                        break;
                    default:
                        coneColor = new Color(0f, 1f, 0.3f, 0.12f);
                        break;
                }
                shapeRenderer.setColor(coneColor);

                int segments = 16;
                float startAngle = facingAngle - coneHalf;
                float step = (coneHalf * 2f) / segments;
                for (int s = 0; s < segments; s++) {
                    float a1 = startAngle + step * s;
                    float a2 = startAngle + step * (s + 1);
                    float x1 = cx + MathUtils.cosDeg(a1) * coneRadius;
                    float y1 = cy + MathUtils.sinDeg(a1) * coneRadius;
                    float x2 = cx + MathUtils.cosDeg(a2) * coneRadius;
                    float y2 = cy + MathUtils.sinDeg(a2) * coneRadius;
                    shapeRenderer.triangle(cx, cy, x1, y1, x2, y2);
                }
            }
            shapeRenderer.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
    }

    private void drawRect(Rectangle r) {
        shapeRenderer.rect(r.x, r.y, r.width, r.height);
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

    private static boolean isSpawnFxPose(String pose) {
        return "hidden1".equals(pose) || "hidden2".equals(pose)
            || "jump1".equals(pose)   || "jump2".equals(pose);
    }

    private boolean isVisible(TransformComponent t) {
        float halfW = camera.viewportWidth  * 0.5f + 64f;
        float halfH = camera.viewportHeight * 0.5f + 64f;
        float cx    = camera.position.x;
        float cy    = camera.position.y;
        return t.x + t.w > cx - halfW && t.x < cx + halfW
            && t.y + t.h > cy - halfH && t.y < cy + halfH;
    }

    @Override
    protected void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}
