package io.github.zom;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

import io.github.zom.config.ConfigLoader;
import io.github.zom.rendering.TextureCache;

/**
 * Application entry point shared by all platforms.
 *
 * Startup order:
 *   1. ConfigLoader.load()   — parses all 4 JSON configs into memory once
 *   2. setScreen(GameScreen) — GameScreen builds the ECS world and registers systems
 *
 * Shutdown order (via dispose):
 *   1. Current screen dispose  (ECS world + SpriteBatch)
 *   2. TextureCache.dispose()  — frees every GPU texture
 */
public class Main extends Game {

    @Override
    public void create() {
        ConfigLoader.load();
        Gdx.app.log("Main", "Configs loaded — launching MainMenuScreen.");
        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        TextureCache.get().dispose();
    }
}
