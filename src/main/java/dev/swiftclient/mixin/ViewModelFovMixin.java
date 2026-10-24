package dev.swiftclient.mixin;

import dev.swiftclient.visuals.ViewModelModule;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** View Model: the field of view of the hand and held item, apart from the world's. */
@Mixin({GameRenderer.class})
public abstract class ViewModelFovMixin {
   //? if >=26.1 {
   @org.spongepowered.asm.mixin.injection.ModifyArg(
      // 26.3 moved the hand projection to render3dHud.
      method = {"renderLevel", "render3dHud"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/Projection;setupPerspective(FFFFF)V"
      ),
      index = 2
   )
   //?} else if >=1.21.6 {
   /*/^* The second getFov of renderLevel is the one of the 3D hud (the hand). ^/
   @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
      method = {"renderLevel"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getFov(Lnet/minecraft/client/Camera;FZ)F", ordinal = 1)
   )
   *///?} else {
   /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
      method = {"renderItemInHand"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getFov(Lnet/minecraft/client/Camera;FZ)F")
   )
   *///?}
   private float swiftclient$viewModelFov(float fov) {
      return ViewModelModule.fov(fov);
   }
}
