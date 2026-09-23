package dev.swiftclient.core.mods;

public final class TabAnimState {
   private static long shownStart = 0L;
   private static long lastFrame = 0L;
   private static final float DIST = 22.0F;
   private static final double DURATION = 170.0;

   private TabAnimState() {
   }

   public static float slideY() {
      long now = System.currentTimeMillis();
      if (now - lastFrame > 120L) {
         shownStart = now;
      }

      lastFrame = now;
      float t = (float)Math.min(1.0, (now - shownStart) / 170.0);
      float eased = t * t * (3.0F - 2.0F * t);
      return -(1.0F - eased) * 22.0F;
   }
}
