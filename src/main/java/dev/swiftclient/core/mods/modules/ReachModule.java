package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class ReachModule extends Module {
   public ReachModule() {
      super("hud_reach", "Reach Display", "Distance of your last hit. Move it in the HUD Editor.", "HUD", "reach", false);
      this.slider("decimals", "Decimals", 2.0, 0.0, 3.0, 1.0, "").desc("How many digits after the decimal point.");
   }
}
