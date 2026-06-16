package io.github.zom.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Reads all JSON configs from assets/config/ on startup and holds the results.
 * Call ConfigLoader.load() once during game init; access via static getters afterwards.
 *
 * Configs loaded:
 *   config/player.json   → PlayerConfig
 *   config/items.json    → ItemDatabase
 *   config/equipped.json → EquippedDatabase
 *   config/zed.json      → ZedConfig
 */
public final class ConfigLoader {

    private static PlayerConfig     playerConfig;
    private static ItemDatabase     itemDatabase;
    private static EquippedDatabase equippedDatabase;
    private static ZedConfig        zedConfig;

    private static boolean loaded = false;

    private ConfigLoader() {}

    /** Call once, e.g. from Main.create(). Subsequent calls are no-ops. */
    public static void load() {
        if (loaded) return;

        Json json = new Json();
        json.setIgnoreUnknownFields(true);

        playerConfig     = loadConfig(json, PlayerConfig.class,     Gdx.files.internal("config/player.json"));
        itemDatabase     = loadConfig(json, ItemDatabase.class,     Gdx.files.internal("config/items.json"));
        equippedDatabase = loadConfig(json, EquippedDatabase.class, Gdx.files.internal("config/equipped.json"));
        zedConfig        = loadConfig(json, ZedConfig.class,        Gdx.files.internal("config/zed.json"));

        itemDatabase.buildIndex();
        equippedDatabase.buildIndex();

        loaded = true;
        Gdx.app.log("ConfigLoader", "All configs loaded successfully.");
    }

    private static <T> T loadConfig(Json json, Class<T> type, FileHandle file) {
        JsonValue root = new JsonReader().parse(file);
        filterComments(root);
        return json.readValue(type, null, root);
    }

    /** Recursively removes all fields starting with '_' (e.g. _comment) from the JsonValue tree. */
    private static void filterComments(JsonValue value) {
        if (value == null) return;
        if (value.isObject()) {
            JsonValue it = value.child;
            JsonValue prev = null;
            while (it != null) {
                if (it.name != null && it.name.startsWith("_")) {
                    if (prev == null) value.child = it.next;
                    else prev.next = it.next;
                } else {
                    filterComments(it);
                    prev = it;
                }
                it = it.next;
            }
        } else if (value.isArray()) {
            for (JsonValue it = value.child; it != null; it = it.next) {
                filterComments(it);
            }
        }
    }

    public static PlayerConfig     getPlayerConfig()     { assertLoaded(); return playerConfig;     }
    public static ItemDatabase     getItemDatabase()     { assertLoaded(); return itemDatabase;     }
    public static EquippedDatabase getEquippedDatabase() { assertLoaded(); return equippedDatabase; }
    public static ZedConfig        getZedConfig()        { assertLoaded(); return zedConfig;        }

    private static void assertLoaded() {
        if (!loaded) throw new IllegalStateException("ConfigLoader.load() has not been called yet.");
    }
}
