package io.github.zom.component;

import com.artemis.Component;

/**
 * Marks an entity as a zed enemy and holds its visual identity.
 *
 * zedType     — "normal"|"fast"|"army"|"buried"|"jumper"|"screamer"|"shooter"|"tank"
 * skinName    — alive skin folder, e.g. "zed_normal_skin3"
 * deadSkinName— dead  skin folder, e.g. "zed_normal_skin3_dead"
 * dieVariant  — "die1" or "die2" — chosen randomly at death, constrained to what exists
 * alive       — false once the death animation starts
 * dirty       — true when skinName changes; ZedRenderSystem rebuilds the renderer
 */
public class ZedComponent extends Component {

    public String  zedType      = "normal";
    public String  skinName     = "zed_normal_skin1";
    public String  deadSkinName = "zed_normal_skin1_dead";
    public String  dieVariant   = "die1";
    public boolean alive        = true;
    public boolean dirty        = true;

    public void die(String variant) {
        this.alive      = false;
        this.dieVariant = variant;
    }
}
