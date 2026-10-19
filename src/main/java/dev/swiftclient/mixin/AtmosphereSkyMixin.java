package dev.swiftclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.swiftclient.visuals.AtmosphereModule;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Atmosphere: replaces or decorates each part of the vanilla sky (dome, sunset fan, stars, moon, end sky). */
@Mixin({SkyRenderer.class})
public abstract class AtmosphereSkyMixin {
   @Inject(
      method = {"extractRenderState"},
      at = {@At("TAIL")}
   )
   private void swiftclient$readSky(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && state.skybox == DimensionType.Skybox.OVERWORLD) {
         atmosphere.extract(level, partialTick, state);
      }
   }

   @Inject(
      method = {"renderSkyDisc"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$dome(int color, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.skyDisc()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderSkyDisc"},
      at = {@At("TAIL")}
   )
   private void swiftclient$afterDome(int color, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         atmosphere.afterSkyDisc();
      }
   }

   @Inject(
      method = {"renderSunriseAndSunset"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$noSunriseFan(PoseStack pose, float sunAngle, int color, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.replacesSunrise()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderStars"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$stars(float starBrightness, PoseStack pose, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.stars(pose)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderMoon"},
      at = {@At("HEAD")}
   )
   private void swiftclient$beforeMoon(MoonPhase moonPhase, float rainBrightness, PoseStack pose, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         atmosphere.beforeMoon(pose);
      }
   }

   @Inject(
      method = {"renderMoon"},
      at = {@At("TAIL")}
   )
   private void swiftclient$afterMoon(MoonPhase moonPhase, float rainBrightness, PoseStack pose, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         atmosphere.afterMoon(pose);
      }
   }

   @Inject(
      method = {"renderSunMoonAndStars"},
      at = {@At("TAIL")}
   )
   private void swiftclient$afterCelestials(
      PoseStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         atmosphere.afterCelestials();
      }
   }

   @Inject(
      method = {"renderEndSky"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$endSky(CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.endSky()) {
         ci.cancel();
      }
   }
}
