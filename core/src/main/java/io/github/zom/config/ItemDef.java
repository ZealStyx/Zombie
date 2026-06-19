package io.github.zom.config;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * One item row from item_textures.json (formerly items.json).
 * Contains ONLY display/identity data — no weapon stats, no grid size.
 * Weapon/container/food stats → ItemDataDef.
 * Grid footprint → ItemGridDef.
 */
public class ItemDef {

    public int     id;
    public int     old_id;
    public String  name;
    public String  description;
    public String  type;
    public boolean equippable;
    public Sprite  sprite;

    public static class Sprite {
        public String icon;
        public String on_ground;
        public ObjectMap<String, ObjectMap<String, Array<String>>> equipped;
    }
}
