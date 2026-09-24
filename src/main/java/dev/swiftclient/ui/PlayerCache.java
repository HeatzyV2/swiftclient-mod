package dev.swiftclient.ui;

import net.minecraft.world.entity.LivingEntity;

public class PlayerCache {
   private static LivingEntity cached;

   public static void set(LivingEntity player) {
      if (player != null) {
         cached = player;
      }
   }

   public static LivingEntity get() {
      return cached;
   }

   /** Called on disconnect: the cached player would otherwise keep its whole ClientLevel alive. */
   public static void clear() {
      cached = null;
   }
}
