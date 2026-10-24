package dev.swiftclient.mixin;

import dev.swiftclient.modules.ScreenshotModule;
import java.util.function.Consumer;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Screenshot module: hooks the callback of the F2 screenshot (clipboard, notification, quiet chat). */
@Mixin({Screenshot.class})
public abstract class ScreenshotMixin {
   // Every F2 screenshot goes through this overload, in every version.
   @ModifyVariable(
      method = {"grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private static Consumer<Component> swiftclient$wrapCallback(Consumer<Component> callback) {
      return ScreenshotModule.wrap(callback);
   }
}
