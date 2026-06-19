package io.github.zom.world;

import io.github.zom.world.noise.OpenSimplex2;

/**
 * Full pipeline: FBm noise → boolean grid → cellular automaton smoothing
 *                         → 8-bit bitmask autotile → TileDesc[][].
 *
 * Noise is tuned for very low spatial frequency so biomes form large sweeping
 * landmasses rather than narrow strips the tileset cannot represent.
 */
public final class ProceduralMapGenerator {

    // ── Noise parameters ──────────────────────────────────────────────────────
    /** Spatial frequency. Lower = larger biome blobs. 0.003 ≈ 333-tile wavelength. */
    private static final double NOISE_SCALE      = 0.003;
    private static final int    NOISE_OCTAVES    = 4;
    private static final double NOISE_PERSIST    = 0.5;
    private static final double NOISE_LACUNARITY = 2.0;
    /** Noise values above this threshold become GRASS. */
    private static final float  GRASS_THRESHOLD  = 0.05f;

    /** Number of cellular-automaton smoothing passes to kill isolated blobs. */
    private static final int SMOOTH_PASSES = 2;

    private final int  mapW;
    private final int  mapH;
    private final long seed;

    private boolean[][]  isGrass; // intermediate boolean grid
    private TileDesc[][] tiles;   // final output

    public ProceduralMapGenerator(int mapW, int mapH, long seed) {
        this.mapW = mapW;
        this.mapH = mapH;
        this.seed = seed;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Run the full pipeline. Call once after construction; then getTiles().
     * Safe to call on a background thread before the ECS world is created.
     */
    public void generate() {
        isGrass = buildNoiseGrid();
        for (int i = 0; i < SMOOTH_PASSES; i++) smooth();
        tiles = autotile();
        isGrass = null; // release intermediate memory
    }

    public TileDesc[][] getTiles() { return tiles; }
    public int getMapW()           { return mapW;  }
    public int getMapH()           { return mapH;  }

    // ── Step 1: Noise grid ────────────────────────────────────────────────────

    private boolean[][] buildNoiseGrid() {
        boolean[][] grid = new boolean[mapW][mapH];
        for (int x = 0; x < mapW; x++) {
            for (int y = 0; y < mapH; y++) {
                grid[x][y] = (float) fbm(x, y) >= GRASS_THRESHOLD;
            }
        }
        return grid;
    }

    /** Fractal Brownian Motion — sums several octaves of OpenSimplex2 noise. */
    private double fbm(int x, int y) {
        double amplitude = 1.0;
        double frequency = 1.0;
        double total     = 0.0;
        double maxVal    = 0.0;
        for (int o = 0; o < NOISE_OCTAVES; o++) {
            double nx = x * NOISE_SCALE * frequency;
            double ny = y * NOISE_SCALE * frequency;
            total  += OpenSimplex2.noise2(seed + o * 12345L, nx, ny) * amplitude;
            maxVal += amplitude;
            amplitude *= NOISE_PERSIST;
            frequency *= NOISE_LACUNARITY;
        }
        return total / maxVal; // normalise to approx [-1, 1]
    }

    // ── Step 2: Cellular automaton smoothing ──────────────────────────────────

    /**
     * One smoothing pass.
     * ≥5 grass neighbours → force GRASS.
     * ≤3 grass neighbours → force DIRT.
     * Eliminates isolated 1-tile speckles the tileset can't represent.
     */
    private void smooth() {
        boolean[][] next = new boolean[mapW][mapH];
        for (int x = 0; x < mapW; x++) {
            for (int y = 0; y < mapH; y++) {
                int n = countGrassNeighbours8(x, y);
                if      (n >= 5) next[x][y] = true;
                else if (n <= 3) next[x][y] = false;
                else             next[x][y] = isGrass[x][y];
            }
        }
        isGrass = next;
    }

    private int countGrassNeighbours8(int x, int y) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                if (dx != 0 || dy != 0)
                    count += grassAt(x + dx, y + dy) ? 1 : 0;
        return count;
    }

    // ── Step 3: Autotile pass ─────────────────────────────────────────────────

    private TileDesc[][] autotile() {
        TileDesc[][] out = new TileDesc[mapW][mapH];
        for (int x = 0; x < mapW; x++) {
            for (int y = 0; y < mapH; y++) {
                int cardinal = buildCardinalMask(x, y);
                int diagonal = (cardinal == 0b1111) ? buildDiagonalMask(x, y) : 0xF;
                out[x][y] = TileAutotiler.resolve(cardinal, diagonal, x, y, seed);
            }
        }
        return out;
    }

    /**
     * 4-bit cardinal mask: N=bit3, E=bit2, S=bit1, W=bit0.
     * A set bit means that cardinal neighbour is GRASS.
     */
    private int buildCardinalMask(int x, int y) {
        return (grassAt(x,   y+1) ? 0b1000 : 0)
            | (grassAt(x+1, y  ) ? 0b0100 : 0)
            | (grassAt(x,   y-1) ? 0b0010 : 0)
            | (grassAt(x-1, y  ) ? 0b0001 : 0);
    }

    /**
     * 4-bit diagonal mask: NE=bit3, SE=bit2, SW=bit1, NW=bit0.
     * Only evaluated when all 4 cardinals are grass.
     */
    private int buildDiagonalMask(int x, int y) {
        return (grassAt(x+1, y+1) ? 0b1000 : 0)
            | (grassAt(x+1, y-1) ? 0b0100 : 0)
            | (grassAt(x-1, y-1) ? 0b0010 : 0)
            | (grassAt(x-1, y+1) ? 0b0001 : 0);
    }

    /** Out-of-bounds always returns true — map edges are clean grass border. */
    private boolean grassAt(int x, int y) {
        if (x < 0 || x >= mapW || y < 0 || y >= mapH) return true;
        return isGrass[x][y];
    }
}
