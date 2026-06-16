package io.github.zom.component;

import com.artemis.Component;

/**
 * Tracks the active animation pose and accumulated time for one entity.
 *
 * Updated every tick by AnimationStateSystem.
 * Read by PlayerRenderSystem / ZedRenderSystem to pick the correct frame.
 *
 * pose       — e.g. "idle", "run", "walk", "attack", "axe", "pistol",
 *              "twohand", "using", "scream", "stun", "throw", "dodge",
 *              "fly", "dead", "die1", "die2", "hidden1", "hidden2", "jump1", "jump2"
 * stateTime  — seconds accumulated in the current pose (resets on pose change)
 * locked     — when true a one-shot animation is playing; movement logic must not
 *              interrupt it until AnimationStateSystem clears the flag
 */
public class AnimationStateComponent extends Component {

    public String  pose        = "idle";
    public float   stateTime   = 0f;
    public boolean locked      = false;
    public float   minDuration = 0f;

    /** Switch to a new pose and reset the timer. No-op if pose unchanged. */
    public void setPose(String newPose) {
        if (!this.pose.equals(newPose)) {
            this.pose      = newPose;
            this.stateTime = 0f;
            this.locked    = false;
        }
    }

    /**
     * Switch to a one-shot pose (attack, scream, using …).
     * Sets locked=true so movement won't interrupt the animation.
     */
    public void playOnce(String newPose) {
        this.pose      = newPose;
        this.stateTime = 0f;
        this.locked    = true;
    }

    /** Advance time. Called by AnimationStateSystem each frame. */
    public void tick(float delta) {
        stateTime += delta;
    }
}
