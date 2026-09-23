package dev.swiftclient.ui;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess.Frozen;

public final class RegistryCache {
   private static Frozen cachedRegistries;
   private static ClientPacketListener cachedHandler;

   public static void set(Frozen r, ClientPacketListener h) {
      if (cachedRegistries == null && r != null) {
         cachedRegistries = r;
      }

      if (cachedHandler == null && h != null) {
         cachedHandler = h;
      }
   }

   public static Frozen get() {
      return cachedRegistries;
   }

   public static ClientPacketListener getHandler() {
      return cachedHandler;
   }

   private RegistryCache() {
   }
}
