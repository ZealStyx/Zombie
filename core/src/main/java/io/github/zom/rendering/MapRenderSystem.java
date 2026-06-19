package io.github.zom.rendering;

import com.artemis.BaseSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.github.zom.world.ProceduralMapGenerator;
import io.github.zom.world.TileDesc;

/**
 * Artemis BaseSystem that draws the procedural tile map.
 * Registered BEFORE GameRenderSystem so tiles are drawn first (background layer).
 *
 * Draw strategy:
 *   - Camera frustum culling — only tiles inside the viewport (+1 tile margin).
 *   - Per-sheet passes (sheets 1→4) so each pass only binds one colour texture
 *     and one mask texture, minimising GL state switches.
 *   - Sub-regions cached via TextureCache.storeRegionByKey() to avoid per-frame
 *     TextureRegion allocation.
 */
public final class MapRenderSystem extends BaseSystem {

    public static final int TILE_PX = 60; // tile size in world pixels

    private final SpriteBatch        batch;
    private final OrthographicCamera camera;

    private ProceduralMapGenerator generator;
    private TileShader             tileShader;

    // Indexed 1–4; index 0 unused
    private final Texture[]       colourTex = new Texture[5];
    private final Texture[]       maskTex   = new Texture[5];

    public MapRenderSystem(SpriteBatch batch, OrthographicCamera camera) {
        this.batch  = batch;
        this.camera = camera;
    }

    /** Wire the generator before the ECS world processes its first frame. */
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

        // ── Frustum cull: compute visible tile range ──────────────────────────
        float halfW = camera.viewportWidth  * camera.zoom * 0.5f;
        float halfH = camera.viewportHeight * camera.zoom * 0.5f;

        int xMin = Math.max(0,    (int)((camera.position.x - halfW) / TILE_PX) - 1);
        int xMax = Math.min(mapW, (int)((camera.position.x + halfW) / TILE_PX) + 2);
        int yMin = Math.max(0,    (int)((camera.position.y - halfH) / TILE_PX) - 1);
        int yMax = Math.min(mapH, (int)((camera.position.y + halfH) / TILE_PX) + 2);

        // ── One pass per sheet to minimise texture/shader switches ────────────
        for (int sheet = 1; sheet <= 4; sheet++) {
            drawSheetPass(tiles, xMin, xMax, yMin, yMax, sheet);
        }
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
                    batch.setShader(tileShader.getProgram());
                    tileShader.bindMask(maskTex[targetSheet]);
                    shaderBound = true;
                }

                TextureRegion region = getOrCreateRegion(targetSheet, td.sheetCol, td.sheetRow);
                batch.draw(region, x * TILE_PX, y * TILE_PX, TILE_PX, TILE_PX);
            }
        }

        if (shaderBound) {
            batch.setShader(null); // restore default SpriteBatch shader
        }
    }

    /**
     * Returns (and permanently caches) the 60×60 TextureRegion for the given
     * sheet position. Key format: "tile:S:C:R".
     */
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
