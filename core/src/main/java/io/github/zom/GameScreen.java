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
import io.github.zom.rendering.MapRenderSystem;
import io.github.zom.system.AnimationStateSystem;
import io.github.zom.system.AndroidControllerSystem;
import io.github.zom.system.CollisionSystem;
import io.github.zom.system.CombatSystem;
import io.github.zom.system.DebugConsoleSystem;
import io.github.zom.system.GameRenderSystem;
import io.github.zom.system.HealthSystem;
import io.github.zom.system.InventoryUiSystem;
import io.github.zom.system.MovementSystem;
import io.github.zom.system.ZedAISystem;
import io.github.zom.util.EntityFactory;
import io.github.zom.world.ProceduralMapGenerator;
import io.github.zom.world.WorldCollision;

/**
 * Main gameplay screen.
 *
 * System execution order each frame:
 * 1.  AnimationStateSystem
 * 2.  AndroidControllerSystem
 * 3.  MovementSystem
 * 4.  ZedAISystem
 * 5.  CollisionSystem
 * 6.  CombatSystem
 * 7.  HealthSystem
 * 8.  InventoryUiSystem
 * 9.  DebugConsoleSystem
 * 10. MapRenderSystem    ← tiles drawn first (background layer)
 * 11. GameRenderSystem   ← entities drawn on top
 */
public class GameScreen implements Screen {

    public static final float PPU    = 1f;
    public static final int   VP_W   = 640;
    public static final int   VP_H   = 360;

    /** Map size in tiles. 64×64 = 3840×3840 world pixels. */
    public static final int MAP_TILES_W = 64;
    public static final int MAP_TILES_H = 64;

    /** World size in pixels derived from tile count. */
    public static final int WORLD_W = MAP_TILES_W * MapRenderSystem.TILE_PX; // 3840
    public static final int WORLD_H = MAP_TILES_H * MapRenderSystem.TILE_PX; // 3840

    private SpriteBatch  batch;
    private OrthographicCamera camera;
    private com.artemis.World  ecsWorld;

    private DebugConsoleSystem    debugConsole;
    private AndroidControllerSystem androidController;
    private InventoryUiSystem     inventoryUi;
    private InputMultiplexer      inputMux;

    private final GameClient networkClient;

    public GameScreen()                   { this(null); }
    public GameScreen(GameClient client)  { this.networkClient = client; }

    @Override
    public void show() {
        batch  = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, VP_W, VP_H);

        // ── Generate the map BEFORE building the ECS world ────────────────────
        long seed = System.currentTimeMillis();
        ProceduralMapGenerator mapGen =
            new ProceduralMapGenerator(MAP_TILES_W, MAP_TILES_H, seed);
        mapGen.generate();
        Gdx.app.log("GameScreen", "Map generated: "
            + MAP_TILES_W + "×" + MAP_TILES_H + " tiles  seed=" + seed);

        // ── Collision walls sized to the tile map ─────────────────────────────
        initWorldCollision();

        // ── Systems ───────────────────────────────────────────────────────────
        debugConsole      = new DebugConsoleSystem(camera);
        androidController = new AndroidControllerSystem();
        inventoryUi       = new InventoryUiSystem();

        MapRenderSystem  mapRenderSystem  = new MapRenderSystem(batch, camera);
        mapRenderSystem.setGenerator(mapGen);

        GameRenderSystem gameRenderSystem = new GameRenderSystem(batch, camera);

        WorldConfiguration cfg = new WorldConfigurationBuilder()
            .with(
                new AnimationStateSystem(),
                androidController,
                new MovementSystem(camera),
                new ZedAISystem(),
                new CollisionSystem(),
                new CombatSystem(),
                new HealthSystem(),
                inventoryUi,
                debugConsole,
                mapRenderSystem,    // ← background tiles
                gameRenderSystem)   // ← entities on top
            .build();

        ecsWorld = new com.artemis.World(cfg);

        inputMux = new InputMultiplexer();
        inputMux.addProcessor(debugConsole.getStage());
        inputMux.addProcessor(inventoryUi.getStage());
        Gdx.input.setInputProcessor(inputMux);

        spawnTestEntities();
    }

    private void initWorldCollision() {
        WorldCollision.clear();
        float wall = 8f;
        // Border walls sized to full map
        WorldCollision.add(0f,             0f,              WORLD_W, wall);
        WorldCollision.add(0f,             WORLD_H - wall,  WORLD_W, wall);
        WorldCollision.add(0f,             0f,              wall,    WORLD_H);
        WorldCollision.add(WORLD_W - wall, 0f,              wall,    WORLD_H);

        // Interior test obstacles (kept from original, still work at any world size)
        WorldCollision.add(320f, 200f,  80f,  16f);
        WorldCollision.add(600f, 350f,  16f, 120f);
        WorldCollision.add(200f, 400f, 130f,  16f);
    }

    private void spawnTestEntities() {
        // Spawn in the centre of the tile map
        float cx = WORLD_W * 0.5f;
        float cy = WORLD_H * 0.5f;

        EntityFactory.createPlayer(ecsWorld, cx, cy);

        EntityFactory.createZed(ecsWorld, cx - 80f, cy,      "normal", "zed_normal_skin1",  "zed_normal_skin1_dead");
        EntityFactory.createZed(ecsWorld, cx + 60f, cy,      "fast",   "zed_fast_skin1",    "zed_fast_skin1_dead");
        EntityFactory.createZed(ecsWorld, cx,        cy - 90f,"army",   "zed_army_skin1",    "zed_army_dead_skin1");

        EntityFactory.createWorldItem(ecsWorld, cx - 20f, cy - 60f, 175, 1); // FNX 45
        EntityFactory.createWorldItem(ecsWorld, cx + 35f, cy - 60f, 1,   3); // Canned Beans
        EntityFactory.createWorldItem(ecsWorld, cx + 10f, cy - 90f, 192, 1); // Mosin-Nagant
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

        inventoryUi.drawStage();
        debugConsole.drawStage(delta);
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        camera.setToOrtho(false, VP_W, VP_H);
        debugConsole.resize(width, height);
        inventoryUi.resize(width, height);
        androidController.resize(width, height);
    }

    @Override public void pause()  {}
    @Override public void resume() {}

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        if (ecsWorld != null) ecsWorld.dispose();
        if (batch    != null) batch.dispose();
    }
}
