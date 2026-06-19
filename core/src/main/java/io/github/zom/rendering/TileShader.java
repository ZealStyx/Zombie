package io.github.zom.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

public final class TileShader implements Disposable {

    public static final Color GRASS_COLOR = new Color(0.35f, 0.55f, 0.18f, 1f);
    public static final Color DIRT_COLOR  = new Color(0.52f, 0.38f, 0.22f, 1f);

    private final ShaderProgram program;

    public TileShader() {
        ShaderProgram.pedantic = false;
        String vert = Gdx.files.internal("shaders/tile.vert").readString();
        String frag = Gdx.files.internal("shaders/tile.frag").readString();
        program = new ShaderProgram(vert, frag);
        if (!program.isCompiled())
            throw new RuntimeException("TileShader compile error:\n" + program.getLog());
    }

    public ShaderProgram getProgram() { return program; }

    public void bindMask(Texture maskTexture) {
        // Bind the mask to texture unit 1
        maskTexture.bind(1);

        // ── CRITICAL FIX ────────────────────────────────────────────────────
        // Texture.bind(1) leaves GL_TEXTURE1 as the active unit.
        // SpriteBatch.draw() calls texture.bind() with no unit argument, which
        // binds to whatever unit is currently active. If we leave it on unit 1,
        // SpriteBatch uploads the colour sheet to unit 1 (overwriting the mask)
        // and unit 0 stays stale. This makes tiles render as solid colour and
        // all subsequent sprites render using the tile sheet.
        // Restore unit 0 as active immediately so SpriteBatch works correctly.
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        // ────────────────────────────────────────────────────────────────────

        program.setUniformi("u_mask",    1);
        program.setUniformi("u_texture", 0);
        program.setUniformf("u_grassColor",
            GRASS_COLOR.r, GRASS_COLOR.g, GRASS_COLOR.b, GRASS_COLOR.a);
        program.setUniformf("u_dirtColor",
            DIRT_COLOR.r, DIRT_COLOR.g, DIRT_COLOR.b, DIRT_COLOR.a);
    }

    @Override
    public void dispose() { program.dispose(); }
}
