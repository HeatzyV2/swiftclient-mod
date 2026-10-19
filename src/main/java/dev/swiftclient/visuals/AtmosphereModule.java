package dev.swiftclient.visuals;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.mixin.ClientLevelSkyFlashAccessor;
import dev.swiftclient.core.mods.ConsumedBy;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import com.mojang.math.Axis;
import net.minecraft.util.ARGB;
import org.joml.Vector3fc;
import org.joml.Vector4f;

@ConsumedBy({"AtmosphereSkyMixin", "AtmosphereFogMixin", "AtmosphereCloudsMixin", "AtmosphereRainFogMixin"})
public final class AtmosphereModule extends Module {
   private static volatile AtmosphereModule active;
   private static final String CUSTOM = "Custom";
   private static final String AURORA_ALWAYS = "Always";
   private static final String AURORA_COLD = "Cold Biomes";
   private static final String AURORA_OFF = "Off";
   private static final float MOON_SCALE = 1.4F;
   private static final Theme[] THEMES = new Theme[]{new Theme("Borealis", 198414, 729648, 13073302, 3080090, 9063935, 13163263), new Theme("Nebula", 459791, 2233916, 14053304, 16732120, 5991679, 15124735), new Theme("Swift", 0x040816, 0x0E1F45, 0xE07A5F, 0x3B82F6, 0x6FE3FF, 0xBFD8FF), new Theme("Frost", 264724, 1454154, 11057384, 8382719, 15135999, 14544639), new Theme("Crimson", 721926, 3018260, 15227490, 16724821, 16751164, 16765640)};
   private final ModuleSetting theme = this.cycle("theme", "Theme", new String[]{"Borealis", "Nebula", "Swift", "Frost", "Crimson", "Custom"}, 0).desc("Colours of the night sky, sunsets and aurora. Custom uses the colours below.");
   private final ModuleSetting nightColor = this.color("night_color", "Night sky color", -16047568).desc("Colour of the night sky (Custom theme).");
   private final ModuleSetting sunsetColor = this.color("sunset_color", "Sunset color", -3703914).desc("Colour of sunrises and sunsets (Custom theme).");
   private final ModuleSetting auroraColor = this.color("aurora_color", "Aurora color", -13697126).desc("Colour at the bottom of the aurora (Custom theme).");
   private final ModuleSetting auroraTopColor = this.color("aurora_top_color", "Aurora top color", -7713281).desc("Colour at the top of the aurora (Custom theme).");
   private final ModuleSetting skyShader = this.cycle("sky_shader", "Sky shader", new String[]{"Off", "Space", "Northern Lights", "Ringed Planet", "Black Hole"}, 0).desc("A full animated sky drawn on the GPU. Off keeps the gradient sky.");
   private final ModuleSetting shaderDaytime = this.slider("sky_shader_daytime", "Shader in daytime", 1.0, 0.0, 1.0, 0.05, "").desc("How much of the shader sky stays visible during the day.");
   private final ModuleSetting shaderBrightness = this.slider("sky_shader_brightness", "Shader brightness", 1.0, 0.2, 2.0, 0.1, "").desc("Brightness of the shader sky.");
   private final ModuleSetting shaderSpeed = this.slider("sky_shader_speed", "Shader speed", 1.0, 0.0, 3.0, 0.1, "").desc("Animation speed of the shader sky.");
   private final ModuleSetting shaderStars = this.slider("sky_shader_stars", "Shader stars", 1.0, 0.0, 2.0, 0.1, "").desc("How many stars the shader sky shows.");
   private final ModuleSetting shaderEffect = this.slider("sky_shader_effect", "Nebula / aurora", 1.0, 0.0, 2.0, 0.1, "").desc("Strength of the nebula or aurora in the shader sky.");
   private final ModuleSetting shaderBand = this.slider("sky_shader_galaxy", "Galaxy band", 1.0, 0.0, 2.0, 0.1, "").desc("Strength of the galaxy band in the shader sky.");
   private final ModuleSetting gradientSky = this.toggle("gradient_sky", "Gradient sky", true).desc("A smooth gradient dome instead of the flat vanilla sky.");
   private final ModuleSetting keepInRain = this.toggle("keep_in_rain", "Clear sky in rain", true).desc("The sky and stars stay visible when it rains.");
   private final ModuleSetting celestialGlow = this.toggle("sun_moon_glow", "Sun & moon glow", true).desc("A soft halo around the sun and the moon.");
   private final ModuleSetting stars = this.toggle("stars", "Stars", true).desc("A denser, twinkling star field.");
   private final ModuleSetting starBrightness = this.slider("star_brightness", "Star brightness", 1.3, 0.5, 2.0, 0.1, "").desc("How bright the stars are.");
   private final ModuleSetting milkyWay = this.toggle("milky_way", "Milky Way", true).desc("A galaxy band across the night sky.");
   private final ModuleSetting milkyWayBrightness = this.slider("milky_way_brightness", "Milky Way brightness", 1.0, 0.2, 2.0, 0.1, "").desc("How bright the Milky Way is.");
   private final ModuleSetting shootingStars = this.toggle("shooting_stars", "Shooting stars", true).desc("Now and then a star streaks across the sky.");
   private final ModuleSetting aurora = this.cycle("aurora", "Aurora", new String[]{"Always", "Cold Biomes", "Off"}, 0).desc("Northern lights at night: always, only in cold biomes, or never.");
   private final ModuleSetting auroraBrightness = this.slider("aurora_brightness", "Aurora brightness", 1.0, 0.2, 2.0, 0.1, "").desc("How bright the aurora is.");
   private final ModuleSetting auroraSpeed = this.slider("aurora_speed", "Aurora speed", 1.0, 0.2, 3.0, 0.1, "").desc("How fast the aurora waves.");
   private final ModuleSetting cloudColors = this.toggle("cloud_colors", "Cloud colors", true).desc("Clouds take the colours of the sky.");
   private final ModuleSetting endSky = this.toggle("end_sky", "End sky", true).desc("A nebula sky in the End instead of the vanilla texture.");
   private final ModuleSetting netherColors = this.toggle("nether_colors", "Nether colors", true).desc("Richer fog colours in the Nether.");
   private final AtmosphereSky sky = new AtmosphereSky();
   private final AtmosphereStars starField = new AtmosphereStars();
   private final AtmosphereAurora auroraMesh = new AtmosphereAurora();
   private final AtmosphereShaderSky shaderSky = new AtmosphereShaderSky();
   private final PoseStack world = new PoseStack();
   private float night;
   private float rainBrightness = 1.0F;
   private float auroraHere;
   private long lastFrame;
   private boolean moonScaled;
   private boolean shaderRendered;

   public AtmosphereModule() {
      super("atmosphere", "Atmosphere", "A custom sky: stars, milky way, shooting stars, aurora and sky shaders. Only on your screen.", "Visual", "star", false);
   }

   public static AtmosphereModule get() {
      AtmosphereModule module = active;
      return module != null && module.isEnabled() ? module : null;
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      this.sky.close();
      this.starField.close();
      this.auroraMesh.close();
      this.shaderSky.close();
      this.shaderRendered = false;
      this.lastFrame = 0L;
      this.auroraHere = 0.0F;
   }

   public void extract(ClientLevel level, float partialTick, SkyRenderState state) {
      this.shaderRendered = false;
      this.night = Mth.clamp(state.starBrightness * 2.0F, 0.0F, 1.0F);
      float rain = 1.0F - state.rainBrightness;
      float thunder = level.getThunderLevel(partialTick);
      if (this.keepInRain.boolValue()) {
         this.night = Math.max(this.night, clearNight(state.sunAngle) * rain);
         state.starBrightness = Math.max(state.starBrightness, this.night * 0.5F);
         state.rainBrightness = 1.0F;
         rain = 0.0F;
         thunder = 0.0F;
      }

      this.rainBrightness = 1.0F - rain;
      Theme colours = this.currentTheme();
      int flashTicks = ((ClientLevelSkyFlashAccessor)level).swiftclient$skyFlashTime();
      this.sky.update(state.sunAngle, state.moonAngle, rain, thunder, flashTicks > 0 ? Math.min(1.0F, ((float)flashTicks - partialTick) / 2.0F) : 0.0F, colours.nightZenith, colours.nightHorizon, colours.twilight);
   }

   public boolean keepsSkyInRain() {
      return this.keepInRain.boolValue();
   }

   private static float clearNight(float sunAngle) {
      float dark = Mth.clamp(0.75F - 2.0F * Mth.cos((double)sunAngle), 0.0F, 1.0F);
      return dark * dark;
   }

   public boolean skyDisc() {
      if (this.gradientSky.boolValue() && this.sky.ready()) {
         if (false && this.shaderOn() && this.shaderVisibility() >= 1.0F) {
            this.renderShaderSky();
            if (!this.shaderRendered) {
               this.sky.renderDome(this.world);
            }

            return true;
         } else {
            this.sky.renderDome(this.world);
            this.renderShaderSky();
            return true;
         }
      } else {
         return false;
      }
   }

   public void afterSkyDisc() {
      this.renderShaderSky();
   }

   private void renderShaderSky() {
      if (this.shaderOn()) {
         Theme colours = this.currentTheme();
         this.shaderRendered = this.shaderSky.render(this.skyShader.label(), this.shaderVisibility(), this.shaderBrightness.floatValue(), this.shaderSpeed.floatValue(), this.shaderStars.floatValue(), this.shaderEffect.floatValue(), this.shaderBand.floatValue(), colours.auroraLow, colours.auroraHigh, colours.milkyWay, colours.nightZenith, colours.nightHorizon);
      }
   }

   private boolean shaderOn() {
      return !this.skyShader.is("Off");
   }

   private boolean shaderAurora() {
      return this.skyShader.is("Northern Lights");
   }

   private float shaderVisibility() {
      return Mth.lerp(this.night, this.shaderDaytime.floatValue(), 1.0F) * (0.4F + 0.6F * this.rainBrightness);
   }

   public boolean replacesSunrise() {
      return this.gradientSky.boolValue() && this.sky.ready();
   }

   public boolean stars(PoseStack pose) {
      if (this.shaderRendered) {
         return true;
      } else {
         float clear = this.night * this.rainBrightness;
         if (this.milkyWay.boolValue()) {
            this.starField.renderBand(pose, clear * this.milkyWayBrightness.floatValue(), this.currentTheme().milkyWay);
         }

         if (!this.stars.boolValue()) {
            return false;
         } else {
            this.starField.renderStars(pose, clear, this.starBrightness.floatValue());
            return true;
         }
      }
   }

   public void beforeMoon(PoseStack pose) {
      this.moonScaled = this.celestialGlow.boolValue();
      if (this.moonScaled) {
         pose.pushPose();
         pose.scale(1.4F, 1.0F, 1.4F);
      }
   }

   public void afterMoon(PoseStack pose) {
      if (this.moonScaled) {
         pose.popPose();
      }

      this.moonScaled = false;
   }

   public void afterCelestials() {
      long now = System.nanoTime();
      float dt = this.lastFrame == 0L ? 0.0F : Math.min((float)(now - this.lastFrame) / 1.0E9F, 0.1F);
      this.lastFrame = now;
      if (this.sky.ready()) {
         if (this.celestialGlow.boolValue()) {
            this.sky.renderHalos(this.world, this.rainBrightness, this.night);
         }

         if (this.shootingStars.boolValue()) {
            this.sky.renderMeteors(this.world, this.night, this.rainBrightness);
         }

         this.auroraHere += (this.auroraTarget() - this.auroraHere) * Math.min(1.0F, dt * 0.5F);
         if (this.auroraHere > 0.01F && this.night > 0.01F && !this.shaderRendered) {
            Theme colours = this.currentTheme();
            float time = (float)(now % 3600000000000L) / 1.0E9F * this.auroraSpeed.floatValue();
            float intensity = this.auroraHere * this.night * (0.35F + 0.65F * this.rainBrightness) * this.auroraBrightness.floatValue();
            this.auroraMesh.render(this.world, intensity, time, colours.auroraLow, colours.auroraHigh);
         }

      }
   }

   private float auroraTarget() {
      if (this.aurora.is("Off")) {
         return 0.0F;
      } else if (this.aurora.is("Always")) {
         return 1.0F;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level == null) {
            return 0.0F;
         } else {
            BlockPos at = BlockPos.containing(mc.gameRenderer.mainCamera().position());
            return ((Biome)mc.level.getBiome(at).value()).coldEnoughToSnow(at, mc.level.getSeaLevel()) ? 1.0F : 0.0F;
         }
      }
   }

   public void fog(Camera camera, ClientLevel level, Vector4f color) {
      if (level.dimension() == Level.OVERWORLD) {
         if (this.gradientSky.boolValue() && this.sky.ready()) {
            Vector3fc look = camera.forwardVector();
            int rgb = this.sky.horizonToward(look.x(), look.z());
            color.x = (float)(rgb >> 16 & 255) / 255.0F;
            color.y = (float)(rgb >> 8 & 255) / 255.0F;
            color.z = (float)(rgb & 255) / 255.0F;
         }

         if (this.shaderOn()) {
            this.shaderFog(color);
         }
      } else if (level.dimension() == Level.NETHER && this.netherColors.boolValue()) {
         float luma = 0.2126F * color.x + 0.7152F * color.y + 0.0722F * color.z;
         color.x = Mth.clamp((luma + (color.x - luma) * 1.45F) * 1.12F, 0.0F, 1.0F);
         color.y = Mth.clamp((luma + (color.y - luma) * 1.45F) * 1.12F, 0.0F, 1.0F);
         color.z = Mth.clamp((luma + (color.z - luma) * 1.45F) * 1.12F, 0.0F, 1.0F);
      }

   }

   private void shaderFog(Vector4f color) {
      int horizon = this.currentTheme().nightHorizon;
      float scale = this.shaderAurora() ? 0.85F : 0.3F;
      float amount = this.shaderVisibility() * 0.85F;
      color.x = Mth.lerp(amount, color.x, (float)(horizon >> 16 & 255) / 255.0F * scale);
      color.y = Mth.lerp(amount, color.y, (float)(horizon >> 8 & 255) / 255.0F * scale);
      color.z = Mth.lerp(amount, color.z, (float)(horizon & 255) / 255.0F * scale);
   }

   public int clouds(int argb) {
      Minecraft mc = Minecraft.getInstance();
      if (this.cloudColors.boolValue() && this.sky.ready() && mc.level != null && mc.level.dimension() == Level.OVERWORLD) {
         float golden = this.sky.golden();
         float dark = this.sky.nightness();
         int target = ARGB.srgbLerp(golden, ARGB.srgbLerp(dark, 16777215, this.currentTheme().nightHorizon), this.sky.twilightGlow());
         float amount = Math.max(golden * 0.75F, dark * 0.55F);
         return keepBrightness(argb, target, amount);
      } else {
         return argb;
      }
   }

   public boolean endSky() {
      if (!this.endSky.boolValue()) {
         return false;
      } else {
         long now = System.nanoTime();
         this.sky.renderEndDome(this.world);
         PoseStack turning = new PoseStack();
         turning.mulPose(Axis.YP.rotationDegrees((float)(now % 1200000000000L) / 1.0E9F * 0.3F));
         this.starField.renderNebulae(turning, 1.0F);
         this.starField.renderStars(turning, 1.0F, 1.2F);
         return true;
      }
   }

   private Theme currentTheme() {
      for(Theme each : THEMES) {
         if (this.theme.is(each.name)) {
            return each;
         }
      }

      int horizon = (this.nightColor.colorValue() & 0xFFFFFF);
      return new Theme("Custom", AtmosphereSky.scale(horizon, 0.3F), horizon, (this.sunsetColor.colorValue() & 0xFFFFFF), (this.auroraColor.colorValue() & 0xFFFFFF), (this.auroraTopColor.colorValue() & 0xFFFFFF), 15263999);
   }

   private static int keepBrightness(int argb, int target, float amount) {
      if (amount <= 0.0F) {
         return argb;
      } else {
         float r = (float)(argb >> 16 & 255) / 255.0F;
         float g = (float)(argb >> 8 & 255) / 255.0F;
         float b = (float)(argb & 255) / 255.0F;
         float tr = (float)(target >> 16 & 255) / 255.0F;
         float tg = (float)(target >> 8 & 255) / 255.0F;
         float tb = (float)(target & 255) / 255.0F;
         float scale = luminance(r, g, b) / Math.max(luminance(tr, tg, tb), 0.001F);
         r = Mth.lerp(amount, r, Math.min(1.0F, tr * scale));
         g = Mth.lerp(amount, g, Math.min(1.0F, tg * scale));
         b = Mth.lerp(amount, b, Math.min(1.0F, tb * scale));
         return argb & -16777216 | channel(r) << 16 | channel(g) << 8 | channel(b);
      }
   }

   private static float luminance(float r, float g, float b) {
      return 0.2126F * r + 0.7152F * g + 0.0722F * b;
   }

   private static int channel(float value) {
      return Mth.clamp(Math.round(value * 255.0F), 0, 255);
   }

   private static record Theme(String name, int nightZenith, int nightHorizon, int twilight, int auroraLow, int auroraHigh, int milkyWay) {
   }
}
