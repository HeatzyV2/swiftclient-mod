package dev.swiftclient.core.mods;

public final class BlockOverlayState {
   private BlockOverlayState() {
   }

   private static ModuleSetting s(String id) {
      Module m = ModuleManager.byId("blockoverlay");
      return m == null ? null : m.setting(id);
   }

   public static boolean enabled() {
      return ModuleManager.active("blockoverlay");
   }

   public static boolean hidden() {
      ModuleSetting m = s("mode");
      return m != null && m.cycleIndex() == 1;
   }

   public static int argb() {
      if (hidden()) {
         return 0;
      } else {
         ModuleSetting c = s("color");
         int argb = c == null ? -872415232 : c.colorValue();
         ModuleSetting o = s("opacity");
         if (o != null) {
            int a = (int)Math.round(Math.max(0.0, Math.min(100.0, o.value())) * 2.55);
            argb = argb & 16777215 | a << 24;
         }

         return argb;
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
