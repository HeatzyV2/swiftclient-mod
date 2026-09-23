package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class MemoryModule extends Module {
   public MemoryModule() {
      super("hud_memory", "Memory", "Used memory. Move it in the HUD Editor.", "HUD", "chip", false);
      this.settings.add(ModuleSetting.toggle("hud_memory", "percent", "Show percentage", true).desc("Show the share of the allocated memory that is in use."));
      this.settings.add(ModuleSetting.toggle("hud_memory", "max", "Show allocated", true).desc("Also show how much memory the game was given in total."));
      this.settings.add(ModuleSetting.toggle("hud_memory", "bar", "Usage bar", true).desc("Draw a fill bar under the numbers."));
      this.settings.add(ModuleSetting.toggle("hud_memory", "colored", "Color by usage", true).desc("Turn orange then red as the memory fills up."));
   }
}
