package io.github.zom.component;

import com.artemis.Component;
import com.badlogic.gdx.utils.Array;

/**
 * HP, death state, invulnerability frames, and pending damage queue.
 *
 * Design:
 *  - Damage sources (CombatSystem, ZedAISystem) write into pendingDamage each frame.
 *  - HealthSystem drains the queue once per frame, applies i-frame logic, and sets dead.
 *  - Both players and zeds use this component.
 */
public class HealthComponent extends Component {

    public float   hp              = 100f;
    public float   maxHp           = 100f;
    public boolean dead            = false;

    /** Invulnerability timer — positive while immune to damage. */
    public float iFrameTimer    = 0f;
    /** How long invulnerability lasts after taking a hit (seconds). */
    public float iFrameDuration = 0.3f;

    /** Pending damage entries. Each float[2]: [damage, headshot(1=yes/0=no)]. */
    public final Array<float[]> pendingDamage = new Array<>(false, 4);

    /** Queue a damage event. Thread-safe to call from any system in the same frame. */
    public void queueDamage(float amount, boolean headshot) {
        pendingDamage.add(new float[]{ amount, headshot ? 1f : 0f });
    }

    public boolean isInvulnerable() {
        return iFrameTimer > 0f;
    }

    /** Restore HP, clear dead flag and i-frames (debug console / heal command). */
    public void heal(float amount) {
        hp = Math.min(maxHp, hp + amount);
        dead = false;
        iFrameTimer = 0f;
        pendingDamage.clear();
    }

    /** Full heal shortcut. */
    public void healFull() {
        heal(maxHp);
    }
}
