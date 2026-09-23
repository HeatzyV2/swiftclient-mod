package dev.swiftclient.core.hud;

public final class HudStats {
   private static long fpsWindowStart;
   private static int framesInWindow;
   private static int fps;
   private static double fpsSmoothed;
   private static double lastX;
   private static double lastY;
   private static double lastZ;
   private static long lastPosNanos;
   private static boolean posInit;
   private static double speedH;
   private static double speedV;
   private static final int CLICK_WINDOW_MS = 1000;
   private static final int MAX_CLICKS = 64;
   private static final long[] LEFT = new long[64];
   private static final long[] RIGHT = new long[64];
   private static int leftCount;
   private static int rightCount;
   private static int prevMask;
   private static int mask;

   private HudStats() {
   }

   public static void update(HudData d) {
      long now = System.nanoTime();
      framesInWindow++;
      if (fpsWindowStart == 0L) {
         fpsWindowStart = now;
      }

      long elapsed = now - fpsWindowStart;
      if (elapsed >= 500000000L) {
         fps = (int)Math.round(framesInWindow / (elapsed / 1.0E9));
         fpsSmoothed = fpsSmoothed <= 0.0 ? fps : fpsSmoothed + (fps - fpsSmoothed) * 0.35;
         framesInWindow = 0;
         fpsWindowStart = now;
      }

      if (d != null) {
         if (!posInit) {
            lastX = d.x();
            lastY = d.y();
            lastZ = d.z();
            lastPosNanos = now;
            posInit = true;
         } else {
            double dt = (now - lastPosNanos) / 1.0E9;
            if (dt >= 0.05) {
               double dx = d.x() - lastX;
               double dy = d.y() - lastY;
               double dz = d.z() - lastZ;
               double h = Math.sqrt(dx * dx + dz * dz) / dt;
               double v = Math.abs(dy) / dt;
               speedH = speedH + (h - speedH) * 0.4;
               speedV = speedV + (v - speedV) * 0.4;
               lastX = d.x();
               lastY = d.y();
               lastZ = d.z();
               lastPosNanos = now;
            }
         }

         mask = d.inputMask();
         long ms = System.currentTimeMillis();
         if ((mask & 128) != 0 && (prevMask & 128) == 0) {
            push(LEFT, ms, true);
         }

         if ((mask & 256) != 0 && (prevMask & 256) == 0) {
            push(RIGHT, ms, false);
         }

         prevMask = mask;
         leftCount = prune(LEFT, leftCount, ms);
         rightCount = prune(RIGHT, rightCount, ms);
      }
   }

   private static void push(long[] buf, long ms, boolean left) {
      int n = left ? leftCount : rightCount;
      if (n >= 64) {
         System.arraycopy(buf, 1, buf, 0, 63);
         n = 63;
      }

      buf[n] = ms;
      if (left) {
         leftCount = n + 1;
      } else {
         rightCount = n + 1;
      }
   }

   private static int prune(long[] buf, int count, long ms) {
      int keep = 0;

      for (int i = 0; i < count; i++) {
         if (ms - buf[i] <= 1000L) {
            buf[keep++] = buf[i];
         }
      }

      return keep;
   }

   public static int fps() {
      return fps;
   }

   public static int fpsSmoothed() {
      return (int)Math.round(fpsSmoothed <= 0.0 ? fps : fpsSmoothed);
   }

   public static double speedHorizontal() {
      return speedH;
   }

   public static double speedTotal() {
      return Math.sqrt(speedH * speedH + speedV * speedV);
   }

   public static int leftCps() {
      return leftCount;
   }

   public static int rightCps() {
      return rightCount;
   }

   public static int inputMask() {
      return mask;
   }

   public static boolean pressed(int bit) {
      return (mask & bit) != 0;
   }

   public static long usedMemoryMb() {
      Runtime r = Runtime.getRuntime();
      return (r.totalMemory() - r.freeMemory()) / 1048576L;
   }

   public static long maxMemoryMb() {
      return Runtime.getRuntime().maxMemory() / 1048576L;
   }

   public static int memoryPercent() {
      long max = maxMemoryMb();
      return max <= 0L ? 0 : (int)Math.round(usedMemoryMb() * 100.0 / max);
   }
}
