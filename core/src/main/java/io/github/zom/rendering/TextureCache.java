package io.github.zom.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Lazily loads and caches Texture objects keyed by asset-relative path.
 * All textures use Nearest filtering for crisp pixel art.
 * Call dispose() on shutdown to release all GPU memory.
 */
public final class TextureCache implements Disposable {

    private static TextureCache instance;

    private final ObjectMap<String, Texture> cache = new ObjectMap<>(512);

    private TextureCache() {}

    public static TextureCache get() {
        if (instance == null) instance = new TextureCache();
        return instance;
    }

    /** Load (or retrieve cached) Texture for the given asset-relative path. */
    public Texture texture(String path) {
        Texture t = cache.get(path);
        if (t == null) {
            t = new Texture(Gdx.files.internal(path));
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            cache.put(path, t);
        }
        return t;
    }

    /** Convenience: full-texture TextureRegion. */
    public TextureRegion region(String path) {
        return new TextureRegion(texture(path));
    }

    /** Build an ordered Array of TextureRegions from a list of frame paths. */
    public Array<TextureRegion> regions(Array<String> paths) {
        Array<TextureRegion> out = new Array<>(true, paths.size, TextureRegion.class);
        for (String p : paths) out.add(region(p));
        return out;
    }

    @Override
    public void dispose() {
        for (Texture t : cache.values()) t.dispose();
        cache.clear();
        instance = null;
    }
}
