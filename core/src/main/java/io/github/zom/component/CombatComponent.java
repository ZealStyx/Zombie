package io.github.zom.component;

import com.artemis.Component;

/**
 * Weapon stats and pending attack request flags for a combat entity (player).
 *
 * MovementSystem sets meleeRequested / rangedRequested each frame from input.
 * CombatSystem reads them, resolves hits, and clears them.
 *
 * Distances in pixels (PPU=1).
 */
public class CombatComponent extends Component {

    // ── Melee ─────────────────────────────────────────────────────────────────
    public float   meleeDamage    = 20f;
    /** Half-width and range of the melee hit rectangle in front of the player. */
    public float   meleeRange     = 36f;   // px in front of player
    public float   meleeHalfWidth = 12f;   // px to each side of the attack arc
    public float   meleeCooldown  = 0.5f;  // seconds between swings
    public float   meleeTimer     = 0f;

    // ── Ranged ────────────────────────────────────────────────────────────────
    public boolean hasRanged      = false;
    public float   rangedDamage   = 30f;
    public float   rangedRange    = 400f;  // px — hitscan max distance
    public float   rangedCooldown = 0.4f;
    public float   rangedTimer    = 0f;

    // ── Ammunition ────────────────────────────────────────────────────────────
    /** Rounds currently loaded in the clip. */
    public int     currentAmmo    = 0;
    /** Maximum clip size for the equipped gun. */
    public int     clipSize       = 0;
    /** Item ID of the ammo type required by the equipped gun. */
    public int     ammoItemId     = 0;

    // ── Reload state ──────────────────────────────────────────────────────────
    public boolean reloading       = false;
    public float   reloadTimer     = 0f;
    public float   reloadDuration  = 2.0f;  // seconds to reload

    // ── Fire mode ─────────────────────────────────────────────────────────────
    /** "semi", "auto", or "both". */
    public String  fireMode        = "semi";
    /** True while the fire button is held down (for auto-fire). */
    public boolean isAutoFiring    = false;

    // ── Input flags (set by MovementSystem, cleared by CombatSystem) ──────────
    public boolean meleeRequested  = false;
    public boolean rangedRequested = false;
    public boolean reloadRequested = false;

    // ── God mode (set by debug console) ──────────────────────────────────────
    public boolean godMode = false;
}

