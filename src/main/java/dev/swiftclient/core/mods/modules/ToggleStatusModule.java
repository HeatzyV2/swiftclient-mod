package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class ToggleStatusModule extends Module {
   public ToggleStatusModule() {
      super("hud_togglestatus", "Toggle Sneak/Sprint", "Shows when you are sprinting or sneaking, and whether it is toggled. Move it in the HUD Editor.", "HUD", "togglesprint", false);
      this.color("color", "Text color", -1).desc("Colour of the status text.");
   }
}
