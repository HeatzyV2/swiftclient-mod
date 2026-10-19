package dev.swiftclient.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

/** The item you switch to spins, flips or pops into your hand instead of just rising. */
@ConsumedBy("HotbarSwapMixin")
public final class HotbarSwapModule extends Module {
   public static volatile HotbarSwapModule active;
   private final ModuleSetting style = this.cycle("style", "Style", new String[]{"Spin", "Flip", "Roll", "Pop"}, 0).desc("Spin: tumbles up. Flip: turns around. Roll: rolls in. Pop: grows from small.");
   private final ModuleSetting strength = this.slider("strength", "Strength", 1.0, 0.3, 2.0, 0.1, "x").desc("How wide the motion is.");

   public HotbarSwapModule() {
      super("hotbar_swap", "Hotbar Swap", "Animates the item you switch to: it spins, flips, rolls or pops into your hand.", "Visual", "swap", false);
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   /** inverse is 1 right after the switch and falls to 0 when the item is up (vanilla equip progress). */
   public static void apply(PoseStack pose, float inverse, int side) {
      HotbarSwapModule module = active;
      if (module == null || inverse <= 0.001F) {
         return;
      }

      float k = module.strength.floatValue() * inverse;
      pose.translate((float)side * 0.56F, -0.52F, -0.72F);
      switch (module.style.cycleIndex()) {
         case 1 -> pose.mulPose(Axis.YP.rotationDegrees((float)side * 180.0F * k));
         case 2 -> pose.mulPose(Axis.ZP.rotationDegrees((float)side * -120.0F * k));
         case 3 -> {
            float s = Math.max(0.05F, 1.0F - 0.85F * Math.min(1.0F, k));
            pose.scale(s, s, s);
         }
         default -> pose.mulPose(Axis.XP.rotationDegrees(-160.0F * k));
      }

      pose.translate((float)(-side) * 0.56F, 0.52F, 0.72F);
   }
}
