package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager", "WorldOverlays"})
public final class TntCountdownModule extends Module {
   public TntCountdownModule() {
      super("hud_tnt", "TNT Countdown", "Fuse time of lit TNT, on screen and above the TNT itself. Move it in the HUD Editor.", "PvP", "bomb", false);
      this.toggle("world", "Above the TNT", true).desc("Write the seconds left above every lit TNT near you.");
   }
}
