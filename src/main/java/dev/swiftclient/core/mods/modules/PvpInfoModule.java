package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class PvpInfoModule extends Module {
   public PvpInfoModule() {
      super("hud_pvpinfo", "PvP Info", "Card about who you are fighting: head, health, armour and distance. Move it in the HUD Editor.", "HUD", "sword", false);
      this.toggle("distance", "Show distance", true).desc("Write how far your opponent is.");
      this.toggle("armor", "Show armor", true).desc("Write the armour points of your opponent.");
   }
}
