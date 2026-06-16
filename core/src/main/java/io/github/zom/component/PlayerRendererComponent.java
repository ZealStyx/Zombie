package io.github.zom.component;

import com.artemis.Component;

import io.github.zom.rendering.PlayerRenderer;

/**
 * Holds the PlayerRenderer instance attached to this player entity.
 * Created lazily by PlayerRenderSystem on the first frame.
 */
public class PlayerRendererComponent extends Component {
    public PlayerRenderer renderer;
}
