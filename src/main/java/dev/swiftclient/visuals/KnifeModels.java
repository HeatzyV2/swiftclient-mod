package dev.swiftclient.visuals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3f;


final class KnifeModels {
   static final int KARAMBIT_DETAIL = 2;
   static final float KARAMBIT_RING_X = -16.0F;
   static final float KARAMBIT_RING_Y = -1.0F;
   static final float KARAMBIT_BLADE_LENGTH = 21.0F;
   static final float[] KARAMBIT_TIP = karambitTip();
   private static final float KATANA_BOW = 0.0016F;
   static final float KATANA_BOW_FROM = 3.0F;
   static final float KATANA_BOW_RADIUS = 312.5F;

   private KnifeModels() {
   }

   private static float grain(int x, int y, int z) {
      int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
      h ^= h >>> 13;
      h *= 1540483477;
      h ^= h >>> 15;
      return 0.92F + 0.08F * ((float)(h & 1023) / 1023.0F);
   }

   static KnifeMesh butterflyBlade() {
      KnifeMesh.Voxels v = new KnifeMesh.Voxels();

      for(int x = -3; x <= 21; ++x) {
         int top = x <= 12 ? 2 : Math.round(2.0F - (float)(x - 12) * 3.0F / 9.0F);
         int bottom = x <= 14 ? -2 : Math.round(-2.0F + (float)Math.pow((double)((float)(x - 14) / 7.0F), (double)2.0F) * 1.0F);

         for(int y = bottom; y <= top; ++y) {
            int ink;
            if (x < 0) {
               ink = 5;
            } else if (x <= 1) {
               ink = 1;
            } else if (y == bottom) {
               ink = 2;
            } else if (y == top) {
               ink = x >= 13 ? 2 : 1;
            } else {
               ink = 0;
            }

            v.set(x, y, 0, ink, grain(x, y, 0));
         }
      }

      return v.mesh(0.0F, 22.0F);
   }

   static KnifeMesh butterflyHandle(int side) {
      KnifeMesh.Voxels v = new KnifeMesh.Voxels();
      int inner = side > 0 ? 1 : -1;
      int outer = side > 0 ? 2 : -2;
      int cap = side > 0 ? 3 : -3;

      for(int x = -27; x <= 0; ++x) {
         for(int y = -2; y <= 2; ++y) {
            boolean corner = (x == 0 || x == -27) && Math.abs(y) == 2;
            boolean hole = Math.abs(y) <= 1 && (x <= -6 && x >= -8 || x <= -12 && x >= -14 || x <= -18 && x >= -20);
            if (!corner && !hole) {
               v.set(x, y, inner, 4, grain(x, y, inner));
               int ink = 3;
               if ((x == -10 || x == -16) && Math.abs(y) <= 1) {
                  ink = 6;
               }

               if (y == 0 && (x == -2 || x == -25)) {
                  ink = 5;
               }

               v.set(x, y, outer, ink, grain(x, y, outer));
            }
         }
      }

      v.set(-2, 0, cap, 5, 1.0F);
      v.set(-25, 0, cap, 5, 1.0F);
      if (side < 0) {
         v.set(-28, 1, inner, 5, 1.0F);
         v.set(-28, 2, inner, 5, 1.0F);
         v.set(-29, 2, inner, 5, 1.0F);
      }

      return v.mesh(-29.0F, 0.0F);
   }

   private static float[] karambitCurve(float s) {
      float x = 5.5F;
      float y = 0.5F;
      float step = 0.05F;

      for(float t = 0.0F; t < s; t += step) {
         float heading = -(0.04F * t + 0.0026F * t * t);
         x += (float)Math.cos((double)heading) * step;
         y += (float)Math.sin((double)heading) * step;
      }

      float heading = -(0.04F * s + 0.0026F * s * s);
      return new float[]{x, y, (float)Math.cos((double)heading), (float)Math.sin((double)heading)};
   }

   private static float[] karambitTip() {
      float[] c = karambitCurve(21.0F);
      return new float[]{c[0] * 2.0F, c[1] * 2.0F};
   }

   static KnifeMesh karambit() {
      KnifeMesh.Voxels v = new KnifeMesh.Voxels();
      int k = 2;
      float ringX = -16.0F / (float)k;
      float ringY = -1.0F / (float)k;

      for(int x = Math.round((ringX + 2.4F) * (float)k); x < 6 * k; ++x) {
         float px = ((float)x + 0.5F) / (float)k;
         float middle = 0.5F - (float)Math.pow((double)((5.0F - px) / 11.0F), (double)2.0F);

         for(int y = -6 * k; y <= 4 * k; ++y) {
            float across = ((float)y + 0.5F) / (float)k - middle;
            if (!(Math.abs(across) > 2.45F)) {
               v.set(x, y, 0, 5, grain(x, y, 0));
               boolean rivet = Math.hypot((double)(px + 3.0F), (double)across) < (double)0.7F || Math.hypot((double)(px - 2.5F), (double)across) < (double)0.7F;
               boolean inlay = Math.abs(across) < 0.5F && px > -1.5F && px < 1.5F;
               boolean rim = Math.abs(across) > 1.95F;

               for(int z : new int[]{-1, 1}) {
                  int ink = rivet ? 5 : (inlay ? 6 : (rim ? 4 : 3));
                  v.set(x, y, z, ink, grain(x, y, z));
               }

               if (rivet) {
                  v.set(x, y, -2, 5, 1.0F);
                  v.set(x, y, 2, 5, 1.0F);
               }
            }
         }
      }

      for(int x = Math.round((ringX - 3.4F) * (float)k); x <= Math.round((ringX + 3.4F) * (float)k); ++x) {
         for(int y = Math.round((ringY - 3.4F) * (float)k); y <= Math.round((ringY + 3.4F) * (float)k); ++y) {
            float dx = ((float)x + 0.5F) / (float)k - ringX;
            float dy = ((float)y + 0.5F) / (float)k - ringY;
            float d = (float)Math.sqrt((double)(dx * dx + dy * dy));
            if (!(d < 2.0F) && !(d > 3.0F) && !v.filled(x, y, 0)) {
               float shade = !(d > 2.75F) && !(d < 2.25F) ? 1.0F : 0.86F;

               for(int z = -1; z <= 1; ++z) {
                  v.set(x, y, z, 5, shade * grain(x, y, z));
               }
            }
         }
      }

      int samples = 420;
      float[][] curve = new float[samples + 1][];

      for(int i = 0; i <= samples; ++i) {
         curve[i] = karambitCurve(21.0F * (float)i / (float)samples);
      }

      for(int x = 5 * k; x <= 31 * k; ++x) {
         for(int y = -24 * k; y <= 6 * k; ++y) {
            float px = ((float)x + 0.5F) / (float)k;
            float py = ((float)y + 0.5F) / (float)k;
            int best = 0;
            float bestDistance = Float.MAX_VALUE;

            for(int i = 0; i <= samples; ++i) {
               float dx = px - curve[i][0];
               float dy = py - curve[i][1];
               float d = dx * dx + dy * dy;
               if (d < bestDistance) {
                  bestDistance = d;
                  best = i;
               }
            }

            float[] c = curve[best];
            float dx = px - c[0];
            float dy = py - c[1];
            float along = dx * c[2] + dy * c[3];
            float across = -dx * c[3] + dy * c[2];
            if ((best != 0 || !(along < -0.5F)) && (best != samples || !(along > 0.3F))) {
               float left = 1.0F - (float)best / (float)samples;
               float spine = 2.9F * (float)Math.pow((double)left, 0.6) + 0.15F;
               float edge = 2.9F * (float)Math.pow((double)left, 1.1) + 0.1F;
               if (!(across > spine) && !(across < -edge) && !v.filled(x, y, 0)) {
                  int ink = across < -edge + 0.55F ? 2 : (across > spine - 0.55F ? 1 : 0);
                  v.set(x, y, 0, ink, grain(x, y, 0));
               }
            }
         }
      }

      return v.mesh(5.0F * (float)k, 27.0F * (float)k);
   }

   /** Kunai model (Blockbench cubes, 1/16 block units): scale to knife units and the landmarks used below. */
   private static final String KUNAI_MODEL = "/assets/swiftclient/knife_models/minato_kunai.json";
   private static final float KUNAI_SCALE = 2.7F;
   /** Model point that becomes the knife origin: where the blade meets the handle, on the handle's axis. */
   private static final float KUNAI_ORIGIN_X = 7.915F;
   private static final float KUNAI_ORIGIN_Y = 0.67F;
   private static final float KUNAI_ORIGIN_Z = 8.79F;
   /** Centre of the ring (model z 13.36), where the finger goes through: the inspect spins the knife around it. */
   static final float KUNAI_RING_X = (8.79F - 13.36F) * 2.7F;
   static final float KUNAI_RING_Y = 0.0F;
   /** Middle of the wrapped handle (model z 10.67), where the fist holds it. */
   static final float KUNAI_GRIP_X = (8.79F - 10.67F) * 2.7F;
   /** Blade tip (model z 1.07). */
   static final float KUNAI_TIP_X = (8.79F - 1.07F) * 2.7F;
   private static final String[] FACES = {"north", "south", "west", "east", "down", "up"};

   /**
    * Minato's three-pronged kunai, read from its Blockbench model: every cube face becomes a quad, rotated like in
    * the model, then turned so the blade runs along +x with the tip forward. Light grey is the blade, dark grey the
    * blade's darker parts and the beige wrap the handle; the sword's material gives the actual colours.
    */
   static KnifeMesh kunai() {
      JsonObject model;
      try (InputStream in = KnifeModels.class.getResourceAsStream(KUNAI_MODEL)) {
         model = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
      } catch (IOException | RuntimeException e) {
         throw new IllegalStateException("Kunai model", e);
      }

      List<float[]> quads = new ArrayList<>();
      List<float[]> normals = new ArrayList<>();
      List<Byte> inks = new ArrayList<>();
      for (JsonElement el : model.getAsJsonArray("elements")) {
         JsonObject e = el.getAsJsonObject();
         float[] from = floats(e.getAsJsonArray("from"));
         float[] to = floats(e.getAsJsonArray("to"));
         Quaternionf turn = new Quaternionf();
         Vector3f origin = new Vector3f();
         if (e.has("rotation")) {
            JsonObject r = e.getAsJsonObject("rotation");
            float angle = (float)Math.toRadians(r.get("angle").getAsFloat());
            switch (r.get("axis").getAsString()) {
               case "x" -> turn.rotationX(angle);
               case "y" -> turn.rotationY(angle);
               default -> turn.rotationZ(angle);
            }

            float[] o = floats(r.getAsJsonArray("origin"));
            origin.set(o[0], o[1], o[2]);
         }

         JsonObject faces = e.getAsJsonObject("faces");
         for (int f = 0; f < FACES.length; f++) {
            if (!faces.has(FACES[f])) {
               continue;
            }

            String texture = faces.getAsJsonObject(FACES[f]).get("texture").getAsString();
            int ink = texture.equals("#0") ? KnifeMaterial.WRAP : texture.equals("#1") ? KnifeMaterial.BLADE_DARK : KnifeMaterial.BLADE;
            float[] corners = faceCorners(f, from, to);
            float[] q = new float[12];
            Vector3f p = new Vector3f();
            for (int c = 0; c < 4; c++) {
               p.set(corners[c * 3], corners[c * 3 + 1], corners[c * 3 + 2]).sub(origin);
               turn.transform(p).add(origin);
               q[c * 3] = (KUNAI_ORIGIN_Z - p.z) * KUNAI_SCALE;
               q[c * 3 + 1] = (KUNAI_ORIGIN_X - p.x) * KUNAI_SCALE;
               q[c * 3 + 2] = (p.y - KUNAI_ORIGIN_Y) * KUNAI_SCALE;
            }

            Vector3f n = turn.transform(faceNormal(f));
            quads.add(q);
            normals.add(new float[]{-n.z, -n.x, n.y});
            inks.add((byte)ink);
         }
      }

      int count = quads.size();
      float[] pos = new float[count * 12];
      float[] normal = new float[count * 3];
      byte[] material = new byte[count];
      float[] tone = new float[count];
      for (int i = 0; i < count; i++) {
         System.arraycopy(quads.get(i), 0, pos, i * 12, 12);
         System.arraycopy(normals.get(i), 0, normal, i * 3, 3);
         material[i] = inks.get(i);
         tone[i] = 1.0F;
      }

      return KnifeMesh.quads(pos, normal, material, tone, 0.0F, KUNAI_TIP_X);
   }

   private static float[] floats(JsonArray a) {
      return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
   }

   /** The four corners of a cube face, in order around it (north, south, west, east, down, up). */
   private static float[] faceCorners(int face, float[] lo, float[] hi) {
      float x0 = lo[0], y0 = lo[1], z0 = lo[2], x1 = hi[0], y1 = hi[1], z1 = hi[2];
      return switch (face) {
         case 0 -> new float[]{x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0};
         case 1 -> new float[]{x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1};
         case 2 -> new float[]{x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0};
         case 3 -> new float[]{x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1};
         case 4 -> new float[]{x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1};
         default -> new float[]{x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0};
      };
   }

   private static Vector3f faceNormal(int face) {
      return switch (face) {
         case 0 -> new Vector3f(0.0F, 0.0F, -1.0F);
         case 1 -> new Vector3f(0.0F, 0.0F, 1.0F);
         case 2 -> new Vector3f(-1.0F, 0.0F, 0.0F);
         case 3 -> new Vector3f(1.0F, 0.0F, 0.0F);
         case 4 -> new Vector3f(0.0F, -1.0F, 0.0F);
         default -> new Vector3f(0.0F, 1.0F, 0.0F);
      };
   }

   static KnifeMesh katanaScabbard() {
      KnifeMesh.Voxels v = new KnifeMesh.Voxels();

      for(int x = 1; x <= 51; ++x) {
         int bow = katanaBow(Math.min(x, 48));
         boolean mouth = x <= 2;
         int low = mouth ? bow - 4 : bow - 3;
         int high = mouth ? bow + 3 : bow + 2;
         int thick = mouth ? 2 : 1;

         for(int y = low; y <= high; ++y) {
            for(int z = -thick; z <= thick; ++z) {
               boolean outside = y == low || y == high || Math.abs(z) == thick;
               int ink;
               if (!mouth && x < 50) {
                  if (x >= 12 && x <= 14 && outside) {
                     ink = (x + y + z) % 2 == 0 ? 7 : 6;
                  } else {
                     ink = 4;
                  }
               } else {
                  ink = 8;
               }

               v.set(x, y, z, ink, ink == 4 ? 0.9F + 0.1F * grain(x, y, z) : grain(x, y, z));
            }
         }
      }

      int knob = katanaBow(16);
      v.set(16, knob + 3, 0, 8, 1.0F);
      v.set(17, knob + 3, 0, 8, 1.0F);
      return v.mesh(1.0F, 51.0F);
   }

   static float katanaCentre(float x) {
      return 0.0016F * (x - 3.0F) * (x - 3.0F);
   }

   static int katanaBow(int x) {
      return Math.round(0.0016F * ((float)x - 3.0F) * ((float)x - 3.0F));
   }

   static KnifeMesh katana() {
      KnifeMesh.Voxels v = new KnifeMesh.Voxels();

      for(int x = -17; x <= -1; ++x) {
         for(int y = -1; y <= 1; ++y) {
            for(int z = -1; z <= 1; ++z) {
               int ink;
               if (x == -1) {
                  ink = 8;
               } else if (x == -17) {
                  ink = y == 0 && z == 0 ? 8 : 5;
               } else {
                  boolean face = Math.abs(y) == 1 ^ Math.abs(z) == 1;
                  boolean window = face && ((x + 20) % 4 == 1 || (x + 20) % 4 == 2);
                  ink = window ? 2 : 7;
               }

               v.set(x, y, z, ink, ink == 2 ? 0.82F : grain(x, y, z));
            }
         }
      }

      for(int y = -4; y <= 4; ++y) {
         for(int z = -4; z <= 4; ++z) {
            float d = (float)Math.sqrt((double)(y * y + z * z));
            if (!(d > 3.9F)) {
               v.set(0, y, z, d >= 3.0F ? 8 : 5, grain(0, y, z));
            }
         }
      }

      for(int y = -1; y <= 1; ++y) {
         for(int z = -1; z <= 1; ++z) {
            v.set(1, y, z, 8, grain(1, y, z));
         }

         v.set(2, y, 0, 8, grain(2, y, 0));
      }

      for(int x = 3; x <= 48; ++x) {
         int bow = katanaBow(x);
         int lowest = x >= 48 ? bow + 1 : (x >= 46 ? bow : (x >= 44 ? bow - 1 : bow - 2));
         int highest = x >= 48 ? bow + 1 : bow + 1;

         for(int y = lowest; y <= highest; ++y) {
            int ink;
            if (y == lowest) {
               ink = 2;
            } else if (y == highest) {
               ink = 1;
            } else if (y == lowest + 1) {
               ink = x > 5 && Math.sin((double)x * 0.85) + Math.sin((double)x * 0.37) * 0.6 > 0.35 ? 2 : 0;
            } else {
               ink = 0;
            }

            v.set(x, y, 0, ink, grain(x, y, 0));
         }
      }

      return v.mesh(3.0F, 49.0F);
   }
}
