package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

@ConsumedBy({"HudManager"})
public final class FpsModule extends Module {
   public FpsModule() {
      super("hud_fps", "FPS", "Frames per second. Move it in the HUD Editor.", "HUD", "activity", false);
      this.settings.add(ModuleSetting.toggle("hud_fps", "label", "Show \"FPS\" label", true).desc("Prefix the number with the word \"FPS\"."));
      this.settings
         .add(
            ModuleSetting.toggle("hud_fps", "smooth", "Smoothed value", true).desc("Average the value over a second. The raw counter jumps too much to read.")
         );
      this.settings.add(ModuleSetting.toggle("hud_fps", "colored", "Color by performance", true).desc("Green above 60, orange in the middle, red below 30."));
   }
}
