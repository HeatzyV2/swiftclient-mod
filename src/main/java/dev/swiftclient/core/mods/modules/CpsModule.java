package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class CpsModule extends Module {
   public CpsModule() {
      super("hud_cps", "CPS", "Clicks per second. Move it in the HUD Editor.", "HUD", "click", false);
      this.settings
         .add(
            ModuleSetting.cycle("hud_cps", "button", "Button", new String[]{"Left", "Right", "Both"}, 0)
               .desc("Which mouse button is counted: left, right, or both added together.")
         );
      this.settings.add(ModuleSetting.toggle("hud_cps", "label", "Show \"CPS\" label", true).desc("Prefix the number with the word \"CPS\"."));
   }
}
