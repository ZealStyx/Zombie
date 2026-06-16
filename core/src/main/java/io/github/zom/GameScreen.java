package io.github.zom;

import com.artemis.WorldConfiguration;
import com.artemis.WorldConfigurationBuilder;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.zom.net.GameClient;
import io.github.zom.system.AnimationStateSystem;
import io.github.zom.system.CollisionSystem;
import io.github.zom.system.CombatSystem;
import io.github.zom.system.DebugConsoleSystem;
import io.github.zom.system.HealthSystem;
import io.github.zom.system.ItemPickupSystem;
import io.github.zom.system.MovementSystem;
import io.github.zom.system.GameRenderSystem;
import io.github.zom.system.ZedAISystem;
import io.github.zom.util.EntityFactory;
import io.github.zom.world.WorldCollision;

/**
 * Main gameplay screen.
 *
 * System execution order each frame:
 * 1. AnimationStateSystem — advance stateTime, release finished one-shots
 * 2. MovementSystem — WASD input → velocity, direction, pose; sets combat flags
 * 3. ZedAISystem — steering + state machine (wander/chase/attack)
 * 4. CollisionSystem — resolve feet vs world for ALL entities
 * 5. CombatSystem — melee hitbox + hitscan, queue damage
 * 6. HealthSystem — process damage queues, trigger death animations
 * 7. ItemPickupSystem — F key: nearest item → inventory
 * 8. DebugConsoleSystem — render console overlay (no-op when closed)
 * 9. GameRenderSystem — draw all items, dead/alive zeds, and player sorted by
 * feet Y-coordinate
 *
 * PPU (pixels per world unit) = 1. Sprites render at native pixel size via
 * TransformComponent.w/h set from the actual texture dimensions in
 * EntityFactory.
 * The camera viewport is expressed in pixels, so 1 px = 1 unit on screen.
 *
 * InputMultiplexer: DebugConsoleSystem's Stage gets priority; game input is
 * second.
 */
public class GameScreen implements Screen {

    // Viewport in world units (= pixels at PPU=1)
    // At PPU=1, player sprite is 30 px wide → ~21 players across the screen
    // horizontally
    public static final float PPU = 1f;
    public static final int VP_W = 640;
    public static final int VP_H = 360;
    public static final int WORLD_W = 1280;
    public static final int WORLD_H = 720;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private com.artemis.World ecsWorld;
    private DebugConsoleSystem debugConsole;
    private io.github.zom.system.AndroidControllerSystem androidController;
    private io.github.zom.system.InventoryUiSystem inventoryUi;
    private InputMultiplexer inputMux;

    /** Optional network client for multiplayer mode. Null = single-player. */
    private final GameClient networkClient;

    /** Single-player constructor. */
    public GameScreen() {
        this(null);
    }

    /** Multiplayer constructor — pass an already-connected GameClient. */
    public GameScreen(GameClient client) {
        this.networkClient = client;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, VP_W, VP_H);

        initWorldCollision();

        debugConsole = new DebugConsoleSystem(camera);
        androidController = new io.github.zom.system.AndroidControllerSystem();
        inventoryUi = new io.github.zom.system.InventoryUiSystem();

        ItemPickupSystem itemPickupSystem = new ItemPickupSystem();
        GameRenderSystem gameRenderSystem = new GameRenderSystem(batch, camera);
        gameRenderSystem.setPickupSystem(itemPickupSystem);

        WorldConfiguration cfg = new WorldConfigurationBuilder()
                .with(
                        new AnimationStateSystem(),
                        androidController,
                        new MovementSystem(camera),
                        new ZedAISystem(),
                        new CollisionSystem(),
                        new CombatSystem(),
                        new HealthSystem(),
                        itemPickupSystem,
                        inventoryUi,
                        debugConsole,
                        gameRenderSystem)
                .build();

        ecsWorld = new com.artemis.World(cfg);

        // InputMultiplexer: debug console Stage first (captures when open), inventory Stage second, game input third
        inputMux = new InputMultiplexer();
        inputMux.addProcessor(debugConsole.getStage());
        inputMux.addProcessor(inventoryUi.getStage());
        Gdx.input.setInputProcessor(inputMux);

        spawnTestEntities();
    }

    private void initWorldCollision() {
        WorldCollision.clear();
        float wall = 8f; // 8 px border walls
        WorldCollision.add(0f, 0f, WORLD_W, wall);
        WorldCollision.add(0f, WORLD_H - wall, WORLD_W, wall);
        WorldCollision.add(0f, 0f, wall, WORLD_H);
        WorldCollision.add(WORLD_W - wall, 0f, wall, WORLD_H);

        // Some interior obstacles (in pixels)
        WorldCollision.add(320f, 200f, 80f, 16f);
        WorldCollision.add(600f, 350f, 16f, 120f);
        WorldCollision.add(200f, 400f, 130f, 16f);
    }

    private void spawnTestEntities() {
        float cx = WORLD_W / 2f;
        float cy = WORLD_H / 2f;

        EntityFactory.createPlayer(ecsWorld, cx, cy);

        EntityFactory.createZed(ecsWorld, cx - 80f, cy, "normal", "zed_normal_skin1", "zed_normal_skin1_dead");
        EntityFactory.createZed(ecsWorld, cx + 60f, cy, "fast", "zed_fast_skin1", "zed_fast_skin1_dead");
        EntityFactory.createZed(ecsWorld, cx, cy - 90f, "army", "zed_army_skin1", "zed_army_dead_skin1");

        // Dropped items near player — walk close and press F
        EntityFactory.createWorldItem(ecsWorld, cx - 20f, cy - 60f, 175, 1); // FNX 45
        EntityFactory.createWorldItem(ecsWorld, cx + 35f, cy - 60f, 1, 3); // Canned Beans
        EntityFactory.createWorldItem(ecsWorld, cx + 10f, cy - 90f, 192, 1); // Mosin
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        ecsWorld.setDelta(delta);
        ecsWorld.process();
        batch.end();

        // Stages draw after the SpriteBatch to stay on top
        inventoryUi.drawStage();
        debugConsole.drawStage(delta);
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0)
            return;
        camera.setToOrtho(false, VP_W, VP_H);
        debugConsole.resize(width, height);
        inventoryUi.resize(width, height);
        androidController.resize(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (ecsWorld != null)
            ecsWorld.dispose();
        if (batch != null)
            batch.dispose();
    }
}