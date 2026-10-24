package dev.swiftclient.modules;

import dev.swiftclient.compat.Mc;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.ui.Notifications;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

/** A notification when it starts (or stops) raining or thundering where you are. */
public final class WeatherNotifierModule extends Module {
   /** The weather must hold this many ticks before it counts, so a flicker on join does not notify. */
   private static final int STABLE_TICKS = 20;
   public final ModuleSetting thunder;
   public final ModuleSetting clear;
   private ClientLevel level;
   private int weather;
   private int changedTicks;

   public WeatherNotifierModule() {
      super("weather_notifier", "Weather Notifier", "Tells you when it starts raining or thundering, so you are never caught off guard.", "World", "cloud", false);
      this.thunder = this.toggle("thunder", "Thunderstorms", true).desc("Also tell you when a thunderstorm starts (mobs can spawn in daylight).");
      this.clear = this.toggle("clear", "Clear sky", false).desc("Also tell you when the weather clears up.");
   }

   @Override
   protected void onEnable() {
      this.level = null;
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel current = mc.level;
      if (current != this.level) {
         this.level = current;
         this.weather = current == null ? 0 : weatherOf(current);
         this.changedTicks = 0;
      }

      if (current == null || Mc.screen() instanceof LevelLoadingScreen) {
         this.changedTicks = 0;
         return;
      }

      int now = weatherOf(current);
      if (now == this.weather) {
         this.changedTicks = 0;
      } else if (++this.changedTicks >= STABLE_TICKS) {
         int before = this.weather;
         this.weather = now;
         this.changedTicks = 0;
         if (now == 2 && this.thunder.boolValue()) {
            notify(mc, "Thunderstorm!", "A thunderstorm started.");
         } else if (now == 1 && before == 0) {
            notify(mc, "Weather changed", "It started raining.");
         } else if (now == 0 && this.clear.boolValue()) {
            notify(mc, "Weather changed", "The sky is clear again.");
         }
      }
   }

   /** 0 clear, 1 rain, 2 thunder. */
   private static int weatherOf(ClientLevel level) {
      return level.isThundering() ? 2 : (level.isRaining() ? 1 : 0);
   }

   /** Swift toast when the Notifications element is on, vanilla toast otherwise. */
   private static void notify(Minecraft mc, String title, String body) {
      if (ModuleManager.active(Notifications.MODULE_ID)) {
         Notifications.push(title, body, "cloud");
      } else {
         SystemToast.add(Mc.toasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal(title), Component.literal(body));
      }
   }
}
