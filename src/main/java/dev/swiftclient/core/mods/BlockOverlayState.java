package dev.swiftclient.core.mods;

import dev.swiftclient.core.mods.modules.BlockOverlayModule;

public final class BlockOverlayState {
   private BlockOverlayState() {
   }

   private static BlockOverlayModule module() {
      return ModuleManager.get(BlockOverlayModule.class);
   }

   public static boolean enabled() {
      return module().isEnabled();
   }

   public static boolean hidden() {
      return module().mode.cycleIndex() == BlockOverlayModule.MODE_HIDDEN;
   }

   public static int argb() {
      if (hidden()) {
         return 0;
      } else {
         BlockOverlayModule m = module();
         int a = (int)Math.round(Math.max(0.0, Math.min(100.0, m.opacity.value())) * 2.55);
         return m.color.colorValue() & 0xFFFFFF | a << 24;
      }
   }

   public static float red(int argb) {
      return (argb >> 16 & 0xFF) / 255.0F;
   }

   public static float green(int argb) {
      return (argb >> 8 & 0xFF) / 255.0F;
   }

   public static float blue(int argb) {
      return (argb & 0xFF) / 255.0F;
   }

   public static float alpha(int argb) {
      return (argb >>> 24 & 0xFF) / 255.0F;
   }
}
