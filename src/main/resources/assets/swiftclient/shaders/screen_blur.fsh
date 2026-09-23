#version 150

// Blur plein écran (façon PolyBlur) : floute la scène capturée (Sampler0) par un blur
// gaussien à pas adaptatif, puis assombrit selon dim.

in vec2 uv;

uniform sampler2D Sampler0;

layout(std140) uniform Uniforms {
    vec4 params;   // x = force du blur (px), y = largeur texture, z = hauteur, w = inutilisé
    vec4 dim;      // voile d'assombrissement (rgba, a = intensité)
};

out vec4 fragColor;

void main() {
    float blur = params.x;
    vec2 texSize = vec2(params.y, params.z);

    vec3 acc;
    if (blur > 0.05) {
        float r = min(blur, 24.0);
        float sigma = max(1.0, r / 2.0);
        float s2 = 2.0 * sigma * sigma;
        float stepAmount = max(1.0, r / 8.0);
        vec3 sum = vec3(0.0);
        float wsum = 0.0;
        for (float x = -r; x <= r; x += stepAmount) {
            for (float y = -r; y <= r; y += stepAmount) {
                float wgt = exp(-(x * x + y * y) / s2);
                sum += texture(Sampler0, uv + vec2(x, y) / texSize).rgb * wgt;
                wsum += wgt;
            }
        }
        acc = sum / wsum;
    } else {
        acc = texture(Sampler0, uv).rgb;
    }

    vec3 col = mix(acc, dim.rgb, dim.a);
    fragColor = vec4(col, 1.0);
}
