package dev.swiftclient.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.pvp.CombatTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * First-person swing animations (slash, chop, hook, spin...) and swing speed. Only your own view changes:
 * the attack itself and what other players see stay vanilla.
 */
@ConsumedBy({"ItemInHandAnimationsMixin", "LivingEntitySwingMixin"})
public final class SwingAnimationModule extends Module {
   public static volatile SwingAnimationModule active;
   private static final long COMBAT_WINDOW_MS = 3000L;
   public final ModuleSetting style;
   public final ModuleSetting tempo;
   public final ModuleSetting intensity;
   public final ModuleSetting offhand;
   public final ModuleSetting combatOnly;
   private boolean styled;

   public SwingAnimationModule() {
      super("swing_animation", "Swing Animation", "Changes your first-person swing animation and how fast it plays. Only your own view changes.", "Visual", "sword", false);
      this.style = this.cycle("style", "Style", new String[]{"Vanilla", "Slash", "Chop", "Hook", "Spin", "Double Tap", "Push"}, 1)
         .desc("The motion of your hand when you swing. Vanilla keeps the normal swing (use Tempo alone).");
      this.tempo = this.slider("tempo", "Tempo", 1.0, 0.4, 2.5, 0.05, "x").desc("Swing speed: below 1 slows it down, above 1 speeds it up.");
      this.intensity = this.slider("intensity", "Intensity", 1.0, 0.2, 2.0, 0.05, "x").desc("How wide the motion is.");
      this.offhand = this.toggle("offhand", "Offhand too", true).desc("Also animate the offhand.");
      this.combatOnly = this.toggle("combat_only", "Combat only", false).desc("Only use the style while fighting (a hit in the last 3 seconds, or a player or monster in your crosshair).");
   }

   @Override
   protected void onEnable() {
      this.latch();
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   public void latch() {
      this.styled = !this.combatOnly.boolValue() || inCombat();
   }

   private static boolean inCombat() {
      if (System.currentTimeMillis() - CombatTracker.lastAttackMs() < COMBAT_WINDOW_MS) {
         return true;
      } else {
         Entity looked = Minecraft.getInstance().crosshairPickEntity;
         return (looked instanceof Player || looked instanceof Enemy) && looked.isAlive();
      }
   }

   /** Does the style replace the vanilla swing for this hand? */
   public boolean covers(boolean mainHand) {
      return (mainHand || this.offhand.boolValue()) && this.styled && this.style.cycleIndex() != 0;
   }

   public int swingTicks(int ticks) {
      return !this.styled ? ticks : Math.max(1, Math.round((float)ticks / (float)this.tempo.value()));
   }

   public void pose(PoseStack pose, float progress, int side) {
      if (!(progress <= 0.0F) && !(progress >= 1.0F)) {
         float k = (float)this.intensity.value();
         switch (this.style.cycleIndex() - 1) {
            case 1 -> ankh(pose, progress, side, k);
            case 2 -> sickle(pose, progress, side, k);
            case 3 -> scarab(pose, progress, side, k);
            case 4 -> horus(pose, progress, side, k);
            case 5 -> sphinx(pose, progress, side, k);
            default -> khopesh(pose, progress, side, k);
         }

      }
   }

   private static void khopesh(PoseStack pose, float t, int side, float k) {
      float a = crest(t, 0.3F) * k;
      pose.translate((float)side * -0.3F * a, 0.05F * Mth.sin(((float)Math.PI * t)) * k, -0.1F * a);
      pose.mulPose(Axis.YP.rotationDegrees((float)side * 64.0F * a));
      pose.mulPose(Axis.ZP.rotationDegrees((float)side * 48.0F * a));
      pose.mulPose(Axis.XP.rotationDegrees(-14.0F * a));
   }

   private static void ankh(PoseStack pose, float t, int side, float k) {
      float raise = t < 0.28F ? Mth.sin(((float)Math.PI * t / 0.28F)) * k : 0.0F;
      float strike = t <= 0.18F ? 0.0F : crest((t - 0.18F) / 0.82F, 0.28F) * k;
      pose.translate((float)side * -0.06F * strike, 0.16F * raise - 0.1F * strike, 0.06F * raise - 0.24F * strike);
      pose.mulPose(Axis.XP.rotationDegrees(30.0F * raise - 105.0F * strike));
      pose.mulPose(Axis.ZP.rotationDegrees((float)side * 10.0F * strike));
   }

   private static void sickle(PoseStack pose, float t, int side, float k) {
      float a = crest(t, 0.42F);
      float hook = a * a * k;
      a *= k;
      pose.translate((float)side * -0.1F * a, -0.24F * a, -0.06F * a);
      pose.mulPose(Axis.ZP.rotationDegrees((float)side * -100.0F * a));
      pose.mulPose(Axis.YP.rotationDegrees((float)side * 30.0F * hook));
      pose.mulPose(Axis.XP.rotationDegrees(-25.0F * a));
   }

   private static void scarab(PoseStack pose, float t, int side, float k) {
      float turn = ((float)Math.PI * 2F) * t;
      float cos = Mth.cos(turn);
      float sin = Mth.sin(turn);
      float body = Mth.sin(((float)Math.PI * t)) * k;
      float r = 0.17F * k;
      pose.translate((float)side * r * 0.6F * (cos - 1.0F), r * sin, -0.12F * body);
      pose.mulPose(Axis.ZP.rotationDegrees((float)side * 40.0F * sin * k));
      pose.mulPose(Axis.XP.rotationDegrees(-40.0F * body));
   }

   private static void horus(PoseStack pose, float t, int side, float k) {
      float beat = Mth.sin((((float)Math.PI * 2F) * t));
      float w = beat * beat * (t < 0.5F ? 1.0F : 0.65F) * k;
      pose.translate((float)side * -0.08F * w, 0.06F * w, -0.1F * w);
      pose.mulPose(Axis.YP.rotationDegrees((float)side * 26.0F * w));
      pose.mulPose(Axis.XP.rotationDegrees(-50.0F * w));
      pose.mulPose(Axis.ZP.rotationDegrees((float)side * -22.0F * w));
   }

   private static void sphinx(PoseStack pose, float t, int side, float k) {
      float a = (0.5F - 0.5F * Mth.cos((((float)Math.PI * 2F) * t))) * k;
      pose.translate(0.0F, -0.06F * a, -0.1F * a);
      pose.mulPose(Axis.XP.rotationDegrees(-24.0F * a));
      pose.mulPose(Axis.YP.rotationDegrees((float)side * 10.0F * a));
   }

   private static float crest(float t, float peak) {
      if (!(t <= 0.0F) && !(t >= 1.0F)) {
         if (t < peak) {
            float u = 1.0F - t / peak;
            return 1.0F - u * u * u;
         } else {
            float u = (t - peak) / (1.0F - peak);
            return 1.0F - u * u * (3.0F - 2.0F * u);
         }
      } else {
         return 0.0F;
      }
   }
}
