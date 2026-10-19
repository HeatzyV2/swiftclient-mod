package dev.swiftclient.mixin;

import dev.swiftclient.visuals.ViewModelModule;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** View Model: the hands are drawn with their own projection (hudFov), so only that FOV changes. */
@Mixin({GameRenderer.class})
public abstract class ViewModelFovMixin {
   @ModifyArg(
      method = {"renderLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/Projection;setupPerspective(FFFFF)V"
      ),
      index = 2
   )
   private float swiftclient$viewModelFov(float fov) {
      return ViewModelModule.fov(fov);
   }
}
