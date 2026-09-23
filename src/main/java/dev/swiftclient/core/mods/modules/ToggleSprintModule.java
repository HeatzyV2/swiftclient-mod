package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class ToggleSprintModule extends Module {
   public ToggleSprintModule() {
      super("togglesprint", "Toggle Sprint", "Sprinte automatiquement en avancant.", "Movement", "togglesprint", false);
      this.settings
         .add(
            ModuleSetting.toggle("togglesprint", "backwards", "Sprint backwards", false)
               .desc("Keep sprinting when you walk backwards, which vanilla never does.")
         );
   }
}
