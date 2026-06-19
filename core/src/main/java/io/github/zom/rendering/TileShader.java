package io.github.zom.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * Loads tile.vert / tile.frag and exposes helpers to bind the mask
 * texture (unit 1) and set biome tint uniforms before each sheet pass.
 */
public final class TileShader implements Disposable {

    /** Grass tint — adjust to taste. */
    public static final Color GRASS_COLOR = new Color(0.35f, 0.55f, 0.18f, 1f);
    /** Dirt tint. */
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

    /**
     * Bind maskTexture to GL texture unit 1 and set all uniforms.
     * Call after batch.setShader(program) but before any batch.draw() calls.
     */
    public void bindMask(Texture maskTexture) {
        maskTexture.bind(1);
        program.setUniformi("u_mask",    1); // sampler on unit 1
        program.setUniformi("u_texture", 0); // SpriteBatch always uses unit 0
        program.setUniformf("u_grassColor",
            GRASS_COLOR.r, GRASS_COLOR.g, GRASS_COLOR.b, GRASS_COLOR.a);
        program.setUniformf("u_dirtColor",
            DIRT_COLOR.r, DIRT_COLOR.g, DIRT_COLOR.b, DIRT_COLOR.a);
    }

    @Override
    public void dispose() { program.dispose(); }
}
