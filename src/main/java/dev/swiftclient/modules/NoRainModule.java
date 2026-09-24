package dev.swiftclient.modules;

import dev.swiftclient.core.mods.Module;
import net.minecraft.client.Minecraft;

public final class NoRainModule extends Module {
   public NoRainModule() {
      super("norain", "No Rain", "Hides rain, snow and thunderstorms on your screen. The server keeps its weather.", "Render", "norain", false);
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         mc.level.setRainLevel(0.0F);
         mc.level.setThunderLevel(0.0F);
      }
   }
}
