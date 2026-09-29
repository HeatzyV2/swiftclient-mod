package dev.swiftclient.modules;

import dev.swiftclient.core.hud.Stopwatch;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.input.SwiftKeys;

/** Stopwatch on the HUD, started and paused with a key. */
public final class StopwatchModule extends Module {
   public StopwatchModule() {
      super("hud_stopwatch", "Stopwatch", "A stopwatch on your HUD, started and paused with a key. Move it in the HUD Editor.", "HUD", "timer", false);
      this.action("key", "Start / pause key", () -> SwiftKeys.label(SwiftKeys.STOPWATCH), SwiftKeys::openControls)
         .desc("Change it in Options > Controls > Key Binds, under Swift Client.");
      this.action("reset_key", "Reset key", () -> SwiftKeys.label(SwiftKeys.STOPWATCH_RESET), SwiftKeys::openControls)
         .desc("Optional key that puts the stopwatch back to zero.");
      this.action("reset", "Reset", () -> Tr.of("swift.stopwatch.reset"), Stopwatch::reset).desc("Put the stopwatch back to zero now.");
      this.toggle("millis", "Hundredths", true).desc("Show hundredths of a second.");
   }

   @Override
   protected void onTick() {
      while (SwiftKeys.STOPWATCH.consumeClick()) {
         Stopwatch.toggle();
      }

      while (SwiftKeys.STOPWATCH_RESET.consumeClick()) {
         Stopwatch.reset();
      }
   }
}
