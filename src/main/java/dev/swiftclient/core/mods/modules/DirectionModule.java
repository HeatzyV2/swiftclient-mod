package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class DirectionModule extends Module {
   public DirectionModule() {
      super("hud_direction", "Direction HUD", "A compass strip showing where you are heading. Move it in the HUD Editor.", "HUD", "compass", false);
      this.toggle("degrees", "Show degrees", true).desc("Write the exact heading under the strip.");
      this.toggle("background", "Show background", true).desc("Draw a dark panel behind the strip.");
      this.color("color", "Marker color", -12877066).desc("Colour of the centre mark.");
   }
}
