package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class ScoreboardModule extends Module {
   public ScoreboardModule() {
      super("hud_scoreboard", "Scoreboard", "Move, resize and restyle the server scoreboard. Move it in the HUD Editor.", "HUD", "list", false);
      this.settings
         .add(ModuleSetting.toggle("hud_scoreboard", "numbers", "Show score numbers", true).desc("Show the red score numbers on the right of each line."));
      this.settings.add(ModuleSetting.toggle("hud_scoreboard", "title", "Show title", true).desc("Show the server's scoreboard title."));
      this.settings.add(ModuleSetting.toggle("hud_scoreboard", "background", "Show background", true).desc("Draw a panel behind the lines."));
      this.settings
         .add(
            ModuleSetting.color("hud_scoreboard", "bg_color", "Background color", 1711276032)
               .desc("Colour of that panel. The alpha sets how much of the world shows through.")
         );
   }
}
