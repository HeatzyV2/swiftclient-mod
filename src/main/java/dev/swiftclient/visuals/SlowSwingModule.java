package dev.swiftclient.visuals;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

/** Slows down (or speeds up) your hand swing animation. Only on your screen, attacks are not affected. */
@ConsumedBy("LivingEntitySwingMixin")
public final class SlowSwingModule extends Module {
   public static volatile SlowSwingModule active;
   public final ModuleSetting speed;

   public SlowSwingModule() {
      super("slow_swing", "Slow Swing", "Slows down your hand swing animation. Only the animation changes, not your attacks.", "Visual", "hand", false);
      this.speed = this.slider("speed", "Speed", 0.25, 0.1, 2.0, 0.05, "x").desc("Speed of the swing. 1 is vanilla, lower is slower.");
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   public int swingTicks(int ticks) {
      return Math.max(1, Math.round((float)ticks / (float)Math.max(0.1, this.speed.value())));
   }
}
