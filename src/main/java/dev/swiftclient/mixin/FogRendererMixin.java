package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.swiftclient.modules.FogModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fog Customizer: edits the fog vanilla computed for the frame, before it is uploaded (Sodium reads the same buffer). */
@Mixin({FogRenderer.class})
public abstract class FogRendererMixin {
   //? if >=26.1 {
   @ModifyReturnValue(
      method = {"setupFog"},
      at = {@At("RETURN")}
   )
   private FogData swiftclient$customFog(FogData fog, @Local(argsOnly = true) Camera camera) {
      return FogModule.adjust(fog, camera.getFluidInCamera());
   }
   //?} else {
   /*/^* Before 26.1 setupFog fills a local FogData and returns the colour: edit both before the upload. ^/
   @org.spongepowered.asm.mixin.injection.Inject(
      method = {"setupFog"},
      at = {@At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;renderDistanceEnd:F", opcode = 181, shift = At.Shift.AFTER)}
   )
   private void swiftclient$customFog(
      org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<org.joml.Vector4f> cir,
      @Local(argsOnly = true) Camera camera,
      @Local FogData fog,
      @Local org.joml.Vector4f color
   ) {
      FogModule.adjust(fog, camera.getFluidInCamera());
      FogModule.tint(color);
   }
   *///?}
}
