package io.github.zom.component;

import com.artemis.Component;

import io.github.zom.rendering.ZedRenderer;

/**
 * Holds the ZedRenderer instance for this zed entity.
 * Created lazily by ZedRenderSystem on the first frame.
 */
public class ZedRendererComponent extends Component {
    public ZedRenderer renderer;
    public float       deadStateTime = 0f;  // separate timer so death anim runs at its own pace
}
