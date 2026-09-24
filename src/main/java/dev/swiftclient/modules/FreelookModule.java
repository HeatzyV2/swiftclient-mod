package dev.swiftclient.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.freelook.Freelook;
import dev.swiftclient.input.SwiftKeys;
import net.minecraft.client.Minecraft;

public final class FreelookModule extends Module {
   public final ModuleSetting maxYaw;

   public FreelookModule() {
      super("freelook", "Freelook", "Hold a key to look around without turning your player.", "Render", "gear", false);
      this.action("controls", "Freelook key", () -> SwiftKeys.label(SwiftKeys.FREELOOK), SwiftKeys::openControls)
         .desc("Change it in Options > Controls > Key Binds, under Swift Client.");
      this.maxYaw = this.slider("maxyaw", "Max angle", 360.0, 90.0, 360.0, 15.0, "°");
   }

   @Override
   protected void onTick() {
      Freelook.tick(Minecraft.getInstance(), (float)this.maxYaw.value());
   }

   @Override
   protected void onDisable() {
      if (Freelook.active) {
         Freelook.stop(Minecraft.getInstance());
      }
   }
}
