package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;

/** Shows the world at the time of day you choose. Only on your screen: the server keeps its own time. */
@ConsumedBy("ClientClockMixin")
public final class TimeChangerModule extends Module {
   public final ModuleSetting hour;
   public final ModuleSetting frozen;

   public TimeChangerModule() {
      super("time_changer", "Time Changer", "Shows the world at the time you choose. Only on your screen: the server keeps its own time.", "Visual", "sunrise", false);
      this.hour = this.slider("hour", "Hour", 12.0, 0.0, 23.5, 0.5, "h").desc("Time of day to show: 6h sunrise, 12h noon, 18h sunset, 0h midnight.");
      this.frozen = this.toggle("frozen", "Frozen", true).desc("Stay at that hour. Off: the day goes on from there at normal speed.");
   }

   /** Clock ticks shown instead of the server's, same day number (moon phase kept). */
   public static long override(long serverTicks) {
      if (!ModuleManager.isLoaded()) {
         return serverTicks;
      }

      TimeChangerModule m = ModuleManager.get(TimeChangerModule.class);
      if (!m.isEnabled()) {
         return serverTicks;
      }

      // Minecraft day starts at 6h: tick 0 = sunrise, 6000 = noon.
      long target = Math.round((m.hour.value() - 6.0) * 1000.0);
      target = Math.floorMod(target, 24000L);
      long day = Math.floorDiv(serverTicks, 24000L);
      if (m.frozen.boolValue()) {
         return day * 24000L + target;
      } else {
         return serverTicks + (target - Math.floorMod(m.anchor(serverTicks), 24000L));
      }
   }

   private long anchorTicks = Long.MIN_VALUE;

   /** Server time when the hour was chosen, so an unfrozen day moves on from the chosen hour. */
   private long anchor(long serverTicks) {
      if (this.anchorTicks == Long.MIN_VALUE) {
         this.anchorTicks = serverTicks;
      }

      return this.anchorTicks;
   }

   @Override
   protected void onDisable() {
      this.anchorTicks = Long.MIN_VALUE;
   }
}
