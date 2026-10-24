package dev.swiftclient.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2f;

/**
 * The 2D transform of a GUI draw: a Matrix3x2fStack since 1.21.6, a PoseStack before. Same method names as
 * Matrix3x2fStack, so drawing code reads {@code GuiPose.of(g).translate(x, y)} on every version. Render thread
 * only: {@link #of} reuses one instance.
 */
public final class GuiPose {
   private static final GuiPose INSTANCE = new GuiPose();
   private GuiGraphicsExtractor g;

   private GuiPose() {
   }

   public static GuiPose of(GuiGraphicsExtractor g) {
      INSTANCE.g = g;
      return INSTANCE;
   }

   public GuiPose pushMatrix() {
      //? if >=1.21.6 {
      this.g.pose().pushMatrix();
      //?} else
      /*this.g.pose().pushPose();*/
      return this;
   }

   public GuiPose popMatrix() {
      //? if >=1.21.6 {
      this.g.pose().popMatrix();
      //?} else
      /*this.g.pose().popPose();*/
      return this;
   }

   public GuiPose translate(float x, float y) {
      //? if >=1.21.6 {
      this.g.pose().translate(x, y);
      //?} else
      /*this.g.pose().translate(x, y, 0.0F);*/
      return this;
   }

   public GuiPose scale(float x, float y) {
      //? if >=1.21.6 {
      this.g.pose().scale(x, y);
      //?} else
      /*this.g.pose().scale(x, y, 1.0F);*/
      return this;
   }

   /** Rotation in radians, around the current origin. */
   public GuiPose rotate(float radians) {
      //? if >=1.21.6 {
      this.g.pose().rotate(radians);
      //?} else
      /*this.g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(radians));*/
      return this;
   }

   public GuiPose mul(Matrix3x2f m) {
      //? if >=1.21.6 {
      this.g.pose().mul(m);
      //?} else
      /*this.g.pose().mulPose(new org.joml.Matrix4f(m.m00(), m.m01(), 0.0F, 0.0F, m.m10(), m.m11(), 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, m.m20(), m.m21(), 0.0F, 1.0F));*/
      return this;
   }

   /** Horizontal scale of the current transform. */
   public float m00() {
      //? if >=1.21.6 {
      return this.g.pose().m00();
      //?} else
      /*return this.g.pose().last().pose().m00();*/
   }

   /** Horizontal offset of the current transform. */
   public float m20() {
      //? if >=1.21.6 {
      return this.g.pose().m20();
      //?} else
      /*return this.g.pose().last().pose().m30();*/
   }

   /** Vertical offset of the current transform. */
   public float m21() {
      //? if >=1.21.6 {
      return this.g.pose().m21();
      //?} else
      /*return this.g.pose().last().pose().m31();*/
   }
}
