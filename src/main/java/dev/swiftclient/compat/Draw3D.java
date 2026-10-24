package dev.swiftclient.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.11 {
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
*///?}

/**
 * Lines, boxes and labels drawn in the world (chunk borders, hitboxes, waypoints...), in world coordinates.
 * Since 1.21.11 they are vanilla gizmos; before, they are queued here and drawn after the debug renderers
 * (DebugRendererMixin calls {@code flush}). Called from WorldOverlays.emit, once per frame.
 */
public final class Draw3D {
   private Draw3D() {
   }

   public static void line(Vec3 from, Vec3 to, int argb, float width) {
      //? if >=1.21.11 {
      Gizmos.line(from, to, argb, width);
      //?} else
      /*LINES.add(new Line(from, to, argb));*/
   }

   /** Outline of a box. */
   public static void box(AABB box, int argb, float width) {
      //? if >=1.21.11 {
      Gizmos.cuboid(box, GizmoStyle.stroke(argb, width));
      //?} else
      /*BOXES.add(new Box(box, argb, 0));*/
   }

   /** Solid box. */
   public static void fill(AABB box, int argb) {
      //? if >=1.21.11 {
      Gizmos.cuboid(box, GizmoStyle.fill(argb));
      //?} else
      /*BOXES.add(new Box(box, 0, argb));*/
   }

   public static void boxAndFill(AABB box, int stroke, float width, int fill) {
      //? if >=1.21.11 {
      Gizmos.cuboid(box, GizmoStyle.strokeAndFill(stroke, width, fill));
      //?} else
      /*BOXES.add(new Box(box, stroke, fill));*/
   }

   /** Outline of a block, grown by {@code inflate} on every side. */
   public static void block(BlockPos pos, float inflate, int argb, float width) {
      box(new AABB(pos).inflate(inflate), argb, width);
   }

   /** A label facing the camera, centred on {@code pos} and drawn over everything. */
   public static void text(String text, Vec3 pos, int argb, float scale) {
      //? if >=1.21.11 {
      Gizmos.billboardText(text, pos, TextGizmo.Style.forColorAndCentered(argb).withScale(scale)).setAlwaysOnTop();
      //?} else
      /*TEXTS.add(new Text(text, pos, argb, scale));*/
   }

   //? if <1.21.11 {
   /*private record Line(Vec3 from, Vec3 to, int argb) {
   }

   private record Box(AABB box, int stroke, int fill) {
   }

   private record Text(String text, Vec3 pos, int argb, float scale) {
   }

   private static final List<Line> LINES = new ArrayList<>();
   private static final List<Box> BOXES = new ArrayList<>();
   private static final List<Text> TEXTS = new ArrayList<>();

   /^* Draws what was queued this frame (camera-relative pose, like the debug renderers). ^/
   public static void flush(PoseStack pose, MultiBufferSource.BufferSource buffers, double camX, double camY, double camZ) {
      try {
         for (Box b : BOXES) {
            AABB box = b.box().move(-camX, -camY, -camZ);
            if (b.fill() != 0) {
               int c = b.fill();
               DebugRenderer.renderFilledBox(pose, buffers, box, (c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F, (c >>> 24) / 255.0F);
            }
         }

         VertexConsumer lines = buffers.getBuffer(RenderType.lines());
         PoseStack.Pose p = pose.last();
         for (Line l : LINES) {
            segment(lines, p, l.from().subtract(camX, camY, camZ), l.to().subtract(camX, camY, camZ), l.argb());
         }

         for (Box b : BOXES) {
            if (b.stroke() != 0) {
               AABB x = b.box().move(-camX, -camY, -camZ);
               Vec3[] c = {
                  new Vec3(x.minX, x.minY, x.minZ), new Vec3(x.maxX, x.minY, x.minZ), new Vec3(x.maxX, x.minY, x.maxZ), new Vec3(x.minX, x.minY, x.maxZ),
                  new Vec3(x.minX, x.maxY, x.minZ), new Vec3(x.maxX, x.maxY, x.minZ), new Vec3(x.maxX, x.maxY, x.maxZ), new Vec3(x.minX, x.maxY, x.maxZ)
               };
               for (int i = 0; i < 4; i++) {
                  segment(lines, p, c[i], c[(i + 1) % 4], b.stroke());
                  segment(lines, p, c[i + 4], c[(i + 1) % 4 + 4], b.stroke());
                  segment(lines, p, c[i], c[i + 4], b.stroke());
               }
            }
         }

         for (Text t : TEXTS) {
            DebugRenderer.renderFloatingText(pose, buffers, t.text(), t.pos().x, t.pos().y, t.pos().z, t.argb(), 0.025F * t.scale(), true, 0.0F, true);
         }
      } finally {
         LINES.clear();
         BOXES.clear();
         TEXTS.clear();
      }
   }

   private static void segment(VertexConsumer out, PoseStack.Pose pose, Vec3 a, Vec3 b, int argb) {
      Vec3 n = b.subtract(a).normalize();
      out.addVertex(pose, (float)a.x, (float)a.y, (float)a.z).setColor(argb).setNormal(pose, (float)n.x, (float)n.y, (float)n.z);
      out.addVertex(pose, (float)b.x, (float)b.y, (float)b.z).setColor(argb).setNormal(pose, (float)n.x, (float)n.y, (float)n.z);
   }
   *///?}
}
