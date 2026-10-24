package dev.swiftclient.streamonly;

import net.minecraft.world.item.ItemStack;

/** Stream Only needs the 26.2+ render API: on older versions the overlay is never usable. */
public final class StreamOverlay {
   private StreamOverlay() {
   }

   public static boolean usable() {
      return false;
   }

   public static void retry() {
   }

   public static void stop() {
   }

   public static void hotbar(int slot, int x, int y, ItemStack item) {
   }

   public static void container(int slot, int x, int y, ItemStack item, boolean isHovered) {
   }

   public static void carried(ItemStack item, int x, int y) {
   }

   public static void selection(int x, int y) {
   }

   public static void tooltip(ItemStack item, int mouseX, int mouseY) {
   }

   public static void itemName(ItemStack item, int alpha, int y) {
   }

   public static void endFrame() {
   }
}
