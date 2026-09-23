package dev.swiftclient.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.swiftclient.core.mods.CapeSim;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class WaveMesh {
   private static final float CAPE_WIDTH = 0.625F;
   private static final float CAPE_HEIGHT = 1.0F;
   private static final float CAPE_DEPTH = 0.0625F;
   private static final float FU0 = 0.015625F;
   private static final float FU1 = 0.171875F;
   private static final float BU0 = 0.1875F;
   private static final float BU1 = 0.34375F;
   private static final float V0 = 0.03125F;
   private static final float V1 = 0.53125F;
   private static final float LU0 = 0.0F;
   private static final float LU1 = 0.015625F;
   private static final float RU0 = 0.171875F;
   private static final float RU1 = 0.1875F;
   private static final float DV0 = 0.0F;
   private static final float DV1 = 0.03125F;

   private WaveMesh() {
   }

   public static void emit(Pose basePose, VertexConsumer vc, CapeSim sim, float delta, int light, ModelPart body) {
      int n = sim.size();
      Matrix4f base = new Matrix4f(basePose.pose());
      Matrix4f[] mat = new Matrix4f[n];
      float x0 = sim.lerpX(0, delta);
      float y0 = sim.lerpY(0, delta);

      for (int part = 0; part < n; part++) {
         float x = sim.lerpX(part, delta) - x0;
         if (x > 0.0F) {
            x = 0.0F;
         }

         float y = y0 - part - sim.lerpY(part, delta);
         float partRot = rotation(sim, part, delta, n);
         Matrix4f m = new Matrix4f(base);
         m.translate(0.0F, 0.0F, 0.125F);
         if (body != null) {
            m.translate(body.x / 16.0F, body.y / 16.0F, body.z / 16.0F);
            m.rotateZYX(body.zRot, body.yRot, body.xRot);
         }

         m.rotateX((float)Math.toRadians(6.0));
         m.rotateY((float)Math.toRadians(180.0));
         m.translate(0.0F, y / n, x / n);
         m.translate(0.0F, 0.03F, -0.03F);
         m.translate(0.0F, part * 1.0F / n, 0.0F);
         m.rotateX((float)Math.toRadians(-partRot));
         m.translate(0.0F, -part * 1.0F / n, 0.0F);
         m.translate(0.0F, -0.03F, 0.03F);
         mat[part] = m;
      }

      float hw = 0.3125F;

      for (int part = 0; part < n; part++) {
         Matrix4f top = mat[Math.max(part - 1, 0)];
         Matrix4f bot = mat[part];
         float hT = part * (1.0F / n);
         float hB = (part + 1) * (1.0F / n);
         float vT = 0.03125F + 0.5F * part / n;
         float vB = 0.03125F + 0.5F * (part + 1) / n;
         quad(
            vc, light, top, -hw, hT, 0.0F, 0.015625F, vT, top, hw, hT, 0.0F, 0.171875F, vT, bot, hw, hB, 0.0F, 0.171875F, vB, bot, -hw, hB, 0.0F, 0.015625F, vB
         );
         quad(
            vc,
            light,
            top,
            hw,
            hT,
            -0.0625F,
            0.34375F,
            vT,
            top,
            -hw,
            hT,
            -0.0625F,
            0.1875F,
            vT,
            bot,
            -hw,
            hB,
            -0.0625F,
            0.1875F,
            vB,
            bot,
            hw,
            hB,
            -0.0625F,
            0.34375F,
            vB
         );
         quad(
            vc, light, top, -hw, hT, 0.0F, 0.0F, vT, bot, -hw, hB, 0.0F, 0.0F, vB, bot, -hw, hB, -0.0625F, 0.015625F, vB, top, -hw, hT, -0.0625F, 0.015625F, vT
         );
         quad(
            vc,
            light,
            top,
            hw,
            hT,
            -0.0625F,
            0.171875F,
            vT,
            bot,
            hw,
            hB,
            -0.0625F,
            0.171875F,
            vB,
            bot,
            hw,
            hB,
            0.0F,
            0.1875F,
            vB,
            top,
            hw,
            hT,
            0.0F,
            0.1875F,
            vT
         );
      }

      Matrix4f end = mat[n - 1];
      quad(
         vc,
         light,
         end,
         -hw,
         1.0F,
         0.0F,
         0.015625F,
         0.0F,
         end,
         hw,
         1.0F,
         0.0F,
         0.171875F,
         0.0F,
         end,
         hw,
         1.0F,
         -0.0625F,
         0.171875F,
         0.03125F,
         end,
         -hw,
         1.0F,
         -0.0625F,
         0.015625F,
         0.03125F
      );
   }

   private static float rotation(CapeSim sim, int part, float delta, int n) {
      if (part >= n - 1) {
         return rotation(sim, n - 2, delta, n);
      } else {
         float ax = sim.lerpX(part, delta);
         float ay = sim.lerpY(part, delta);
         float bx = sim.lerpX(part + 1, delta);
         float by = sim.lerpY(part + 1, delta);
         return (float)(Math.toDegrees(Math.atan2(bx - ax, by - ay)) + 180.0);
      }
   }

   private static void quad(
      VertexConsumer vc,
      int light,
      Matrix4f m1,
      float x1,
      float y1,
      float z1,
      float u1,
      float v1,
      Matrix4f m2,
      float x2,
      float y2,
      float z2,
      float u2,
      float v2,
      Matrix4f m3,
      float x3,
      float y3,
      float z3,
      float u3,
      float v3,
      Matrix4f m4,
      float x4,
      float y4,
      float z4,
      float u4,
      float v4
   ) {
      Vector3f p1 = m1.transformPosition(new Vector3f(x1, y1, z1));
      Vector3f p2 = m2.transformPosition(new Vector3f(x2, y2, z2));
      Vector3f p3 = m3.transformPosition(new Vector3f(x3, y3, z3));
      Vector3f p4 = m4.transformPosition(new Vector3f(x4, y4, z4));
      Vector3f e1 = new Vector3f(p2).sub(p1);
      Vector3f e2 = new Vector3f(p4).sub(p1);
      Vector3f nrm = e1.cross(e2);
      if (nrm.lengthSquared() > 1.0E-8F) {
         nrm.normalize();
      }

      v(vc, light, p1, u1, v1, nrm);
      v(vc, light, p2, u2, v2, nrm);
      v(vc, light, p3, u3, v3, nrm);
      v(vc, light, p4, u4, v4, nrm);
   }

   private static void v(VertexConsumer vc, int light, Vector3f p, float u, float vv, Vector3f nrm) {
      vc.addVertex(p.x, p.y, p.z).setColor(-1).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nrm.x, nrm.y, nrm.z);
   }
}
