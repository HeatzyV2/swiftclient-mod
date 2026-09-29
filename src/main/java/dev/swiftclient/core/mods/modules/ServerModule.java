package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class ServerModule extends Module {
   public ServerModule() {
      super("hud_server", "Server Address", "The address of the server you are on. Move it in the HUD Editor.", "HUD", "globe", false);
      this.toggle("icon", "Show icon", true).desc("Draw a globe before the address.");
      this.toggle("singleplayer", "Show in singleplayer", true).desc("Write \"Singleplayer\" when you are not on a server.");
   }
}
