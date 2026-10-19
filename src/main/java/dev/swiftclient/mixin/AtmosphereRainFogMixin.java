package dev.swiftclient.mixin;

import dev.swiftclient.visuals.AtmosphereModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Atmosphere "Keep in rain": the sky stays clear, so the rain fog does not close in either. */
@Mixin({AtmosphericFogEnvironment.class})
public abstract class AtmosphereRainFogMixin {
   @Shadow
   private float rainFogMultiplier;

   @Inject(
      method = {"updateRainFogState"},
      at = {@At("TAIL")}
   )
   private void swiftclient$keepClearInRain(Camera camera, ClientLevel level, DeltaTracker deltaTracker, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.keepsSkyInRain()) {
         this.rainFogMultiplier = 0.0F;
      }
   }
}
