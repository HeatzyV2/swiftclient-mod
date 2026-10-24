package dev.swiftclient.visuals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Textured knife skins read from Blockbench item models (assets/swiftclient/knife_models/counter, from the
 * CounterCraft knife pack). They are drawn like vanilla draws an item model: the model's own first-person
 * display transform, then its cubes with their textures and UVs. Animated textures (a vertical strip of square
 * frames) play at their .mcmeta frame time.
 */
final class ModelKnife {
   /** Skin label (as listed in CS Knife) and model file. */
   private static final Map<String, String> MODELS = new LinkedHashMap<>();
   private static final Map<String, ModelKnife> LOADED = new HashMap<>();
   private static final String[] FACES = {"north", "south", "west", "east", "down", "up"};

   static {
      MODELS.put("Stiletto", "stilletto");
      MODELS.put("Dark Stiletto", "stillettodark");
      MODELS.put("M9 Bayonet", "m9bayonet");
      MODELS.put("M9 Bayonet Gradient", "m9bayonetgr");
      MODELS.put("M9 Bayonet Violet", "m9bayonetvi");
      MODELS.put("Bayonet", "bayonet");
      MODELS.put("Bayonet Gradient", "bayonetgr");
      MODELS.put("Bayonet Violet", "bayonetvi");
      MODELS.put("Ursus", "ursusst");
      MODELS.put("Ursus Gem", "ursus");
      MODELS.put("Ursus Violet", "ursusdark");
      MODELS.put("Guonith", "pashalko");
      MODELS.put("Guonith Au", "pashalko1");
   }

   private final List<Layer> layers;
   private final Transform right;
   private final Transform left;

   private ModelKnife(List<Layer> layers, Transform right, Transform left) {
      this.layers = layers;
      this.right = right;
      this.left = left;
   }

   /** The skin labels this class draws, in menu order. */
   static String[] labels() {
      return MODELS.keySet().toArray(new String[0]);
   }

   /** The model knife for a skin label, or null when the label is not one of them. */
   static ModelKnife of(String label) {
      String file = MODELS.get(label);
      return file == null ? null : LOADED.computeIfAbsent(file, ModelKnife::load);
   }

   void draw(PoseStack pose, SubmitNodeCollector collector, int light, boolean leftHand, boolean foil) {
      Transform t = leftHand ? this.left : this.right;
      pose.pushPose();
      // Same as vanilla's ItemTransform with the left-hand fix (mirrored x translation, y and z turns)
      float mirror = leftHand ? -1.0F : 1.0F;
      pose.translate(mirror * t.translation.x / 16.0F, t.translation.y / 16.0F, t.translation.z / 16.0F);
      pose.mulPose((new Quaternionf()).rotationXYZ(
         (float)Math.toRadians(t.rotation.x), (float)Math.toRadians(mirror * t.rotation.y), (float)Math.toRadians(mirror * t.rotation.z)));
      pose.scale(t.scale.x, t.scale.y, t.scale.z);
      pose.translate(-0.5F, -0.5F, -0.5F);
      long tick = System.currentTimeMillis() / 50L;
      for (Layer layer : this.layers) {
         int frame = layer.frames <= 1 ? 0 : (int)(tick / layer.frameTime % layer.frames);
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(layer.texture), (p, out) -> layer.emit(p, out, light, frame));
         if (foil) {
            collector.order(1).submitCustomGeometry(pose, RenderTypes.entityGlint(), (p, out) -> layer.emit(p, out, light, frame));
         }
      }

      pose.popPose();
   }

   // ---- loading ----

   private static ModelKnife load(String file) {
      JsonObject model = json("/assets/swiftclient/knife_models/counter/" + file + ".json");
      Map<String, Layer> layers = new LinkedHashMap<>();
      JsonObject textures = model.getAsJsonObject("textures");
      for (JsonElement el : model.getAsJsonArray("elements")) {
         JsonObject e = el.getAsJsonObject();
         float[] lo = floats(e.getAsJsonArray("from"));
         float[] hi = floats(e.getAsJsonArray("to"));
         Quaternionf turn = new Quaternionf();
         Vector3f origin = new Vector3f();
         if (e.has("rotation")) {
            JsonObject r = e.getAsJsonObject("rotation");
            if (r.has("axis")) {
               float angle = (float)Math.toRadians(r.get("angle").getAsFloat());
               switch (r.get("axis").getAsString()) {
                  case "x" -> turn.rotationX(angle);
                  case "y" -> turn.rotationY(angle);
                  default -> turn.rotationZ(angle);
               }
            } else {
               turn.rotationXYZ(
                  (float)Math.toRadians(number(r, "x")), (float)Math.toRadians(number(r, "y")), (float)Math.toRadians(number(r, "z")));
            }

            float[] o = floats(r.getAsJsonArray("origin"));
            origin.set(o[0], o[1], o[2]);
         }

         JsonObject faces = e.getAsJsonObject("faces");
         for (int f = 0; f < FACES.length; f++) {
            if (!faces.has(FACES[f])) {
               continue;
            }

            JsonObject face = faces.getAsJsonObject(FACES[f]);
            String ref = face.get("texture").getAsString().replace("#", "");
            if (!textures.has(ref)) {
               continue;
            }

            String texture = textures.get(ref).getAsString();
            Layer layer = layers.computeIfAbsent(texture, Layer::new);
            float[] uv = face.has("uv") ? floats4(face.getAsJsonArray("uv")) : defaultUv(f, lo, hi);
            int uvTurn = face.has("rotation") ? face.get("rotation").getAsInt() / 90 & 3 : 0;
            layer.add(f, lo, hi, uv, uvTurn, turn, origin);
         }
      }

      JsonObject display = model.getAsJsonObject("display");
      Transform right = Transform.of(display == null ? null : display.getAsJsonObject("firstperson_righthand"));
      Transform left = display != null && display.has("firstperson_lefthand") ? Transform.of(display.getAsJsonObject("firstperson_lefthand")) : right;
      return new ModelKnife(new ArrayList<>(layers.values()), right, left);
   }

   private static JsonObject json(String path) {
      try (InputStream in = ModelKnife.class.getResourceAsStream(path)) {
         if (in == null) {
            throw new IOException("Missing " + path);
         }

         return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
      } catch (IOException | RuntimeException e) {
         throw new IllegalStateException("Knife model " + path, e);
      }
   }

   private static float number(JsonObject o, String key) {
      return o.has(key) ? o.get(key).getAsFloat() : 0.0F;
   }

   private static float[] floats(JsonArray a) {
      return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
   }

   private static float[] floats4(JsonArray a) {
      return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat(), a.get(3).getAsFloat()};
   }

   /** Vanilla's UV for a face without one: its own outline on the 16×16 texture. */
   private static float[] defaultUv(int face, float[] lo, float[] hi) {
      return switch (face) {
         case 0 -> new float[]{16.0F - hi[0], 16.0F - hi[1], 16.0F - lo[0], 16.0F - lo[1]};
         case 1 -> new float[]{lo[0], 16.0F - hi[1], hi[0], 16.0F - lo[1]};
         case 2 -> new float[]{lo[2], 16.0F - hi[1], hi[2], 16.0F - lo[1]};
         case 3 -> new float[]{16.0F - hi[2], 16.0F - hi[1], 16.0F - lo[2], 16.0F - lo[1]};
         case 4 -> new float[]{lo[0], 16.0F - hi[2], hi[0], 16.0F - lo[2]};
         default -> new float[]{lo[0], lo[2], hi[0], hi[2]};
      };
   }

   /** First-person display transform of the model: degrees, 1/16 block, scale. */
   private record Transform(Vector3f rotation, Vector3f translation, Vector3f scale) {
      static Transform of(JsonObject o) {
         Vector3f rotation = new Vector3f();
         Vector3f translation = new Vector3f();
         Vector3f scale = new Vector3f(1.0F);
         if (o != null) {
            if (o.has("rotation")) {
               float[] r = floats(o.getAsJsonArray("rotation"));
               rotation.set(r[0], r[1], r[2]);
            }

            if (o.has("translation")) {
               float[] t = floats(o.getAsJsonArray("translation"));
               translation.set(Math.max(-80.0F, Math.min(80.0F, t[0])), Math.max(-80.0F, Math.min(80.0F, t[1])), Math.max(-80.0F, Math.min(80.0F, t[2])));
            }

            if (o.has("scale")) {
               float[] s = floats(o.getAsJsonArray("scale"));
               scale.set(Math.min(4.0F, s[0]), Math.min(4.0F, s[1]), Math.min(4.0F, s[2]));
            }
         }

         return new Transform(rotation, translation, scale);
      }
   }

   /** The faces using one texture: positions in block units (0..1), UVs within one frame, normals. */
   private static final class Layer {
      private final Identifier texture;
      private final int frames;
      private final int frameTime;
      private final List<float[]> quads = new ArrayList<>();

      Layer(String ref) {
         // "swiftclient:knife/counter/name" -> textures/knife/counter/name.png
         int colon = ref.indexOf(':');
         String path = "textures/" + ref.substring(colon + 1) + ".png";
         this.texture = Identifier.fromNamespaceAndPath(colon > 0 ? ref.substring(0, colon) : "minecraft", path);
         int[] size = pngSize("/assets/" + this.texture.getNamespace() + "/" + path);
         this.frames = size[0] > 0 && size[1] > size[0] ? size[1] / size[0] : 1;
         this.frameTime = this.frames > 1 ? frameTime("/assets/" + this.texture.getNamespace() + "/" + path + ".mcmeta") : 1;
      }

      /** One face: 4 corners of 8 floats (x y z, u v, normal x y z packed as a 12 + 8 + 3 array). */
      void add(int face, float[] lo, float[] hi, float[] uv, int uvTurn, Quaternionf turn, Vector3f origin) {
         float[] corners = corners(face, lo, hi);
         float[] q = new float[23];
         Vector3f p = new Vector3f();
         for (int c = 0; c < 4; c++) {
            float x = corners[c * 3];
            float y = corners[c * 3 + 1];
            float z = corners[c * 3 + 2];
            float[] st = faceSt(face, x, y, z, lo, hi);
            float s = st[0];
            float t = st[1];
            for (int r = 0; r < uvTurn; r++) {
               // A texture turned clockwise: the face shows the texture turned back the other way
               float ns = t;
               t = 1.0F - s;
               s = ns;
            }

            p.set(x, y, z).sub(origin);
            turn.transform(p).add(origin);
            q[c * 3] = p.x / 16.0F;
            q[c * 3 + 1] = p.y / 16.0F;
            q[c * 3 + 2] = p.z / 16.0F;
            q[12 + c * 2] = (uv[0] + (uv[2] - uv[0]) * s) / 16.0F;
            q[12 + c * 2 + 1] = (uv[1] + (uv[3] - uv[1]) * t) / 16.0F;
         }

         Vector3f n = turn.transform(normal(face));
         q[20] = n.x;
         q[21] = n.y;
         q[22] = n.z;
         this.quads.add(q);
      }

      void emit(PoseStack.Pose pose, VertexConsumer out, int light, int frame) {
         float frameV = 1.0F / this.frames;
         for (float[] q : this.quads) {
            for (int c = 0; c < 4; c++) {
               float v = (q[12 + c * 2 + 1] + frame) * frameV;
               out.addVertex(pose, q[c * 3], q[c * 3 + 1], q[c * 3 + 2])
                  .setColor(-1)
                  .setUv(q[12 + c * 2], v)
                  .setOverlay(OverlayTexture.NO_OVERLAY)
                  .setLight(light)
                  .setNormal(pose, q[20], q[21], q[22]);
            }
         }
      }

      /** Where a corner sits on its face, 0..1 along the face's texture axes (u to the right, v down). */
      private static float[] faceSt(int face, float x, float y, float z, float[] lo, float[] hi) {
         float sx = span(x - lo[0], hi[0] - lo[0]);
         float sy = span(hi[1] - y, hi[1] - lo[1]);
         float sz = span(z - lo[2], hi[2] - lo[2]);
         return switch (face) {
            case 0 -> new float[]{1.0F - sx, sy};
            case 1 -> new float[]{sx, sy};
            case 2 -> new float[]{sz, sy};
            case 3 -> new float[]{1.0F - sz, sy};
            case 4 -> new float[]{sx, 1.0F - sz};
            default -> new float[]{sx, sz};
         };
      }

      private static float span(float d, float size) {
         return size == 0.0F ? 0.0F : d / size;
      }

      private static float[] corners(int face, float[] lo, float[] hi) {
         float x0 = lo[0], y0 = lo[1], z0 = lo[2], x1 = hi[0], y1 = hi[1], z1 = hi[2];
         return switch (face) {
            case 0 -> new float[]{x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0};
            case 1 -> new float[]{x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1};
            case 2 -> new float[]{x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0};
            case 3 -> new float[]{x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1};
            case 4 -> new float[]{x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0};
            default -> new float[]{x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1};
         };
      }

      private static Vector3f normal(int face) {
         return switch (face) {
            case 0 -> new Vector3f(0.0F, 0.0F, -1.0F);
            case 1 -> new Vector3f(0.0F, 0.0F, 1.0F);
            case 2 -> new Vector3f(-1.0F, 0.0F, 0.0F);
            case 3 -> new Vector3f(1.0F, 0.0F, 0.0F);
            case 4 -> new Vector3f(0.0F, -1.0F, 0.0F);
            default -> new Vector3f(0.0F, 1.0F, 0.0F);
         };
      }

      /** Width and height from the PNG header (0, 0 when unreadable). */
      private static int[] pngSize(String path) {
         try (InputStream in = ModelKnife.class.getResourceAsStream(path)) {
            byte[] head = in == null ? new byte[0] : in.readNBytes(24);
            if (head.length < 24) {
               return new int[]{0, 0};
            }

            return new int[]{
               (head[16] & 255) << 24 | (head[17] & 255) << 16 | (head[18] & 255) << 8 | head[19] & 255,
               (head[20] & 255) << 24 | (head[21] & 255) << 16 | (head[22] & 255) << 8 | head[23] & 255
            };
         } catch (IOException e) {
            return new int[]{0, 0};
         }
      }

      private static int frameTime(String mcmeta) {
         try (InputStream in = ModelKnife.class.getResourceAsStream(mcmeta)) {
            if (in == null) {
               return 1;
            }

            JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject anim = o.getAsJsonObject("animation");
            return anim != null && anim.has("frametime") ? Math.max(1, anim.get("frametime").getAsInt()) : 1;
         } catch (IOException | RuntimeException e) {
            return 1;
         }
      }
   }
}
