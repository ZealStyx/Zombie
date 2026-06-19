package io.github.zom.world;

/**
 * Converts an 8-bit neighbour bitmask into a TileDesc.
 *
 * Cardinal mask (bits 3-0):  N=3  E=2  S=1  W=0   (1 = grass neighbour)
 * Diagonal mask (bits 3-0): NE=3 SE=2 SW=1 NW=0   (1 = grass diagonal)
 *   — diagonals only evaluated when cardinal mask == 0b1111.
 *
 * Sheet layout:
 *   Sheet 1 — 3×3 border:
 *     (0,0)=NW corner  (1,0)=N edge     (2,0)=NE corner
 *     (0,1)=W edge     (1,1)=full grass  (2,1)=E edge
 *     (0,2)=SW corner  (1,2)=S edge     (2,2)=SE corner
 *
 *   Sheet 2 — 2×2 inner corner (concave, grass wraps around a dirt vertex):
 *     (0,0)=NW concave  (1,0)=NE concave
 *     (0,1)=SW concave  (1,1)=SE concave
 *
 *   Sheet 3 — 2×2 dirt variants  (skip (1,1))
 *   Sheet 4 — 2×2 grass variants (skip (1,1))
 *
 * Out-of-bounds neighbours are treated as GRASS (map edges are clean grass).
 */
public final class TileAutotiler {

    // Pre-built 16-entry table indexed by 4-bit cardinal mask.
    // null = handled by variant or diagonal logic (see resolve()).
    private static final TileDesc[] CARDINAL = new TileDesc[16];

    static {
        // ── All grass / all dirt — handled separately ─────────────────────────
        CARDINAL[0b1111] = null;  // → diagonal check then grass variant
        CARDINAL[0b0000] = null;  // → dirt variant

        // ── Single edge (one cardinal direction is dirt) ───────────────────────
        CARDINAL[0b1110] = new TileDesc(1, 0, 1); // W edge
        CARDINAL[0b1101] = new TileDesc(1, 1, 2); // S edge
        CARDINAL[0b1011] = new TileDesc(1, 2, 1); // E edge
        CARDINAL[0b0111] = new TileDesc(1, 1, 0); // N edge

        // ── Two adjacent cardinals dirt (outer corners) ───────────────────────
        CARDINAL[0b1100] = new TileDesc(1, 2, 0); // S+W dirt → NE outer corner
        CARDINAL[0b0110] = new TileDesc(1, 2, 2); // N+W dirt → SE outer corner
        CARDINAL[0b0011] = new TileDesc(1, 0, 2); // N+E dirt → SW outer corner
        CARDINAL[0b1001] = new TileDesc(1, 0, 0); // S+E dirt → NW outer corner

        // ── Unsupported (thin strips / peninsulas) → safest fallback = dirt ───
        // These should never appear after the cellular-automaton smoothing pass.
        CARDINAL[0b1010] = null; // E+W grass strip
        CARDINAL[0b0101] = null; // N+S grass strip
        CARDINAL[0b1000] = null; // only N is grass
        CARDINAL[0b0100] = null; // only E is grass
        CARDINAL[0b0010] = null; // only S is grass
        CARDINAL[0b0001] = null; // only W is grass
    }

    /**
     * Resolve the TileDesc for one tile.
     *
     * @param cardinalMask 4-bit: N=3 E=2 S=1 W=0 (1=grass neighbour)
     * @param diagonalMask 4-bit: NE=3 SE=2 SW=1 NW=0 (only used when cardinal==1111)
     * @param x            tile column (for seeded variant hash)
     * @param y            tile row    (for seeded variant hash)
     * @param seed         map seed    (for seeded variant hash)
     */
    public static TileDesc resolve(int cardinalMask, int diagonalMask,
                                   int x, int y, long seed) {
        if (cardinalMask == 0b1111) {
            return resolveFullGrass(diagonalMask, x, y, seed);
        }
        if (cardinalMask == 0b0000) {
            return dirtVariant(x, y, seed);
        }

        TileDesc entry = CARDINAL[cardinalMask];
        if (entry != null) return entry;

        // Unsupported thin-strip / peninsula — fall back to dirt
        return dirtVariant(x, y, seed);
    }

    // ── All-grass cases ───────────────────────────────────────────────────────

    private static TileDesc resolveFullGrass(int diagonalMask, int x, int y, long seed) {
        // A 0-bit in the diagonal mask means that diagonal is DIRT → concave corner.
        // Priority: NE → SE → SW → NW (first dirty diagonal wins).
        if ((diagonalMask & 0b1000) == 0) return new TileDesc(2, 1, 0); // NE concave
        if ((diagonalMask & 0b0100) == 0) return new TileDesc(2, 1, 1); // SE concave
        if ((diagonalMask & 0b0010) == 0) return new TileDesc(2, 0, 1); // SW concave
        if ((diagonalMask & 0b0001) == 0) return new TileDesc(2, 0, 0); // NW concave
        // All diagonals are grass too → pure grass variant
        return grassVariant(x, y, seed);
    }

    // ── Variant selection ─────────────────────────────────────────────────────

    /**
     * Picks one of the 3 usable variant cells deterministically from a
     * position-seeded hash. Cell (1,1) is always skipped (bottom-right is empty).
     *   variant 0 → (col=0, row=0)
     *   variant 1 → (col=1, row=0)
     *   variant 2 → (col=0, row=1)
     */
    private static TileDesc variantOf(int sheet, int x, int y, long seed) {
        int hash = (x * 73856093) ^ (y * 19349663) ^ (int)(seed & 0xFFFFFFFFL);
        int v    = Math.abs(hash) % 3;
        int col  = (v == 1) ? 1 : 0;
        int row  = (v == 2) ? 1 : 0;
        return new TileDesc(sheet, col, row);
    }

    private static TileDesc grassVariant(int x, int y, long seed) { return variantOf(4, x, y, seed); }
    private static TileDesc dirtVariant (int x, int y, long seed) { return variantOf(3, x, y, seed); }
}
