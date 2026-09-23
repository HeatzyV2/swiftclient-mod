package dev.swiftclient.core.hud;

import dev.swiftclient.core.platform.Platform;

public final class Nametag {
   private static final String KEY = "nametag.ownname";
   private static volatile Boolean cached;

   private Nametag() {
   }

   public static boolean showOwnName() {
      Boolean c = cached;
      if (c != null) {
         return c;
      } else {
         boolean v;
         try {
            v = !"0".equals(Platform.game().getConfig("nametag.ownname", "1"));
         } catch (Throwable var3) {
            return true;
         }

         cached = v;
         return v;
      }
   }

   public static void setShowOwnName(boolean on) {
      cached = on;

      try {
         Platform.game().setConfig("nametag.ownname", on ? "1" : "0");
      } catch (Throwable var2) {
      }
   }
}
