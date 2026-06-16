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

    // ── Input flags (set by MovementSystem, cleared by CombatSystem) ──────────
    public boolean meleeRequested  = false;
    public boolean rangedRequested = false;

    // ── God mode (set by debug console) ──────────────────────────────────────
    public boolean godMode = false;
}
