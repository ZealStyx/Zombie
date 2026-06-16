package io.github.zom;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import io.github.zom.net.GameClient;
import io.github.zom.net.Protocol;
import io.github.zom.rendering.FontCache;

/**
 * Title screen with Play, Join Server, and Quit buttons.
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

        TextButton joinBtn = new TextButton("Join Server", buttonStyle);
        joinBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                showJoinDialog(buttonStyle);
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
        root.add(joinBtn).width(220f).height(48f).padBottom(12f).row();
        root.add(quitBtn).width(220f).height(48f);

        stage.addActor(root);
    }

    private void showJoinDialog(TextButton.TextButtonStyle buttonStyle) {
        Dialog dialog = new Dialog("Join Server", skin);

        Label.LabelStyle labelStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        labelStyle.font = FontCache.get().regular(11);

        Label ipLabel = new Label("Server IP:Port", labelStyle);
        TextField ipField = new TextField("127.0.0.1:" + Protocol.DEFAULT_PORT, skin);
        ipField.setMaxLength(40);

        Label statusLabel = new Label("", labelStyle);

        TextButton connectBtn = new TextButton("Connect", buttonStyle);
        connectBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                String input = ipField.getText().trim();
                String host = "127.0.0.1";
                int port = Protocol.DEFAULT_PORT;

                if (input.contains(":")) {
                    String[] parts = input.split(":");
                    host = parts[0];
                    try { port = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                } else if (!input.isEmpty()) {
                    host = input;
                }

                statusLabel.setText("Connecting...");

                // Connect on a background thread to avoid freezing the UI
                final String fHost = host;
                final int fPort = port;
                new Thread(() -> {
                    GameClient client = new GameClient();
                    boolean ok = client.connect(fHost, fPort, "Player");
                    Gdx.app.postRunnable(() -> {
                        if (ok) {
                            dialog.hide();
                            game.setScreen(new GameScreen(client));
                        } else {
                            statusLabel.setText(client.getRejectReason());
                        }
                    });
                }, "JoinConnect").start();
            }
        });

        TextButton cancelBtn = new TextButton("Cancel", buttonStyle);
        cancelBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });

        dialog.getContentTable().add(ipLabel).padBottom(8f).row();
        dialog.getContentTable().add(ipField).width(280f).padBottom(8f).row();
        dialog.getContentTable().add(statusLabel).padBottom(8f).row();

        Table btnTable = new Table();
        btnTable.add(connectBtn).width(130f).height(40f).padRight(8f);
        btnTable.add(cancelBtn).width(130f).height(40f);
        dialog.getContentTable().add(btnTable).row();

        dialog.show(stage);
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
