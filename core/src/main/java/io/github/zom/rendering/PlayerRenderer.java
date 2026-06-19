package io.github.zom.rendering;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.github.zom.config.ConfigLoader;
import io.github.zom.config.EquippedDatabase;
import io.github.zom.config.PlayerConfig;

/**
 * Draws a player using four sprite layers (body → hands → clothing → held/holstered).
 *
 * FIX: When the player is NOT running, holsteredAnims is drawn at stateTime=0
 * (frame 0) so the holstered weapon sits statically on the hip/back and does not
 * play the bobbing h_run animation while the player is standing still.
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
            heldAnims      = heldId  > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, heldId)  : null;
            this.heldItemId = heldId;
        }
        if (holstId != this.holsteredItemId) {
            holsteredAnims        = holstId > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, holstId) : null;
            this.holsteredItemId  = holstId;
        }
        if (vestId     != this.vestId)     { vestAnims     = vestId     > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, vestId)     : null; this.vestId     = vestId;     }
        if (helmetId   != this.helmetId)   { helmetAnims   = helmetId   > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, helmetId)   : null; this.helmetId   = helmetId;   }
        if (pantsId    != this.pantsId)    { pantsAnims    = pantsId    > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, pantsId)    : null; this.pantsId    = pantsId;    }
        if (topId      != this.topId)      { topAnims      = topId      > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, topId)      : null; this.topId      = topId;      }
        if (backpackId != this.backpackId) { backpackAnims = backpackId > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, backpackId) : null; this.backpackId = backpackId; }
        if (footwearId != this.footwearId) { footwearAnims = footwearId > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, footwearId) : null; this.footwearId = footwearId; }
    }

    public void draw(SpriteBatch batch, SpriteRenderState state,
                     float x, float y, float w, float h) {
        String pose = state.pose;
        String dir  = state.direction;
        float  t    = state.stateTime;

        // Holstered weapon is static (frame 0) unless the player is running
        // FIX: pass 0f stateTime for holstered when not running
        boolean isRunning = "run".equals(pose);
        float   holstTime = isRunning ? t : 0f;

        drawLayer(batch, skinAnims,     pose,                  dir, t,        x, y, w, h);
        drawLayer(batch, handsAnims,    pose,                  dir, t,        x, y, w, h);
        drawLayer(batch, vestAnims,     pose,                  dir, t,        x, y, w, h);
        drawLayer(batch, helmetAnims,   pose,                  dir, t,        x, y, w, h);
        drawLayer(batch, pantsAnims,    pose,                  dir, t,        x, y, w, h);
        drawLayer(batch, topAnims,      pose,                  dir, t,        x, y, w, h);
        drawLayer(batch, backpackAnims, pose,                  dir, t,        x, y, w, h);
        drawLayer(batch, footwearAnims, pose,                  dir, t,        x, y, w, h);
        drawLayer(batch, heldAnims,     pose,                  dir, t,        x, y, w, h);
        drawLayer(batch, holsteredAnims, toHolsteredPose(pose), dir, holstTime, x, y, w, h);
    }

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
