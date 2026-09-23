package dev.swiftclient.core.ui;

/** Title-screen entrance timeline — Swift In signature. */
public final class SwiftIntro {
   private static long startMs;
   private static boolean whooshPlayed;
   private static boolean active;

   private SwiftIntro() {
   }

   public static void start() {
      startMs = System.currentTimeMillis();
      whooshPlayed = false;
      active = true;
   }

   public static void clear() {
      active = false;
   }

   public static boolean active() {
      return active;
   }

   public static float seconds() {
      if (!active) {
         return 10.0F;
      }
      return (System.currentTimeMillis() - startMs) / 1000.0F;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static float easeOut(float t) {
      t = clamp01(t);
      return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
   }

   /** Logo streak slide 0→1 over first ~0.35s. */
   public static float logoProgress() {
      return easeOut(seconds() / 0.35F);
   }

   public static float logoAlpha() {
      return clamp01(seconds() / 0.18F);
   }

   /** Horizontal offset in px (negative = from the left). */
   public static int logoOffsetX() {
      return Math.round((1.0F - logoProgress()) * -90.0F);
   }

   public static float brandAlpha() {
      return easeOut((seconds() - 0.18F) / 0.28F);
   }

   public static float rowAlpha(int index) {
      float start = 0.32F + index * 0.055F;
      return easeOut((seconds() - start) / 0.22F);
   }

   public static boolean rowReady(int index) {
      return rowAlpha(index) > 0.85F;
   }

   public static float streakAlpha() {
      float s = seconds();
      if (s > 0.45F) {
         return 0.0F;
      }
      return clamp01(1.0F - s / 0.45F) * logoAlpha();
   }

   public static void tickSounds() {
      if (!active || whooshPlayed) {
         return;
      }
      if (seconds() >= 0.04F) {
         whooshPlayed = true;
         SwiftSounds.whoosh();
      }
   }
}
