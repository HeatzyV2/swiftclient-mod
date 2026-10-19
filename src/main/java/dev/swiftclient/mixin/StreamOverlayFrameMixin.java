package dev.swiftclient.mixin;

import dev.swiftclient.streamonly.StreamOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide Inventory "Stream only": once the GUI is drawn, read the slots back, draw the fakes, update the overlay. */
@Mixin({GameRenderer.class})
public abstract class StreamOverlayFrameMixin {
   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/render/GuiRenderer;endFrame()V",
         shift = At.Shift.AFTER
      )}
   )
   private void swiftclient$streamOverlay(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
      StreamOverlay.endFrame();
   }
}
