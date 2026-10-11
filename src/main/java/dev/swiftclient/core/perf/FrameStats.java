package dev.swiftclient.core.perf;

/** The live frame window plus the advice derived from it. Fed by HudStats once per rendered frame. */
public final class FrameStats {
   public static final FrameWindow WINDOW = new FrameWindow(600);
   private static long lastNanos;

   private FrameStats() {
   }

   /** Call once per rendered frame. */
   public static void frame(long nowNanos) {
      if (lastNanos != 0L) {
         WINDOW.add((nowNanos - lastNanos) / 1.0E6);
      }

      lastNanos = nowNanos;
   }

   /** What to tell the player: a key under {@code swift.perf.advice.}. */
   public enum Advice {
      LOW_FPS("low_fps"),
      STUTTER("stutter"),
      FAR_VIEW("far_view");

      public final String key;

      Advice(String key) {
         this.key = key;
      }
   }

   /**
    * @param avg average FPS over the window
    * @param low1 1% low FPS
    * @param renderDistance the player's render distance in chunks
    * @return the advice, or null when all is well
    */
   public static Advice advise(double avg, double low1, int renderDistance) {
      if (avg <= 0.0) {
         return null;
      } else if (avg < 55.0 && renderDistance > 10) {
         return Advice.FAR_VIEW;
      } else if (avg < 55.0) {
         return Advice.LOW_FPS;
      } else if (low1 < avg * 0.5 && low1 < 70.0) {
         return Advice.STUTTER;
      }

      return null;
   }
}
