package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.screen.FriendsScreen;
import dev.swiftclient.core.social.Friends;
import dev.swiftclient.core.ui.ScreenRequest;

/** Your Swift friends (the same list as in the launcher): see them in game, get told when they come online. */
public final class FriendsModule extends Module {
   public FriendsModule() {
      super("friends", "Friends", "Your Swift friends, shared with the launcher: find them in game and know when they come online.", "Interface", "users", true);
      this.action("manage", "Friends list", () -> Tr.of("swift.common.open"), () -> ScreenRequest.open(new FriendsScreen()))
         .desc("Add or remove friends. The list is the same as in the launcher.");
      this.toggle("glow", "Outline in world", true).desc("Draw an outline around your friends, even through walls.");
      this.toggle("tab_star", "Star in tab list", true).desc("Put a gold star in front of your friends in the player list.");
      this.toggle("notify", "Online alerts", true).desc("Show a notification when a friend comes online (needs the Notifications module).");
   }

   @Override
   protected void onTick() {
      Friends.tick();
   }
}
