package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.session.SessionStats;

/** Keeps the session statistics (time in a world, distance walked) shown in the Session page of the menu. */
public final class SessionModule extends Module {
   public SessionModule() {
      super("session", "Session stats", "Counts your time in worlds and the distance you walk. See it in the Session page of this menu.", "Interface", "activity", true);
   }

   @Override
   protected void onRender(float partialTick) {
      dev.swiftclient.core.hud.HudData d = HudManager.lastData();
      SessionStats.sample(d);
      dev.swiftclient.core.session.SessionGraph.sample(
         System.currentTimeMillis(), d != null && d.inWorld(), dev.swiftclient.core.hud.HudStats.fpsSmoothed(), d == null ? -1 : d.ping()
      );
   }
}
