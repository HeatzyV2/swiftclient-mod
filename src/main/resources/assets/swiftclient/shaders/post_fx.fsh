#version 150

// Post-effets Swift : params.x = mode (0 = étalonnage, 1 = fondu de l'image précédente).
//  mode 0 : params.y = saturation, params.z = contraste, params.w = luminosité (tous 1.0 = neutre)
//  mode 1 : params.y = opacité de l'image précédente (flou de mouvement)

in vec2 uv;

uniform sampler2D Sampler0;

layout(std140) uniform Uniforms {
    vec4 params;
    vec4 extra;
};

out vec4 fragColor;

void main() {
    vec3 c = texture(Sampler0, uv).rgb;
    if (params.x < 0.5) {
        float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
        c = mix(vec3(luma), c, params.y);
        c = (c - 0.5) * params.z + 0.5;
        c = c * params.w;
        fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
    } else {
        fragColor = vec4(c, params.y);
    }
}
