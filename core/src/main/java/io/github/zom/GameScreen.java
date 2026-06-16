package io.github.zom;

import com.artemis.WorldConfiguration;
import com.artemis.WorldConfigurationBuilder;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.zom.system.AnimationStateSystem;
import io.github.zom.system.CollisionSystem;
import io.github.zom.system.ItemPickupSystem;
import io.github.zom.system.MovementSystem;
import io.github.zom.system.PlayerRenderSystem;
import io.github.zom.system.WorldItemRenderSystem;
import io.github.zom.system.ZedRenderSystem;
import io.github.zom.util.EntityFactory;
import io.github.zom.world.WorldCollision;

/**
 * Main gameplay screen.
 *
 * System execution order each frame:
 *   1. AnimationStateSystem   — advance stateTime, release finished one-shots
 *   2. MovementSystem         — WASD input → velocity, direction, pose
 *   3. CollisionSystem        — resolve feet-box vs world obstacles, update all 3 collision rects
 *   4. ItemPickupSystem       — F key: scan nearby world items, add to inventory, destroy entity
 *   5. WorldItemRenderSystem  — draw dropped items (behind characters)
 *   6. ZedRenderSystem        — draw zeds
 *   7. PlayerRenderSystem     — draw player on top
 *
 * Camera: 1 world-unit ≈ one character tile (64 px at 1× scale).
 * The camera follows the player via MovementSystem.
 */
public class GameScreen implements Screen {

    private static final int VP_W = 20;   // viewport width  in world units
    private static final int VP_H = 12;   // viewport height in world units
    private static final int WORLD_W = 40;
    private static final int WORLD_H = 24;

    private SpriteBatch        batch;
    private OrthographicCamera camera;
    private com.artemis.World  ecsWorld;

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);

        batch  = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, VP_W, VP_H);

        initWorldCollision();

        WorldConfiguration cfg = new WorldConfigurationBuilder()
            .with(
                new AnimationStateSystem(),
                new MovementSystem(camera),
                new CollisionSystem(),
                new ItemPickupSystem(),
                new WorldItemRenderSystem(batch),
                new ZedRenderSystem(batch),
                new PlayerRenderSystem(batch)
            )
            .build();

        ecsWorld = new com.artemis.World(cfg);

        spawnTestEntities();
    }

    private void initWorldCollision() {
        WorldCollision.clear();

        float wall = 0.5f;
        WorldCollision.add(0f, 0f, WORLD_W, wall);
        WorldCollision.add(0f, WORLD_H - wall, WORLD_W, wall);
        WorldCollision.add(0f, 0f, wall, WORLD_H);
        WorldCollision.add(WORLD_W - wall, 0f, wall, WORLD_H);

        WorldCollision.add(12f, 8f, 3f, 0.5f);
        WorldCollision.add(24f, 14f, 0.5f, 4f);
        WorldCollision.add(8f, 16f, 5f, 0.5f);
    }

    private void spawnTestEntities() {
        // Player at centre
        EntityFactory.createPlayer(ecsWorld, WORLD_W / 2f - 0.5f, WORLD_H / 2f - 0.5f);

        // Two test zeds
        EntityFactory.createZed(ecsWorld,
            WORLD_W / 2f - 3f, WORLD_H / 2f - 0.5f,
            "normal", "zed_normal_skin1", "zed_normal_skin1_dead");

        EntityFactory.createZed(ecsWorld,
            WORLD_W / 2f + 2f, WORLD_H / 2f - 0.5f,
            "fast", "zed_fast_skin1", "zed_fast_skin1_dead");

        // Dropped items — walk near them and press F
        EntityFactory.createWorldItem(ecsWorld, WORLD_W / 2f - 0.3f, WORLD_H / 2f - 2f, 175, 1);  // FNX 45
        EntityFactory.createWorldItem(ecsWorld, WORLD_W / 2f + 1.2f, WORLD_H / 2f - 2f, 1,   3);  // Canned Beans
        EntityFactory.createWorldItem(ecsWorld, WORLD_W / 2f + 0.4f, WORLD_H / 2f - 3f, 192, 1);  // Mosin
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
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        camera.setToOrtho(false, VP_W, VP_H);
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        if (ecsWorld != null) ecsWorld.dispose();
        if (batch    != null) batch.dispose();
    }
}
