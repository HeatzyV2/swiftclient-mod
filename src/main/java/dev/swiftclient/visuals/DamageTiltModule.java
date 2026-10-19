package dev.swiftclient.visuals;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

/** How hard the camera tilts when you take damage (vanilla is about 14 degrees). */
@ConsumedBy("DamageTiltMixin")
public final class DamageTiltModule extends Module {
   public static volatile DamageTiltModule active;
   private final ModuleSetting strength = this.slider("strength", "Strength", 1.5, 0.0, 4.0, 0.1, "x").desc("Multiplier of the hit tilt. 0 turns the tilt off, 1 is vanilla.");

   public DamageTiltModule() {
      super("damage_tilt", "Damage Tilt", "Sets how hard the camera tilts when you take damage.", "Visual", "gauge", false);
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   public static double scale(double vanilla) {
      DamageTiltModule module = active;
      return module != null ? vanilla * module.strength.value() : vanilla;
   }
}
