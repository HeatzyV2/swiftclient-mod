package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

@ConsumedBy({"HudManager"})
public final class PerfModule extends Module {
   public PerfModule() {
      super("hud_perf", "Performance", "Average FPS, 1% lows, a frame-time graph and a tip when it stutters. Move it in the HUD Editor.", "HUD", "gauge", false);
      this.settings.add(ModuleSetting.toggle("hud_perf", "lows", "Show 1% lows", true).desc("The slowest 1% and 0.1% of frames: they show stutter that the average hides."));
      this.settings.add(ModuleSetting.toggle("hud_perf", "graph", "Frame-time graph", true).desc("One bar per frame, tall = slow. The thin line is 60 FPS."));
      this.settings.add(ModuleSetting.toggle("hud_perf", "advice", "Tips", true).desc("One line of advice when the game runs below 55 FPS or stutters."));
   }
}
