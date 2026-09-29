package dev.swiftclient.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import net.minecraft.client.Minecraft;

/** Choose the weather on your screen: clear, rain or thunderstorm. The server keeps its own weather. */
public final class WeatherChangerModule extends Module {
   public final ModuleSetting weather;

   public WeatherChangerModule() {
      super("weather_changer", "Weather Changer", "Choose the weather you see: clear, rain (snow in cold biomes) or thunderstorm. The server keeps its own.", "Visual", "norain", false);
      this.weather = this.cycle("weather", "Weather", new String[]{"Clear", "Rain", "Thunderstorm"}, 0).desc("What you see, whatever the server weather is.");
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         int w = this.weather.cycleIndex();
         mc.level.setRainLevel(w == 0 ? 0.0F : 1.0F);
         mc.level.setThunderLevel(w == 2 ? 1.0F : 0.0F);
      }
   }
}
