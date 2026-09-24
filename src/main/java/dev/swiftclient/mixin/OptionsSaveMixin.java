package dev.swiftclient.mixin;

import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.modules.FullBrightModule;
import dev.swiftclient.modules.ZoomModule;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FullBright and Zoom push gamma / FOV out of their normal range every tick. Put the player's real
 * values back right before options.txt is written; the modules reapply theirs on the next frame.
 */
@Mixin({Options.class})
public abstract class OptionsSaveMixin {
   @Inject(
      method = {"save"},
      at = {@At("HEAD")}
   )
   private void swiftclient$saveRealValues(CallbackInfo ci) {
      if (ModuleManager.isLoaded()) {
         ModuleManager.get(FullBrightModule.class).beforeOptionsSave();
         ModuleManager.get(ZoomModule.class).beforeOptionsSave();
      }
   }
}
