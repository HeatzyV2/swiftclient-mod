package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class ComboModule extends Module {
   public ComboModule() {
      super("hud_combo", "Combo Counter", "Hits landed in a row without being hit back. Move it in the HUD Editor.", "HUD", "combo", false);
      this.toggle("hide_zero", "Hide without combo", false).desc("Only show the counter once you land a hit.");
      this.color("color", "Highlight color", -12877066).desc("Colour of the counter from 3 hits in a row.");
   }
}
