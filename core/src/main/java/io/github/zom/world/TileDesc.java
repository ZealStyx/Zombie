package io.github.zom.world;

/**
 * Immutable render descriptor for one map tile.
 *
 * sheet    : 1=border (3×3), 2=inner corner (2×2),
 *            3=dirt variants (2×2), 4=grass variants (2×2)
 * sheetCol : tile column inside that sheet (0-based, 60 px each)
 * sheetRow : tile row    inside that sheet (0-based, 60 px each)
 */
public final class TileDesc {

    public final int sheet;
    public final int sheetCol;
    public final int sheetRow;

    public TileDesc(int sheet, int sheetCol, int sheetRow) {
        this.sheet    = sheet;
        this.sheetCol = sheetCol;
        this.sheetRow = sheetRow;
    }
}
