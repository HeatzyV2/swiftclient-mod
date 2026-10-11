package dev.swiftclient.core.session;

/**
 * FPS and ping sampled every few seconds while you play, kept for the Session page graph. Memory only:
 * it starts empty with every game launch.
 */
public final class SessionGraph {
   public static final int CAPACITY = 180;
   private static final long EVERY_MS = 10_000L;
   private static final float[] FPS = new float[CAPACITY];
   private static final float[] PING = new float[CAPACITY];
   private static int count;
   private static int head;
   private static long lastMs;

   private SessionGraph() {
   }

   /** Called every frame; records one point every ten seconds while in a world. */
   public static synchronized void sample(long nowMs, boolean inWorld, double fps, int ping) {
      if (inWorld && fps > 0.0 && nowMs - lastMs >= EVERY_MS) {
         lastMs = nowMs;
         FPS[head] = (float)fps;
         PING[head] = ping;
         head = (head + 1) % CAPACITY;
         count = Math.min(count + 1, CAPACITY);
      }
   }

   public static synchronized int size() {
      return count;
   }

   public static synchronized float[] fps() {
      return copy(FPS);
   }

   /** Ping history, -1 where the ping was unknown. */
   public static synchronized float[] ping() {
      return copy(PING);
   }

   private static float[] copy(float[] src) {
      float[] out = new float[count];
      int start = (head - count + CAPACITY * 2) % CAPACITY;
      for (int i = 0; i < count; i++) {
         out[i] = src[(start + i) % CAPACITY];
      }

      return out;
   }

   static synchronized void resetForTests() {
      count = 0;
      head = 0;
      lastMs = 0L;
   }
}
