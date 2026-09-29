package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class PingModule extends Module {
   public PingModule() {
      super("hud_ping", "Ping", "Your latency to the server. Move it in the HUD Editor.", "HUD", "signal", false);
      this.toggle("label", "Show \"ms\"", true).desc("Write the unit after the number.");
      this.toggle("colored", "Color by latency", true).desc("Green under 80 ms, orange up to 160 ms, red above.");
      this.toggle("singleplayer", "Show in singleplayer", false).desc("Keep the element on screen even when there is no server to measure.");
   }
}
