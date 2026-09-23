package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class FreelookModule extends Module {
   public FreelookModule() {
      super("freelook", "Freelook", "Hold a key to look around without turning your player.", "Render", "gear", false);
      this.settings.add(ModuleSetting.key("freelook", "key", "Key", 342));
      this.settings.add(ModuleSetting.slider("freelook", "maxyaw", "Max angle", 360.0, 90.0, 360.0, 15.0, "°"));
   }
}
