package dev.swiftclient.core.mods;

import dev.swiftclient.core.chat.ChatRules;

/**
 * Streamer mode, read by the pieces it affects: the server address and coordinates on the HUD, and private
 * messages in chat. Nothing here knows about the game; it only reads the module's settings.
 */
public final class StreamerMode {
   public static final String MODULE_ID = "streamer";
   public static final String MASK = "••••••";

   private StreamerMode() {
   }

   private static boolean on(String settingId) {
      if (!ModuleManager.isLoaded() || !ModuleManager.active(MODULE_ID)) {
         return false;
      }

      Module m = ModuleManager.byId(MODULE_ID);
      ModuleSetting s = m == null ? null : m.setting(settingId);
      return s != null && s.boolValue();
   }

   /** The server address must not appear on screen. */
   public static boolean hidesAddress() {
      return on("address");
   }

   /** Coordinates must not appear on screen. */
   public static boolean hidesCoords() {
      return on("coords");
   }

   /** Is this chat line a private message that should not reach the screen? */
   public static boolean hidesWhisper(String line) {
      return on("whispers") && ChatRules.isWhisper(line);
   }

   /** What to draw instead of an address or coordinates. */
   public static String mask(String real) {
      return real == null || real.isEmpty() ? real : MASK;
   }
}
