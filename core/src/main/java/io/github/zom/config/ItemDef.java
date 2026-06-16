package io.github.zom.config;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/** One item row from items.json. */
public class ItemDef {

    public int     id;
    public int     old_id;
    public String  name;
    public String  description;
    /** food | medical | ammo | throwable | attachment | material | tool |
     *  footwear | accessory | melee | secondary | primary |
     *  vest | helmet | pants | top | backpack | deployable | unknown */
    public String  type;
    public boolean equippable;
    public Sprite  sprite;

    public static class Sprite {
        /** "items/item/<id>.png" — inventory icon. */
        public String icon;
        /** "items/on_ground/<id>.png" — world drop sprite. */
        public String on_ground;
        /**
         * pose → direction → [framePath, …]  (or "frames" for directionless)
         * null for non-equippable items or items with no equipped/ folder.
         */
        public ObjectMap<String, ObjectMap<String, Array<String>>> equipped;
    }
}
