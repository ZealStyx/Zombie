package io.github.zom.component;

import com.artemis.Component;

/**
 * Tracks the active animation pose and accumulated time for one entity.
 *
 * FIX 1.5: Added playOnce(String pose, float duration) overload so callers
 * can specify an exact unlock duration instead of relying on the system default.
 */
public class AnimationStateComponent extends Component {

    public String  pose        = "idle";
    public float   stateTime   = 0f;
    public boolean locked      = false;
    /** Minimum seconds to stay locked; 0 means use AnimationStateSystem default. */
    public float   minDuration = 0f;

    /** Switch to a new pose and reset the timer. No-op if pose unchanged. */
    public void setPose(String newPose) {
        if (!this.pose.equals(newPose)) {
            this.pose        = newPose;
            this.stateTime   = 0f;
            this.locked      = false;
            this.minDuration = 0f;
        }
    }

    /**
     * Start a one-shot pose. Uses AnimationStateSystem's default duration to
     * determine when the lock is released.
     */
    public void playOnce(String newPose) {
        this.pose        = newPose;
        this.stateTime   = 0f;
        this.locked      = true;
        this.minDuration = 0f;
    }

    /**
     * FIX 1.5 — Start a one-shot pose with an explicit unlock duration.
     * The lock is released once stateTime >= duration.
     *
     * @param newPose  the animation pose name to play
     * @param duration how long (in seconds) to hold the lock before releasing
     */
    public void playOnce(String newPose, float duration) {
        this.pose        = newPose;
        this.stateTime   = 0f;
        this.locked      = true;
        this.minDuration = duration;
    }

    /** Advance time. Called by AnimationStateSystem each frame. */
    public void tick(float delta) {
        stateTime += delta;
    }
}
