package dev.swiftclient.visuals;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Particles, a sound and a flash when you finish off an enemy. */
public final class KillEffectModule extends Module {
   public static volatile KillEffectModule active;
   private final ModuleSetting effect = this.cycle("effect", "Effect", new String[]{"Fireworks", "Souls", "Totem", "Thunder"}, 0).desc("The particles and sound played where the enemy fell.");
   private final ModuleSetting playersOnly = this.toggle("players_only", "Players only", true).desc("Only react to players, not mobs.");
   private final ModuleSetting sound = this.toggle("sound", "Sound", true).desc("Plays the sound of the effect.");
   private final ModuleSetting flash = this.toggle("flash", "Screen flash", true).desc("A short flash and a kill counter on screen.");
   private int lastKill = -1;
   private int streak;
   private long flashAt;
   private long lastKillAt;

   public KillEffectModule() {
      super("kill_effect", "Kill Effect", "Particles, a sound and a flash when you finish off an enemy.", "Visual", "lightning", false);
   }

   @Override
   protected void onEnable() {
      active = this;
      this.lastKill = -1;
   }

   @Override
   protected void onDisable() {
      active = null;
      this.flashAt = 0L;
   }

   @Override
   protected void onTick() {
      LocalPlayer player = Minecraft.getInstance().player;
      ClientLevel level = Minecraft.getInstance().level;
      if (player == null || level == null) {
         return;
      }

      LivingEntity target = player.getLastHurtMob();
      if (target == null || target == player || target.getId() == this.lastKill || !target.isDeadOrDying()) {
         return;
      }

      if (player.tickCount - player.getLastHurtMobTimestamp() > 100 || this.playersOnly.boolValue() && !(target instanceof Player)) {
         return;
      }

      this.lastKill = target.getId();
      this.kill(level, target);
   }

   private void kill(ClientLevel level, LivingEntity target) {
      long now = System.nanoTime();
      this.streak = now - this.lastKillAt < 8000000000L ? this.streak + 1 : 1;
      this.lastKillAt = now;
      this.flashAt = now;
      double x = target.getX();
      double y = target.getY() + target.getBbHeight() * 0.5;
      double z = target.getZ();
      ParticleOptions particle;
      SoundEvent sound;
      switch (this.effect.cycleIndex()) {
         case 1 -> {
            particle = ParticleTypes.SOUL;
            sound = SoundEvents.SOUL_ESCAPE.value();
         }
         case 2 -> {
            particle = ParticleTypes.TOTEM_OF_UNDYING;
            sound = SoundEvents.TOTEM_USE;
         }
         case 3 -> {
            particle = ParticleTypes.ELECTRIC_SPARK;
            sound = SoundEvents.LIGHTNING_BOLT_THUNDER;
         }
         default -> {
            particle = ParticleTypes.FIREWORK;
            sound = SoundEvents.FIREWORK_ROCKET_BLAST;
         }
      }

      for (int i = 0; i < 40; i++) {
         double a = level.getRandom().nextDouble() * Math.PI * 2.0;
         double up = level.getRandom().nextDouble() * 0.5 - 0.1;
         level.addParticle(particle, x, y, z, Math.cos(a) * 0.25, up, Math.sin(a) * 0.25);
      }

      if (this.sound.boolValue()) {
         level.playLocalSound(x, y, z, sound, SoundSource.PLAYERS, 0.8F, 1.0F, false);
      }
   }

   /** Drawn with the HUD (ExtraHud). */
   public static void render(Canvas dl, int w, int h) {
      KillEffectModule module = active;
      if (module == null || !module.flash.boolValue() || module.flashAt == 0L) {
         return;
      }

      float f = (float)(System.nanoTime() - module.flashAt) / 1.2E9F;
      if (f >= 1.0F) {
         return;
      }

      int veil = Math.round(70.0F * Math.max(0.0F, 1.0F - f * 4.0F));
      if (veil > 0) {
         dl.fill(0, 0, w, h, veil << 24 | 0xFFFFFF);
      }

      int alpha = Math.round(255.0F * (f < 0.6F ? 1.0F : (1.0F - f) / 0.4F));
      String text = module.streak > 1 ? "KILL x" + module.streak : "KILL";
      int y = h / 2 + 34 - Math.round(f * 8.0F);
      dl.pushScale(w / 2, y, 1.6F);
      dl.centeredText(text, w / 2, y, alpha << 24 | 0xFF5C5C, true);
      dl.popScale();
   }
}
