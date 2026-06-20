package io.github.zom.rendering;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.github.zom.config.ConfigLoader;
import io.github.zom.config.EquippedDatabase;
import io.github.zom.config.PlayerConfig;

/**
 * Draws a player using layered sprites.
 *
 * FIXES:
 *
 * [Held animation cycles while idle]
 *   drawHeldLayer() uses stateTime=0 when falling back to "idle" direction
 *   animation, so a weapon without its own stance-specific pose is frozen at
 *   frame 0 rather than cycling the idle loop endlessly.
 *   When the animation lock is active (e.g. during a fire/attack one-shot),
 *   the full stateTime is still used so the attack animation plays correctly.
 *
 * [Holstered weapon appears in held/active state when idle]
 *   toHolsteredPose() now maps all non-running poses to "h_idle" instead of
 *   weapon-specific held poses ("h_pistol", "h_twohand" etc.). This prevents
 *   the holstered weapon from adopting an active-hold appearance.
 *   drawHolsteredLayer() no longer falls back to "idle" — it only tries h_*
 *   poses, so the weapon can never accidentally render using a held animation.
 *   When the "h_idle" pose doesn't exist for a weapon, it gracefully falls
 *   back to frame 0 of "h_run" (weapon sitting passively at hip/back).
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
            skinAnims      = AnimationSetBuilder.forPlayerSkin(playerCfg, skinName);
            activeSkinName = skinName;
        }
        if (!handsName.equals(activeHandsName)) {
            handsAnims      = AnimationSetBuilder.forPlayerHands(playerCfg, handsName);
            activeHandsName = handsName;
        }
        if (heldId != this.heldItemId) {
            heldAnims       = heldId  > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, heldId)  : null;
            this.heldItemId = heldId;
        }
        if (holstId != this.holsteredItemId) {
            holsteredAnims       = holstId > 0 ? AnimationSetBuilder.forEquippedItem(equippedDb, holstId) : null;
            this.holsteredItemId = holstId;
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
        String  pose   = state.pose;
        String  dir    = state.direction;
        float   t      = state.stateTime;
        boolean locked = state.locked;

        // Holstered: static except during run.
        boolean isRunning = "run".equals(pose);
        float   holstTime = isRunning ? t : 0f;
        String  holstPose = toHolsteredPose(pose);

        // Body + clothing layers — standard fallback allowed
        drawLayer(batch, skinAnims,     pose, dir, t,        x, y, w, h);
        drawLayer(batch, handsAnims,    pose, dir, t,        x, y, w, h);
        drawLayer(batch, vestAnims,     pose, dir, t,        x, y, w, h);
        drawLayer(batch, helmetAnims,   pose, dir, t,        x, y, w, h);
        drawLayer(batch, pantsAnims,    pose, dir, t,        x, y, w, h);
        drawLayer(batch, topAnims,      pose, dir, t,        x, y, w, h);
        drawLayer(batch, backpackAnims, pose, dir, t,        x, y, w, h);
        drawLayer(batch, footwearAnims, pose, dir, t,        x, y, w, h);

        // Weapon layers — restricted fallback to prevent active-hold animation bleed
        drawHeldLayer(batch, heldAnims, pose, dir, t, locked, x, y, w, h);
        drawHolsteredLayer(batch, holsteredAnims, holstPose, dir, holstTime, x, y, w, h);
    }

    // ── Standard body/clothing layer ──────────────────────────────────────────

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

    // ── Held weapon layer ─────────────────────────────────────────────────────

    /**
     * FIX: Falls back to "idle" at stateTime=0 (not the live timer) so the
     * idle fallback pose is always frozen at frame 0 — no unwanted cycling.
     * During a locked (one-shot attack) animation, stateTime advances normally.
     */
    private void drawHeldLayer(SpriteBatch batch, AnimationSet anims,
                               String pose, String direction, float stateTime,
                               boolean locked, float x, float y, float w, float h) {
        if (anims == null) return;
        Animation<TextureRegion> anim = anims.get(pose, direction);
        if (anim == null) anim = anims.get(pose, "none");
        if (anim == null) {
            // Fallback to idle, but freeze at frame 0 unless actively locked
            anim = anims.get("idle", direction);
            if (anim == null) anim = anims.get("idle", "none");
            if (!locked) stateTime = 0f; // freeze when not mid-attack
        }
        if (anim == null) return;
        TextureRegion frame = anim.getKeyFrame(stateTime);
        if (frame != null) batch.draw(frame, x, y, w, h);
    }

    // ── Holstered weapon layer ────────────────────────────────────────────────

    /**
     * FIX: Only uses h_* poses — never falls back to "idle", which would make
     * the holstered weapon appear as if it's being actively held.
     * If the requested holster pose doesn't exist, falls back to "h_run" at
     * frame 0 (weapon sitting passively at hip/back).
     */
    private void drawHolsteredLayer(SpriteBatch batch, AnimationSet anims,
                                    String holstPose, String direction, float stateTime,
                                    float x, float y, float w, float h) {
        if (anims == null) return;
        Animation<TextureRegion> anim = anims.get(holstPose, direction);
        if (anim == null) anim = anims.get(holstPose, "none");
        if (anim == null) {
            // Fallback: h_run at frame 0 (static holster)
            anim = anims.get("h_run", direction);
            if (anim == null) anim = anims.get("h_run", "none");
            stateTime = 0f;
        }
        if (anim == null) return;
        TextureRegion frame = anim.getKeyFrame(stateTime);
        if (frame != null) batch.draw(frame, x, y, w, h);
    }

    // ── Holstered pose mapping ────────────────────────────────────────────────

    /**
     * FIX: All non-running stances map to "h_idle" so the holstered weapon is
     * always shown in a passive, non-held pose. Only running maps to "h_run"
     * to enable the weapon-bob animation while the player is moving.
     */
    private static String toHolsteredPose(String activePose) {
        return "run".equals(activePose) ? "h_run" : "h_idle";
    }
}
