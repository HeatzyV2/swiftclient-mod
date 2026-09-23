#version 330

// Rectangle arrondi SDF — sommet.
//
// Difference avec le 1.21.11 : la-bas, rect/rayon/couleurs arrivaient par UNIFORMES
// et le quad etait genere depuis gl_VertexID. Impossible ici : en 26.x le GuiRenderer
// FUSIONNE tous les elements partageant un meme pipeline dans un seul buffer et un
// seul draw (voir GuiRenderer$Draw : il ne retient que pipeline/texture/scissor, aucun
// uniforme). Un uniforme par rectangle n'aurait donc aucun sens.
//
// Les donnees par-rectangle voyagent donc dans les ATTRIBUTS de sommet. Le
// VertexConsumer n'expose que des emplacements semantiques fixes, on reutilise donc
// leurs noms canoniques en y encodant autre chose :
//   UV0 = position locale en px FRAMEBUFFER, relative au centre du rectangle
//   UV1 = demi-taille (x,y) en px framebuffer, ×16 (virgule fixe)
//   UV2 = rayon des coins en px framebuffer, ×16 (y inutilise)
// La demi-taille et le rayon sont constants sur les 4 sommets : leur interpolation
// est donc l'identite.

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;

out vec4 vColor;
out vec2 vLocal;
out vec2 vHalf;
out float vRadius;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vColor  = Color;
    vLocal  = UV0;
    vHalf   = vec2(UV1) / 16.0;
    vRadius = float(UV2.x) / 16.0;
}
