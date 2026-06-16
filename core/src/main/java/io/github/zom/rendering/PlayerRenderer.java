package io.github.zom.rendering;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.github.zom.config.ConfigLoader;
import io.github.zom.config.EquippedDatabase;
import io.github.zom.config.PlayerConfig;

/**
 * Draws a player character using four sprite layers (bottom → top):
 *   1. body_skin        — character body
 *   2. hands            — hands overlay
 *   3. equipped_clothing— vest / helmet / pants / top / backpack / footwear
 *   4. equipped_held    — held weapon or melee, plus holstered secondary
 *
 * Create one instance per player entity.
 * Call rebuild() whenever any equipment slot changes (PlayerComponent.dirty flag).
 */
public class PlayerRenderer {

    private AnimationSet skinAnims;
    private AnimationSet handsAnims;
    private AnimationSet vestAnims;
    private AnimationSet helmetAnims;
    private AnimationSet pantsAnims;
    private AnimationSet topAnims;
    private AnimationSet backpackAnims;
    private AnimationSet footwearAnims;
    private AnimationSet heldAnims;
    private AnimationSet holsteredAnims;

    private String activeSkinName  = "";
    private String activeHandsName = "";
    private int    heldItemId, holsteredItemId;
    private int    vestId, helmetId, pantsId, topId, backpackId, footwearId;

    private final PlayerConfig     playerCfg;
    private final EquippedDatabase equippedDb;

    public PlayerRenderer() {
        this.playerCfg  = ConfigLoader.getPlayerConfig();
        this.equippedDb = ConfigLoader.getEquippedDatabase();
        rebuild(playerCfg.default_skin, playerCfg.default_hands,
            0, 0, 0, 0, 0, 0, 0, 0);
    }

    // ── Rebuild ───────────────────────────────────────────────────────────────

    public void rebuild(String skinName, String handsName,
                        int heldId,  int holstId,
                        int vestId,  int helmetId, int pantsId,
                        int topId,   int backpackId, int footwearId) {

        if (!skinName.equals(activeSkinName)) {
            skinAnims = AnimationSetBuilder.forPlayerSkin(playerCfg, skinName);
            activeSkinName = skinName;
        }
        if (!handsName.equals(activeHandsName)) {
            handsAnims = AnimationSetBuilder.forPlayerHands(playerCfg, handsName);
            activeHandsName = handsName;
        }
        if (heldId != this.heldItemId) {
            heldAnims = heldId > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, heldId) : null;
            this.heldItemId = heldId;
        }
        if (holstId != this.holsteredItemId) {
            holsteredAnims = holstId > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, holstId) : null;
            this.holsteredItemId = holstId;
        }
        if (vestId     != this.vestId)     { vestAnims     = vestId     > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, vestId)     : null; this.vestId     = vestId;     }
        if (helmetId   != this.helmetId)   { helmetAnims   = helmetId   > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, helmetId)   : null; this.helmetId   = helmetId;   }
        if (pantsId    != this.pantsId)    { pantsAnims    = pantsId    > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, pantsId)    : null; this.pantsId    = pantsId;    }
        if (topId      != this.topId)      { topAnims      = topId      > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, topId)      : null; this.topId      = topId;      }
        if (backpackId != this.backpackId) { backpackAnims = backpackId > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, backpackId) : null; this.backpackId = backpackId; }
        if (footwearId != this.footwearId) { footwearAnims = footwearId > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, footwearId) : null; this.footwearId = footwearId; }
    }

    // ── Draw ─────────────────────────────────────────────────────────────────

    public void draw(SpriteBatch batch, SpriteRenderState state,
                     float x, float y, float w, float h) {

        String pose = state.pose;
        String dir  = state.direction;
        float  t    = state.stateTime;

        // Layer 1 — body
        drawLayer(batch, skinAnims,      pose,                    dir, t, x, y, w, h);
        // Layer 2 — hands
        drawLayer(batch, handsAnims,     pose,                    dir, t, x, y, w, h);
        // Layer 3 — clothing
        drawLayer(batch, vestAnims,      pose,                    dir, t, x, y, w, h);
        drawLayer(batch, helmetAnims,    pose,                    dir, t, x, y, w, h);
        drawLayer(batch, pantsAnims,     pose,                    dir, t, x, y, w, h);
        drawLayer(batch, topAnims,       pose,                    dir, t, x, y, w, h);
        drawLayer(batch, backpackAnims,  pose,                    dir, t, x, y, w, h);
        drawLayer(batch, footwearAnims,  pose,                    dir, t, x, y, w, h);
        // Layer 4 — held item uses same pose; holstered uses h_ variant
        drawLayer(batch, heldAnims,      pose,                    dir, t, x, y, w, h);
        drawLayer(batch, holsteredAnims, toHolsteredPose(pose),   dir, t, x, y, w, h);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void drawLayer(SpriteBatch batch, AnimationSet anims,
                           String pose, String direction, float stateTime,
                           float x, float y, float w, float h) {
        if (anims == null) return;
        Animation<TextureRegion> anim = anims.get(pose, direction);
        if (anim == null) anim = anims.get(pose, "none");
        if (anim == null) anim = anims.get("idle", direction);
        if (anim == null) return;
        TextureRegion frame = anim.getKeyFrame(stateTime);
        if (frame != null) batch.draw(frame, x, y, w, h);
    }

    private static String toHolsteredPose(String activePose) {
        switch (activePose) {
            case "run":     return "h_run";
            case "axe":     return "h_axe";
            case "pistol":  return "h_pistol";
            case "twohand": return "h_twohand";
            default:        return "h_run";
        }
    }
}
