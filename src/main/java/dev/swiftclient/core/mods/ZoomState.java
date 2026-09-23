package dev.swiftclient.core.mods;

public final class ZoomState {
   private static final double MIN = 5.0;
   private static final double MAX = 110.0;
   private static final double STEP = 5.0;
   private static double target = -1.0;
   public static volatile boolean holding = false;
   private static double depart;
   private static double arrivee;
   private static long debutNanos;
   private static double dureeS = 0.18;
   private static boolean instantane;
   private static boolean prevKeyDown;
   private static boolean toggled;

   private ZoomState() {
   }

   public static void ensureTarget(double settingFov) {
      if (target < 0.0) {
         target = clamp(settingFov);
      }
   }

   public static double target() {
      return target < 0.0 ? 30.0 : target;
   }

   public static void depuis(double fov) {
      depart = fov;
      arrivee = fov;
      debutNanos = System.nanoTime();
   }

   public static void viser(double fov, boolean smooth, double dureeMs) {
      instantane = !smooth;
      dureeS = Math.max(0.01, dureeMs / 1000.0);
      if (!(Math.abs(fov - arrivee) < 1.0E-6)) {
         depart = courant();
         arrivee = fov;
         debutNanos = System.nanoTime();
      }
   }

   public static double courant() {
      if (instantane) {
         return arrivee;
      } else {
         double t = (System.nanoTime() - debutNanos) / 1.0E9 / dureeS;
         if (t >= 1.0) {
            return arrivee;
         } else {
            return t <= 0.0 ? depart : depart + (arrivee - depart) * courbe(t);
         }
      }
   }

   public static boolean arrive() {
      return instantane || (System.nanoTime() - debutNanos) / 1.0E9 >= dureeS;
   }

   private static double courbe(double t) {
      return t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
   }

   public static void update(boolean keyDown, boolean toggleMode) {
      if (toggleMode) {
         if (keyDown && !prevKeyDown) {
            toggled = !toggled;
         }

         holding = toggled;
      } else {
         toggled = false;
         holding = keyDown;
      }

      prevKeyDown = keyDown;
   }

   public static void stop() {
      toggled = false;
      holding = false;
      prevKeyDown = false;
   }

   public static boolean scroll(double vertical) {
      if (holding && vertical != 0.0) {
         target = clamp(target() - Math.signum(vertical) * 5.0);
         return true;
      } else {
         return false;
      }
   }

   private static double clamp(double v) {
      return v < 5.0 ? 5.0 : Math.min(v, 110.0);
   }
}
