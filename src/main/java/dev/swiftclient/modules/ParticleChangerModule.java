package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** More hit particles, fewer distracting ones (explosions, block debris, rain splashes, potion swirls). */
@ConsumedBy({"ParticleEngineMixin", "MultiPlayerGameModeMixin"})
public final class ParticleChangerModule extends Module {
   public final ModuleSetting crits;
   public final ModuleSetting sharpness;
   public final ModuleSetting multiplier;
   public final ModuleSetting explosions;
   public final ModuleSetting blocks;
   public final ModuleSetting rain;
   public final ModuleSetting potions;

   public ParticleChangerModule() {
      super("particles", "Particle Changer", "More hit particles, fewer distracting ones: explosions, block debris, rain, potion swirls.", "Visual", "sparkles", false);
      this.crits = this.toggle("crits", "Always show crits", true).desc("Critical hit particles on every hit you land.").group("Hits");
      this.sharpness = this.toggle("sharpness", "Always show sharpness", false).desc("Blue enchanted-hit particles on every hit you land.").group("Hits");
      this.multiplier = this.slider("multiplier", "Multiplier", 1.0, 1.0, 5.0, 1.0, "x").desc("How many bursts of hit particles each hit makes.").group("Hits");
      this.explosions = this.toggle("explosions", "Hide explosions", false).desc("No explosion clouds (TNT, creepers, fireballs).").group("Hide");
      this.blocks = this.toggle("blocks", "Hide block debris", false).desc("No particles when blocks are mined or broken.").group("Hide");
      this.rain = this.toggle("rain", "Hide rain splashes", false).desc("No splash particles where rain lands.").group("Hide");
      this.potions = this.toggle("potions", "Hide potion swirls", false).desc("No swirls around entities under potion effects.").group("Hide");
   }

   private static ParticleChangerModule active() {
      if (!ModuleManager.isLoaded()) {
         return null;
      }

      ParticleChangerModule m = ModuleManager.get(ParticleChangerModule.class);
      return m.isEnabled() ? m : null;
   }

   /** Should this particle type be dropped? */
   public static boolean hidden(ParticleOptions options) {
      ParticleChangerModule m = active();
      if (m == null) {
         return false;
      }

      ParticleType<?> t = options.getType();
      return m.explosions.boolValue() && (t == ParticleTypes.EXPLOSION || t == ParticleTypes.EXPLOSION_EMITTER)
         || m.rain.boolValue() && t == ParticleTypes.RAIN
         || m.potions.boolValue() && t == ParticleTypes.ENTITY_EFFECT;
   }

   /** Block debris is added directly as particle objects, not through a particle type. */
   public static boolean hidden(Particle particle) {
      ParticleChangerModule m = active();
      return m != null && m.blocks.boolValue() && particle instanceof TerrainParticle;
   }

   /** You hit {@code target}: add the particles asked for. */
   public static void onAttack(Entity target) {
      ParticleChangerModule m = active();
      Minecraft mc = Minecraft.getInstance();
      if (m != null && target instanceof LivingEntity && mc.particleEngine != null) {
         int n = (int)Math.round(m.multiplier.value());
         for (int i = 0; i < n; i++) {
            if (m.crits.boolValue()) {
               mc.particleEngine.createTrackingEmitter(target, ParticleTypes.CRIT);
            }

            if (m.sharpness.boolValue()) {
               mc.particleEngine.createTrackingEmitter(target, ParticleTypes.ENCHANTED_HIT);
            }
         }
      }
   }
}
