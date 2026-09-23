package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class SpeedModule extends Module {
   public SpeedModule() {
      super("hud_speed", "Speed", "Movement speed. Move it in the HUD Editor.", "HUD", "gauge", false);
      this.settings.add(ModuleSetting.cycle("hud_speed", "unit", "Unit", new String[]{"m/s", "km/h"}, 0).desc("Metres per second, or kilometres per hour."));
      this.settings
         .add(
            ModuleSetting.toggle("hud_speed", "vertical", "Include vertical speed", false)
               .desc("Count falling and climbing as speed, not just horizontal movement.")
         );
      this.settings.add(ModuleSetting.slider("hud_speed", "decimals", "Decimals", 2.0, 0.0, 3.0, 1.0, "").desc("How many digits after the decimal point."));
      this.settings.add(ModuleSetting.toggle("hud_speed", "label", "Show unit", true).desc("Show the unit next to the number."));
   }
}
