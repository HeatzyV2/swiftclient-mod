package dev.swiftclient.mixin;

import dev.swiftclient.visuals.AtmosphereModule;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Atmosphere: the horizon fog takes the colours of the custom sky (not under water, lava or blindness). */
@Mixin({FogRenderer.class})
public abstract class AtmosphereFogMixin {
   @Inject(
      method = {"computeFogColor"},
      at = {@At("TAIL")}
   )
   private void swiftclient$fogColour(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkness, Vector4f dest, CallbackInfo ci) {
      if (camera.getFluidInCamera() != FogType.NONE || darkness > 0.0F) {
         return;
      }

      if (camera.entity() instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) {
         return;
      }

      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         atmosphere.fog(camera, level, dest);
      }
   }
}
