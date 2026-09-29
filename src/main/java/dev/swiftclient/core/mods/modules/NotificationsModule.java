package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class NotificationsModule extends Module {
   public NotificationsModule() {
      super("hud_notifications", "Notifications", "Swift toasts: friends coming online, screenshots saved... Move them in the HUD Editor.", "Interface", "bell", false);
      this.slider("duration", "Display time", 4.0, 2.0, 10.0, 1.0, "s").desc("How long a notification stays on screen.");
      this.toggle("friends", "Friends online", true).desc("Tell you when one of your Swift friends comes online.");
      this.toggle("screenshots", "Screenshots", true).desc("Confirm every screenshot you take.");
   }
}
