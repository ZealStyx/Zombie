package io.github.zom.rendering;

/**
 * Snapshot of the animation state needed to pick and advance the correct frame.
 * Passed from ECS components into renderers each frame — reused, never allocated per-frame.
 */
public class SpriteRenderState {

    public String  pose      = "idle";
    public String  direction = "down";
    public float   stateTime = 0f;
    public boolean locked    = false;

    public void set(String pose, String direction) {
        if (!this.pose.equals(pose) || !this.direction.equals(direction)) {
            this.pose      = pose;
            this.direction = direction;
            this.stateTime = 0f;
            this.locked    = false;
        }
    }

    public void tick(float delta) {
        stateTime += delta;
    }
}
