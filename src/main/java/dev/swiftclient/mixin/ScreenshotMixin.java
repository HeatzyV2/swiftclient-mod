package dev.swiftclient.mixin;

import dev.swiftclient.modules.ScreenshotModule;
import java.util.function.Consumer;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Screenshot module: hooks the callback of the F2 screenshot (clipboard, notification, quiet chat). */
@Mixin({Screenshot.class})
public abstract class ScreenshotMixin {
   @ModifyArg(
      method = {"grab(Lnet/minecraft/client/Minecraft;Z)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Screenshot;grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V"
      ),
      index = 2
   )
   private static Consumer<Component> swiftclient$wrapCallback(Consumer<Component> callback) {
      return ScreenshotModule.wrap(callback);
   }
}
