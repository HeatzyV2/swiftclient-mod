package dev.swiftclient.core.hud;

/** The Stopwatch HUD timer, driven by its key (start / pause) and its reset key or button. */
public final class Stopwatch {
   private static long accumulated;
   private static long startedAt;
   private static boolean running;

   private Stopwatch() {
   }

   public static synchronized void toggle() {
      long now = System.currentTimeMillis();
      if (running) {
         accumulated += now - startedAt;
         running = false;
      } else {
         startedAt = now;
         running = true;
      }
   }

   public static synchronized void reset() {
      accumulated = 0L;
      running = false;
   }

   public static synchronized boolean running() {
      return running;
   }

   public static synchronized long elapsed() {
      return running ? accumulated + System.currentTimeMillis() - startedAt : accumulated;
   }
}
