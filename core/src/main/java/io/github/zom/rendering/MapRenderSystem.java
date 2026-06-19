package io.github.zom.rendering;

import com.artemis.BaseSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.github.zom.world.ProceduralMapGenerator;
import io.github.zom.world.TileDesc;

public final class MapRenderSystem extends BaseSystem {

    public static final int TILE_PX = 60;

    private final SpriteBatch        batch;
    private final OrthographicCamera camera;

    private ProceduralMapGenerator generator;
    private TileShader             tileShader;

    private final Texture[] colourTex = new Texture[5];
    private final Texture[] maskTex   = new Texture[5];

    public MapRenderSystem(SpriteBatch batch, OrthographicCamera camera) {
        this.batch  = batch;
        this.camera = camera;
    }

    public void setGenerator(ProceduralMapGenerator gen) {
        this.generator = gen;
    }

    @Override
    protected void initialize() {
        tileShader = new TileShader();
        for (int i = 1; i <= 4; i++) {
            colourTex[i] = TextureCache.get().texture("tiles/" + i + ".png");
            maskTex[i]   = TextureCache.get().texture("tiles/" + i + "_m.png");
        }
    }

    @Override
    protected void processSystem() {
        if (generator == null) return;

        TileDesc[][] tiles = generator.getTiles();
        int mapW = generator.getMapW();
        int mapH = generator.getMapH();

        float halfW = camera.viewportWidth  * camera.zoom * 0.5f;
        float halfH = camera.viewportHeight * camera.zoom * 0.5f;

        int xMin = Math.max(0,    (int)((camera.position.x - halfW) / TILE_PX) - 1);
        int xMax = Math.min(mapW, (int)((camera.position.x + halfW) / TILE_PX) + 2);
        int yMin = Math.max(0,    (int)((camera.position.y - halfH) / TILE_PX) - 1);
        int yMax = Math.min(mapH, (int)((camera.position.y + halfH) / TILE_PX) + 2);

        for (int sheet = 1; sheet <= 4; sheet++) {
            drawSheetPass(tiles, xMin, xMax, yMin, yMax, sheet);
        }

        // ── CRITICAL FIX ────────────────────────────────────────────────────
        // After all tile passes, flush any remaining geometry and restore the
        // default shader explicitly so GameRenderSystem starts clean.
        // batch.setShader(null) inside drawSheetPass already does this per-pass,
        // but we do a final flush here to guarantee the state is clean for the
        // next system (GameRenderSystem) regardless of which pass ran last.
        batch.flush();
        batch.setShader(null);
        // ────────────────────────────────────────────────────────────────────
    }

    private void drawSheetPass(TileDesc[][] tiles,
                               int xMin, int xMax, int yMin, int yMax,
                               int targetSheet) {
        boolean shaderBound = false;

        for (int x = xMin; x < xMax; x++) {
            for (int y = yMin; y < yMax; y++) {
                TileDesc td = tiles[x][y];
                if (td == null || td.sheet != targetSheet) continue;

                if (!shaderBound) {
                    // ── CRITICAL FIX ────────────────────────────────────────
                    // Flush pending geometry BEFORE switching the shader so the
                    // previous batch contents are submitted with the old shader,
                    // not accidentally mixed with the new tile shader.
                    batch.flush();
                    // ────────────────────────────────────────────────────────
                    batch.setShader(tileShader.getProgram());
                    tileShader.bindMask(maskTex[targetSheet]);
                    shaderBound = true;
                }

                TextureRegion region = getOrCreateRegion(targetSheet, td.sheetCol, td.sheetRow);
                batch.draw(region, x * TILE_PX, y * TILE_PX, TILE_PX, TILE_PX);
            }
        }

        if (shaderBound) {
            batch.flush();           // flush tile geometry with tile shader
            batch.setShader(null);   // restore default shader for next pass
        }
    }

    private TextureRegion getOrCreateRegion(int sheet, int col, int row) {
        String key = "tile:" + sheet + ":" + col + ":" + row;
        TextureRegion cached = TextureCache.get().cachedRegionByKey(key);
        if (cached == null) {
            cached = new TextureRegion(colourTex[sheet],
                col * TILE_PX, row * TILE_PX, TILE_PX, TILE_PX);
            TextureCache.get().storeRegionByKey(key, cached);
        }
        return cached;
    }

    @Override
    protected void dispose() {
        if (tileShader != null) tileShader.dispose();
    }
}
