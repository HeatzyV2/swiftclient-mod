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
}
