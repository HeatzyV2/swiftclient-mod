package dev.swiftclient.ui;

import net.minecraft.client.multiplayer.ClientLevel;

public final class WorldCache {
   private static ClientLevel cached;

   public static void set(ClientLevel w) {
      if (cached == null) {
         cached = w;
      }
   }

   public static ClientLevel get() {
      return cached;
   }

   private WorldCache() {
   }
}
