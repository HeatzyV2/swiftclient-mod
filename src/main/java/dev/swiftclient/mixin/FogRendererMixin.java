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
   @ModifyReturnValue(
      method = {"setupFog"},
      at = {@At("RETURN")}
   )
   private FogData swiftclient$customFog(FogData fog, @Local(argsOnly = true) Camera camera) {
      return FogModule.adjust(fog, camera.getFluidInCamera());
   }
}
