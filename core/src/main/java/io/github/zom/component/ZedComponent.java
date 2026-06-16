package io.github.zom.component;

import com.artemis.Component;

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
