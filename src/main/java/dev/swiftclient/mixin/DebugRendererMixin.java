package dev.swiftclient.mixin;

import dev.swiftclient.world.WorldOverlays;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.11 {
import net.minecraft.client.renderer.culling.Frustum;
//?} else {
/*import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.swiftclient.compat.Draw3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

/**
 * Swift world overlays are emitted with the vanilla debug gizmos, inside the frame's gizmo collection
 * (1.21.11+); before gizmos existed they are drawn right after the debug renderers.
 */
@Mixin({DebugRenderer.class})
public abstract class DebugRendererMixin {
   //? if >=1.21.11 {
   @Inject(
      method = {"emitGizmos"},
      at = {@At("TAIL")}
   )
   private void swiftclient$worldOverlays(Frustum frustum, double camX, double camY, double camZ, float partialTicks, CallbackInfo ci) {
      WorldOverlays.emit(camX, camY, camZ, partialTicks);
   }
   //?} else {
   /*@Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void swiftclient$worldOverlays(
      CallbackInfo ci,
      @Local(argsOnly = true) PoseStack pose,
      @Local(argsOnly = true) MultiBufferSource.BufferSource buffers,
      @Local(argsOnly = true, ordinal = 0) double camX,
      @Local(argsOnly = true, ordinal = 1) double camY,
      @Local(argsOnly = true, ordinal = 2) double camZ
   ) {
      WorldOverlays.emit(camX, camY, camZ, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true));
      Draw3D.flush(pose, buffers, camX, camY, camZ);
   }
   *///?}
}
