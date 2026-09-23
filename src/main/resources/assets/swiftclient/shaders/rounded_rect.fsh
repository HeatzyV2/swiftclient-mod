#version 330

// Rectangle arrondi SDF — fragment.
//
// Le calcul reproduit celui du 1.21.11 (rectangle_fragment.fsh) a l'identique :
// meme SDF, meme anti-alias analytique par fwidth, meme dither. Seule la PROVENANCE
// des donnees change (attributs de sommet au lieu d'uniformes) : le resultat par
// pixel, lui, est le meme.
//
// Simplification volontaire vs 1.21.11 : la-bas sdRoundedBox choisissait un rayon
// par coin dans un vec4. Aucun site d'appel n'a jamais utilise de rayons differents
// entre les coins (tous passent r,r,r,r), donc on porte un rayon unique. La branche
// de selection par coin etait du code mort.

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec4 vColor;
in vec2 vLocal;
in vec2 vHalf;
in float vRadius;

out vec4 fragColor;

float sdRoundedBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

float dither(vec2 uv) {
    return (fract(sin(dot(uv.xy, vec2(12.9898, 78.233))) * 43758.5453123) - 0.5) * (1.0 / 255.0);
}

void main() {
    float dist = sdRoundedBox(vLocal, vHalf, vRadius);

    // vLocal est en px framebuffer, donc fwidth mesure la variation par pixel ECRAN
    // exactement comme en 1.21.11 (ou les coordonnees etaient deja en px framebuffer).
    // Travailler en px GUI donnerait un lisse plus large a scale > 1.
    float edge  = fwidth(dist);
    float alpha = 1.0 - smoothstep(-edge, edge, dist);

    if (alpha <= 0.0) discard;

    vec4 color = vColor;
    // Meme dither que la reference : casse le banding sur les fonds translucides.
    // L'argument est la position en px depuis le coin haut-gauche (vLocal + vHalf).
    color.rgb += dither(vLocal + vHalf);

    fragColor = vec4(color.rgb, color.a * alpha) * ColorModulator;
}
