package io.github.zom.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Loads and caches BitmapFont instances generated from the RobotoMono TTF files.
 *
 * Font files in assets:
 *   ui/fonts/RobotoMono-Regular.ttf
 *   ui/fonts/RobotoMono-Bold.ttf
 *   ui/fonts/RobotoMono-Italic.ttf
 *   ui/fonts/RobotoMono-BoldItalic.ttf
 *
 * Usage:
 *   BitmapFont f = FontCache.get().regular(12);
 *   BitmapFont b = FontCache.get().bold(14);
 *
 * Fonts use Nearest filtering to match the pixel-art aesthetic.
 * Call dispose() on shutdown (handled by Main).
 */
public final class FontCache implements Disposable {

    public enum Style { REGULAR, BOLD, ITALIC, BOLD_ITALIC }

    private static FontCache instance;

    private FreeTypeFontGenerator regular;
    private FreeTypeFontGenerator bold;
    private FreeTypeFontGenerator italic;
    private FreeTypeFontGenerator boldItalic;

    /** Cache key: "STYLE:size" → BitmapFont */
    private final ObjectMap<String, BitmapFont> cache = new ObjectMap<>(32);

    private FontCache() {}

    public static FontCache get() {
        if (instance == null) {
            instance = new FontCache();
        }
        return instance;
    }

    /** Call once from Main.create() after Gdx is initialised. */
    public static void load() {
        FontCache fc = get();
        fc.regular    = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/RobotoMono-Regular.ttf"));
        fc.bold       = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/RobotoMono-Bold.ttf"));
        fc.italic     = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/RobotoMono-Italic.ttf"));
        fc.boldItalic = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/RobotoMono-BoldItalic.ttf"));
    }

    // ── Public convenience methods ────────────────────────────────────────────

    public BitmapFont regular(int size)    { return font(Style.REGULAR,     size); }
    public BitmapFont bold(int size)       { return font(Style.BOLD,        size); }
    public BitmapFont italic(int size)     { return font(Style.ITALIC,      size); }
    public BitmapFont boldItalic(int size) { return font(Style.BOLD_ITALIC, size); }

    /** Lazy-generate and cache a BitmapFont for the given style + size (in pixels). */
    public BitmapFont font(Style style, int size) {
        String key = style.name() + ":" + size;
        BitmapFont cached = cache.get(key);
        if (cached != null) return cached;

        FreeTypeFontGenerator gen = generatorFor(style);
        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.size = size;
        // Nearest-neighbour to stay crisp at pixel-art scale
        param.minFilter  = com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest;
        param.magFilter  = com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest;
        param.mono       = false;

        BitmapFont font = gen.generateFont(param);
        font.getData().markupEnabled = false;
        cache.put(key, font);
        return font;
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private FreeTypeFontGenerator generatorFor(Style style) {
        switch (style) {
            case BOLD:        return bold;
            case ITALIC:      return italic;
            case BOLD_ITALIC: return boldItalic;
            default:          return regular;
        }
    }

    @Override
    public void dispose() {
        for (BitmapFont f : cache.values()) f.dispose();
        cache.clear();
        if (regular    != null) regular.dispose();
        if (bold       != null) bold.dispose();
        if (italic     != null) italic.dispose();
        if (boldItalic != null) boldItalic.dispose();
        instance = null;
    }
}
