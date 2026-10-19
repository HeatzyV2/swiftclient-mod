package dev.swiftclient.streamonly;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws one GUI item icon into any colour/depth target, the way the vanilla GUI item atlas does: used to
 * render the real items for the read-back (overlay) and the fake items onto the streamed frame.
 */
final class ItemIcons {
   private static final SubmitNodeStorage STORAGE = new SubmitNodeStorage();
   private static final PoseStack POSE = new PoseStack();
   private static final Projection PROJECTION = new Projection();
   private static ProjectionMatrixBuffer projectionBuffer;

   private ItemIcons() {
   }

   /** The resolved model of {@code stack} as the GUI would draw it, or null for nothing to draw. */
   static TrackingItemStackRenderState resolve(ItemStack stack) {
      Minecraft mc = Minecraft.getInstance();
      if (stack == null || stack.isEmpty() || mc.player == null) {
         return null;
      }

      TrackingItemStackRenderState state = new TrackingItemStackRenderState();
      mc.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, mc.level, mc.player, 0);
      return state.isEmpty() ? null : state;
   }

   /** Draws {@code state} in the {@code px} square at (left, top) of a {@code width} x {@code height} target. */
   static void draw(TrackingItemStackRenderState state, GpuTextureView color, GpuTextureView depth, int width, int height, int left, int top, int px) {
      Minecraft mc = Minecraft.getInstance();
      if (projectionBuffer == null) {
         projectionBuffer = new ProjectionMatrixBuffer("swiftclient stream overlay items");
      }

      POSE.pushPose();
      POSE.translate(left + px / 2.0F, top + px / 2.0F, 0.0F);
      POSE.scale(px, -px, px);
      RenderSystem.outputColorTextureOverride = color;
      RenderSystem.outputDepthTextureOverride = depth;
      PROJECTION.setupOrtho(-1000.0F, 1000.0F, width, height, true);
      RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(PROJECTION), ProjectionType.ORTHOGRAPHIC);
      // Scissor uses the bottom-left origin of the GPU target.
      RenderSystem.enableScissorForRenderTypeDraws(left, height - top - px, px, px);
      mc.gameRenderer.lighting().setupFor(state.usesBlockLight() ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);
      try {
         state.submit(POSE, STORAGE, 15728880, OverlayTexture.NO_OVERLAY, 0);
         mc.gameRenderer.featureRenderDispatcher().renderAllFeatures(STORAGE);
      } finally {
         RenderSystem.disableScissorForRenderTypeDraws();
         RenderSystem.outputColorTextureOverride = null;
         RenderSystem.outputDepthTextureOverride = null;
         POSE.popPose();
      }
   }
}
