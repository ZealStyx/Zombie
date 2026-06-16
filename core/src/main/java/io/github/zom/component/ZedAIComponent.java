package io.github.zom.component;

import com.artemis.Component;

/**
 * Zed AI state machine data. All distances are in world units (pixels at PPU=1).
 *
 * State machine:
 *   IDLE    → used for buried zeds before emergence; no movement
 *   WANDER  → random directional drift; transitions to CHASE on detection
 *   CHASE   → steer toward player at full speed; transitions to ATTACK / back to WANDER
 *   ATTACK  → stop, play attack anim, deal damage on cooldown; transitions to CHASE / WANDER
 */
public class ZedAIComponent extends Component {

    public enum State { IDLE, WANDER, CHASE, ATTACK }

    public State state = State.WANDER;

    // ── Movement ─────────────────────────────────────────────────────────────
    public float speed = 40f;   // px / sec (at PPU=1, player sprites are 30px)

    // ── Detection ────────────────────────────────────────────────────────────
    public float detectionRange = 200f;  // px — aggro radius
    public float deaggroRange   = 350f;  // px — lose-interest radius
    public float attackRange    = 24f;   // px — melee strike distance

    // ── Attack ───────────────────────────────────────────────────────────────
    public float attackDamage   = 15f;
    public float attackCooldown = 1.2f;  // seconds between attacks
    public float attackTimer    = 0f;

    // ── Wander ───────────────────────────────────────────────────────────────
    public float wanderDirX     = 0f;
    public float wanderDirY     = -1f;
    public float wanderTimer    = 0f;
    public float wanderInterval = 3f;    // seconds between direction changes

    // ── Cached player position (written by ZedAISystem) ─────────────────────
    /** Last known player centre X (pixels). */
    public float lastPlayerX = 0f;
    /** Last known player centre Y (pixels). */
    public float lastPlayerY = 0f;
}
