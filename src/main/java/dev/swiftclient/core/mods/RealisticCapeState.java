package dev.swiftclient.core.mods;

public final class RealisticCapeState {
   public static volatile boolean active = false;
   public static final int SEGMENTS = 16;

   private RealisticCapeState() {
   }

   /** Set by RealisticCapeModule every tick; read by the cape renderer. */
   private static volatile double speedPercent = 50.0;
   private static volatile double amplitude = 8.0;

   public static void configure(boolean enabled, double speedSetting, double amplitudeSetting) {
      active = enabled;
      speedPercent = speedSetting;
      amplitude = amplitudeSetting;
   }

   private static double speed() {
      return speedPercent / 100.0;
   }

   private static double amplitudeDeg() {
      return amplitude;
   }

   private static double wave(float phase) {
      double period = 2600.0 / Math.max(0.1, speed());
      double t = System.currentTimeMillis() % (long)period / period;
      return Math.sin(t * Math.PI * 2.0 + phase);
   }

   public static float flapOffset(float phase) {
      double deg = amplitudeDeg() * wave(phase);
      return (float)Math.toRadians(deg);
   }

   public static float twist(float phase) {
      double deg = amplitudeDeg() * 0.35 * wave(phase + 1.7F);
      return (float)Math.toRadians(deg);
   }

   private static double travel() {
      double period = 2600.0 / Math.max(0.1, speed());
      return System.currentTimeMillis() % (long)period / period * Math.PI * 2.0;
   }

   public static float segmentPitch(int i, float extra) {
      double perJoint = Math.toRadians(amplitudeDeg() * 0.5);
      double k = 0.85;
      double growth = 0.35 + 0.65 * (i / 15.0);
      return (float)(perJoint * growth * Math.sin(travel() + extra - i * k));
   }

   public static float rowPitch(int row, int rows, float extra) {
      double amp = Math.toRadians(amplitudeDeg()) * 0.3;
      double growth = 0.3 + 0.7 * ((double)row / Math.max(1, rows - 1));
      double k = (Math.PI * 5) / rows;
      return (float)(amp * growth * Math.sin(travel() + extra - row * k));
   }
}
