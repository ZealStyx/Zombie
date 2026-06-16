package io.github.zom;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

import io.github.zom.config.ConfigLoader;
import io.github.zom.rendering.FontCache;
import io.github.zom.rendering.TextureCache;

/**
 * Application entry point.
 *
 * Startup:  ConfigLoader.load() → FontCache.load() → MainMenuScreen
 * Shutdown: screen.dispose() → FontCache.dispose() → TextureCache.dispose()
 */
public class Main extends Game {

    @Override
    public void create() {
        ConfigLoader.load();
        FontCache.load();
        Gdx.app.log("Main", "Assets loaded — launching main menu.");
        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        FontCache.get().dispose();
        TextureCache.get().dispose();
    }
}
