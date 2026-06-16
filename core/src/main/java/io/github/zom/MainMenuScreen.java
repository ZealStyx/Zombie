package io.github.zom;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import io.github.zom.rendering.FontCache;

/**
 * Title screen with Play and Quit buttons.
 *
 * FIX 1.1: Stage and Skin are disposed in hide() rather than only in dispose(),
 * because Game.setScreen() calls hide() but not dispose() on the outgoing screen.
 */
public class MainMenuScreen implements Screen {

    private final Game game;
    private Stage stage;
    private Skin  skin;

    public MainMenuScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        BitmapFont titleFont  = FontCache.get().bold(22);
        BitmapFont buttonFont = FontCache.get().regular(14);

        Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        titleStyle.font = titleFont;
        titleStyle.fontColor = skin.getColor("white");

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
        buttonStyle.font = buttonFont;

        Table root = new Table();
        root.setFillParent(true);

        Label title = new Label("ZOMBIE", titleStyle);
        title.setFontScale(2f);

        TextButton playBtn = new TextButton("Play", buttonStyle);
        playBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen());
            }
        });

        TextButton quitBtn = new TextButton("Quit", buttonStyle);
        quitBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        root.add(title).padBottom(48f).row();
        root.add(playBtn).width(220f).height(48f).padBottom(12f).row();
        root.add(quitBtn).width(220f).height(48f);

        stage.addActor(root);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.06f, 0.06f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause()  {}
    @Override public void resume() {}

    /**
     * FIX 1.1 — Release resources here so they are freed when the game
     * transitions to GameScreen (Game.setScreen calls hide, not dispose).
     */
    @Override
    public void hide() {
        if (stage != null) { stage.dispose(); stage = null; }
        if (skin  != null) { skin.dispose();  skin  = null; }
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        hide(); // idempotent — safe to call twice
    }
}
