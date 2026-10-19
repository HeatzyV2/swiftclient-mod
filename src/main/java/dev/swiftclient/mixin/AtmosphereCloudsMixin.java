package dev.swiftclient.mixin;

import dev.swiftclient.visuals.AtmosphereModule;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Atmosphere: clouds tinted to match the sky. */
@Mixin({CloudRenderer.class})
public abstract class AtmosphereCloudsMixin {
   @ModifyVariable(
      method = {"render"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private int swiftclient$cloudColour(int color) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      return atmosphere != null ? atmosphere.clouds(color) : color;
   }
}
