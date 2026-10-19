package dev.swiftclient.mixin;

import dev.swiftclient.input.ModuleKeys;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keys of the modules themselves (CS Knife inspect, Coord Snapper, Elytra Swap): may replace the vanilla action. */
@Mixin({KeyboardHandler.class})
public abstract class KeyboardHandlerMixin {
   @Inject(
      method = {"keyPress"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$moduleKeys(long handle, int action, KeyEvent event, CallbackInfo ci) {
      if (ModuleKeys.dispatch(event.key(), action)) {
         ci.cancel();
      }
   }
}
