package dev.swiftclient.mixin;

import dev.swiftclient.input.ModuleKeys;
import net.minecraft.client.KeyboardHandler;
//? if >=1.21.9
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
   //? if >=1.21.9 {
   private void swiftclient$moduleKeys(long handle, int action, KeyEvent event, CallbackInfo ci) {
      int key = event.key();
   //?} else {
   /*private void swiftclient$moduleKeys(long handle, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
   *///?}
      if (ModuleKeys.dispatch(key, action)) {
         ci.cancel();
      }
   }
}
