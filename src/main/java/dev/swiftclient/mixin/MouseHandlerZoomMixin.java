package dev.swiftclient.mixin;

import dev.swiftclient.core.mods.ZoomState;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public class MouseHandlerZoomMixin {
   @Inject(
      method = {"onScroll(JDD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void lightclient$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      if (ZoomState.scroll(vertical)) {
         ci.cancel();
      }
   }
}
