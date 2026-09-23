package dev.swiftclient.renderer.rect;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public record RoundedRectRenderState(
   Matrix3x2fc pose, float x0, float y0, float x1, float y1, float radius, float scale, int color, ScreenRectangle scissorArea, ScreenRectangle bounds
) implements GuiElementRenderState {
   public static RoundedRectRenderState of(
      Matrix3x2fc pose, float x, float y, float w, float h, float radius, float scale, int color, ScreenRectangle scissorArea
   ) {
      float r = Math.max(0.0F, Math.min(radius, Math.min(w, h) / 2.0F));
      Matrix3x2f snapshot = new Matrix3x2f(pose);
      ScreenRectangle b = new ScreenRectangle((int)Math.floor(x), (int)Math.floor(y), (int)Math.ceil(w) + 1, (int)Math.ceil(h) + 1)
         .transformMaxBounds(new Matrix3x2f(snapshot));
      return new RoundedRectRenderState(snapshot, x, y, x + w, y + h, r, scale, color, scissorArea, b);
   }

   public void buildVertices(VertexConsumer vc) {
      float hw = (this.x1 - this.x0) * 0.5F * this.scale;
      float hh = (this.y1 - this.y0) * 0.5F * this.scale;
      int fixHw = fixed(hw);
      int fixHh = fixed(hh);
      int fixR = fixed(this.radius * this.scale);
      this.vertex(vc, this.x0, this.y0, -hw, -hh, fixHw, fixHh, fixR);
      this.vertex(vc, this.x0, this.y1, -hw, hh, fixHw, fixHh, fixR);
      this.vertex(vc, this.x1, this.y1, hw, hh, fixHw, fixHh, fixR);
      this.vertex(vc, this.x1, this.y0, hw, -hh, fixHw, fixHh, fixR);
   }

   private void vertex(VertexConsumer vc, float px, float py, float lx, float ly, int fixHw, int fixHh, int fixR) {
      vc.addVertexWith2DPose(this.pose, px, py).setColor(this.color).setUv(lx, ly).setUv1(fixHw, fixHh).setUv2(fixR, 0);
   }

   private static int fixed(float v) {
      return Math.min(Math.round(v * 16.0F), 32767);
   }

   public RenderPipeline pipeline() {
      return RectPipeline.get();
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }
}
