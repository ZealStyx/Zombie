#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

// The colour tile sheet (1.png / 2.png / 3.png / 4.png)
uniform sampler2D u_texture;
// The matching mask sheet (*_m.png) — grass=white (1.0), dirt=black (0.0)
uniform sampler2D u_mask;

uniform vec4 u_grassColor;
uniform vec4 u_dirtColor;

void main() {
    vec4  texColor = texture2D(u_texture, v_texCoords);
    float maskVal  = texture2D(u_mask,    v_texCoords).r;

    // Lerp between dirt and grass tint using the mask value
    vec4 biome = mix(u_dirtColor, u_grassColor, maskVal);

    // Multiply grayscale tile art by biome tint; v_color carries SpriteBatch vertex colour
    gl_FragColor = v_color * texColor * biome;
}
