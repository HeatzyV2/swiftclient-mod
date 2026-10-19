package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.swiftclient.modules.ZoomModule;
import dev.swiftclient.visuals.CustomFovModule;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Custom FOV: replaces the FOV option where the camera computes the world FOV (not while Zoom drives it). */
@Mixin({Camera.class})
public abstract class CustomFovMixin {
   @ModifyExpressionValue(
      method = {"calculateFov"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/lang/Integer;intValue()I"
      )
   )
   private int swiftclient$customFov(int fov) {
      return ZoomModule.zooming() ? fov : CustomFovModule.base(fov);
   }
}
