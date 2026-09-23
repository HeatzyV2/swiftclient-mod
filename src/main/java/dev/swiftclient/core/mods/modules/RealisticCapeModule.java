package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class RealisticCapeModule extends Module {
   public RealisticCapeModule() {
      super("realisticcape", "Realistic Cape", "Effet de vague continu sur la cape.", "Render", "cape", false);
      this.settings
         .add(ModuleSetting.slider("realisticcape", "amplitude", "Amplitude", 8.0, 2.0, 20.0, 1.0, "°").desc("How far the cape swings away from your back."));
      this.settings.add(ModuleSetting.slider("realisticcape", "speed", "Speed", 50.0, 10.0, 100.0, 5.0, "%").desc("How fast the wave travels down the cape."));
   }
}
