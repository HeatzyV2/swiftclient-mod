package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class PlaytimeModule extends Module {
   public PlaytimeModule() {
      super("hud_playtime", "Playtime", "How long you have been in this world or server. Move it in the HUD Editor.", "HUD", "clock", false);
      this.toggle("label", "Show label", true).desc("Prefix the time with the word \"Playtime\".");
   }
}
