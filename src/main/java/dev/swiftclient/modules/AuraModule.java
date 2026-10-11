package dev.swiftclient.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.player.LocalPlayer;

/**
 * A light particle aura that winds around you. It is drawn by your own game only: other players do not
 * see it, so it never costs the server anything.
 */
public final class AuraModule extends Module {
   private static final String[] STYLES = {"Sparkles", "Hearts", "Soul flames", "Magic", "Snow", "Notes"};
   public final ModuleSetting style;
   public final ModuleSetting density;
   public final ModuleSetting radius;
   public final ModuleSetting firstPerson;
   private int phase;

   public AuraModule() {
      super("aura", "Aura", "A light particle aura around you. Only you see it: other players do not.", "Visual", "sparkles", false);
      this.style = this.cycle("style", "Style", STYLES, 0).desc("The particles that wind around you.");
      this.density = this.slider("density", "Density", 2.0, 1.0, 8.0, 1.0, "").desc("How many particles per tick. Higher looks fuller and costs a little more.");
      this.radius = this.slider("radius", "Radius", 0.7, 0.4, 1.5, 0.1, "").desc("How far from your body the aura winds.");
      this.firstPerson = this.toggle("first_person", "In first person", false).desc("Also draw it when you play in first person (it can get in the way of your view).");
   }

   private ParticleOptions particle() {
      return switch (this.style.cycleIndex()) {
         case 1 -> ParticleTypes.HEART;
         case 2 -> ParticleTypes.SOUL_FIRE_FLAME;
         case 3 -> ParticleTypes.ENCHANT;
         case 4 -> ParticleTypes.SNOWFLAKE;
         case 5 -> ParticleTypes.NOTE;
         default -> ParticleTypes.END_ROD;
      };
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      if (p == null || mc.level == null || p.isSpectator() || p.isInvisible()) {
         return;
      }

      if (mc.options.getCameraType().isFirstPerson() && !this.firstPerson.boolValue()) {
         return;
      }

      ParticleOptions options = this.particle();
      int n = (int)Math.round(this.density.value());
      double r = this.radius.value();
      for (int i = 0; i < n; i++) {
         this.phase++;
         double angle = this.phase * 0.35;
         double rise = (this.phase % 36) / 36.0;
         double x = p.getX() + Math.cos(angle) * r;
         double y = p.getY() + 0.1 + rise * (p.getBbHeight() + 0.2);
         double z = p.getZ() + Math.sin(angle) * r;
         mc.level.addParticle(options, x, y, z, 0.0, 0.01, 0.0);
      }
   }
}
