package io.github.zom.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Lazy singleton — loads each PNG exactly once, Nearest-filtered for pixel art.
 *
 * FIX 1.3: Adds a regionCache so region(path) returns the same TextureRegion
 * object on every call, eliminating per-frame heap allocation from render systems.
 *
 * Call dispose() on shutdown (handled by Main).
 */
public final class TextureCache implements Disposable {

    private static TextureCache instance;

    private final ObjectMap<String, Texture>       textureCache = new ObjectMap<>(512);
    private final ObjectMap<String, TextureRegion> regionCache  = new ObjectMap<>(512);

    private TextureCache() {}

    public static TextureCache get() {
        if (instance == null) instance = new TextureCache();
        return instance;
    }

    /** Load-or-retrieve the Texture for the given asset-relative path. */
    public Texture texture(String path) {
        Texture t = textureCache.get(path);
        if (t == null) {
            t = new Texture(Gdx.files.internal(path));
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            textureCache.put(path, t);
        }
        return t;
    }

    /**
     * Load-or-retrieve a cached full-texture TextureRegion.
     * FIX 1.3: returns the same object on subsequent calls — no allocation.
     */
    public TextureRegion region(String path) {
        TextureRegion r = regionCache.get(path);
        if (r == null) {
            r = new TextureRegion(texture(path));
            regionCache.put(path, r);
        }
        return r;
    }

    /**
     * Build an Array of TextureRegions from a list of frame paths.
     * Each element is the same cached object returned by region(path).
     */
    public Array<TextureRegion> regions(Array<String> paths) {
        Array<TextureRegion> out = new Array<>(true, paths.size, TextureRegion.class);
        for (String p : paths) out.add(region(p));
        return out;
    }

    /**
     * Store an arbitrary TextureRegion under a synthetic key.
     * Used by MapRenderSystem to cache sheet sub-regions without allocating
     * a new TextureRegion on every draw call.
     */
    public void storeRegionByKey(String key, TextureRegion region) {
        regionCache.put(key, region);
    }

    /**
     * Retrieve a TextureRegion by synthetic key, or null if not cached yet.
     */
    public TextureRegion cachedRegionByKey(String key) {
        return regionCache.get(key);
    }

    /**
     * Pixel width of the texture at the given path.
     * Used by EntityFactory to set w/h in world-unit == pixel coordinates (PPU=1).
     */
    public int pixelWidth(String path)  { return texture(path).getWidth();  }
    public int pixelHeight(String path) { return texture(path).getHeight(); }

    @Override
    public void dispose() {
        for (Texture t : textureCache.values()) t.dispose();
        textureCache.clear();
        regionCache.clear();
        instance = null;
    }
}
