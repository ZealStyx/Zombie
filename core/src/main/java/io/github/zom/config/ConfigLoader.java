package io.github.zom.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Reads all JSON configs from assets/config/ on startup.
 * Call ConfigLoader.load() once from Main.create(); use static getters afterwards.
 *
 * File layout after split:
 *   config/player.json           → PlayerConfig
 *   config/item_textures.json    → ItemDatabase       (display data, sprites)
 *   config/item_grid.json        → ItemGridConfig     (inventory footprints only)
 *   config/item_data.json        → ItemDataConfig     (weapon/container/food stats)
 *   config/equipped.json         → EquippedDatabase
 *   config/zed.json              → ZedConfig
 */
public final class ConfigLoader {

    private static PlayerConfig   playerConfig;
    private static ItemDatabase   itemDatabase;
    private static ItemGridConfig itemGridConfig;
    private static ItemDataConfig itemDataConfig;
    private static EquippedDatabase equippedDatabase;
    private static ZedConfig      zedConfig;

    private static boolean loaded = false;

    private ConfigLoader() {}

    public static void load() {
        if (loaded) return;

        Json json = new Json();
        json.setIgnoreUnknownFields(true);

        playerConfig     = loadConfig(json, PlayerConfig.class,
            Gdx.files.internal("config/player.json"));
        itemDatabase     = loadConfig(json, ItemDatabase.class,
            Gdx.files.internal("config/item_textures.json"));
        itemGridConfig   = loadConfig(json, ItemGridConfig.class,
            Gdx.files.internal("config/item_grid.json"));
        itemDataConfig   = loadConfig(json, ItemDataConfig.class,
            Gdx.files.internal("config/item_data.json"));
        equippedDatabase = loadConfig(json, EquippedDatabase.class,
            Gdx.files.internal("config/equipped.json"));
        zedConfig        = loadConfig(json, ZedConfig.class,
            Gdx.files.internal("config/zed.json"));

        itemDatabase.buildIndex();
        itemGridConfig.buildIndex();
        itemDataConfig.buildIndex();
        equippedDatabase.buildIndex();

        loaded = true;
        Gdx.app.log("ConfigLoader", "Loaded: "
            + itemDatabase.size()   + " items, "
            + itemGridConfig.size() + " grid defs, "
            + itemDataConfig.size() + " item data entries.");
    }

    private static <T> T loadConfig(Json json, Class<T> type, FileHandle file) {
        JsonValue root = new JsonReader().parse(file);
        filterComments(root);
        return json.readValue(type, null, root);
    }

    private static void filterComments(JsonValue value) {
        if (value == null) return;
        if (value.isObject()) {
            JsonValue it = value.child;
            JsonValue prev = null;
            while (it != null) {
                if (it.name != null && it.name.startsWith("_")) {
                    if (prev == null) value.child = it.next;
                    else              prev.next   = it.next;
                } else {
                    filterComments(it);
                    prev = it;
                }
                it = it.next;
            }
        } else if (value.isArray()) {
            for (JsonValue it = value.child; it != null; it = it.next)
                filterComments(it);
        }
    }

    public static PlayerConfig    getPlayerConfig()    { assertLoaded(); return playerConfig;    }
    public static ItemDatabase    getItemDatabase()    { assertLoaded(); return itemDatabase;    }
    public static ItemGridConfig  getItemGridConfig()  { assertLoaded(); return itemGridConfig;  }
    public static ItemDataConfig  getItemDataConfig()  { assertLoaded(); return itemDataConfig;  }
    public static EquippedDatabase getEquippedDatabase(){ assertLoaded(); return equippedDatabase; }
    public static ZedConfig       getZedConfig()       { assertLoaded(); return zedConfig;       }

    private static void assertLoaded() {
        if (!loaded)
            throw new IllegalStateException("ConfigLoader.load() has not been called yet.");
    }
}
