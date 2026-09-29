package dev.swiftclient.mixin;

import dev.swiftclient.world.WorldOverlays;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Swift world overlays are emitted with the vanilla debug gizmos, inside the frame's gizmo collection. */
@Mixin({DebugRenderer.class})
public abstract class DebugRendererMixin {
   @Inject(
      method = {"emitGizmos"},
      at = {@At("TAIL")}
   )
   private void swiftclient$worldOverlays(Frustum frustum, double camX, double camY, double camZ, float partialTicks, CallbackInfo ci) {
      WorldOverlays.emit(camX, camY, camZ, partialTicks);
   }
}
