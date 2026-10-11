package dev.swiftclient.core.perf;

import java.util.Arrays;

/**
 * The last few hundred frame times, for the numbers an average hides: the 1% low (what the worst frames
 * feel like) and the 0.1% low. Pure and allocation-free per frame, so it can run on every rendered frame.
 */
public final class FrameWindow {
   private final float[] ms;
   private int count;
   private int head;

   public FrameWindow(int capacity) {
      this.ms = new float[Math.max(16, capacity)];
   }

   /** One rendered frame that took {@code frameMs} milliseconds. Absurd values (a pause, a breakpoint) are dropped. */
   public synchronized void add(double frameMs) {
      if (frameMs > 0.0 && frameMs < 2000.0) {
         this.ms[this.head] = (float)frameMs;
         this.head = (this.head + 1) % this.ms.length;
         this.count = Math.min(this.count + 1, this.ms.length);
      }
   }

   public synchronized void clear() {
      this.count = 0;
      this.head = 0;
   }

   public synchronized int size() {
      return this.count;
   }

   private float[] sorted() {
      float[] copy = Arrays.copyOf(this.ms, this.count);
      Arrays.sort(copy);
      return copy;
   }

   /** Average frames per second over the window, 0 when empty. */
   public synchronized double averageFps() {
      if (this.count == 0) {
         return 0.0;
      }

      double sum = 0.0;
      for (int i = 0; i < this.count; i++) {
         sum += this.ms[i];
      }

      return 1000.0 / (sum / this.count);
   }

   /** FPS of the slowest {@code percent}% of frames (1.0 -> "1% low"). 0 when empty. */
   public synchronized double lowFps(double percent) {
      if (this.count == 0) {
         return 0.0;
      }

      float[] s = this.sorted();
      int from = Math.min(s.length - 1, (int)Math.floor(s.length * (1.0 - percent / 100.0)));
      double sum = 0.0;
      int n = 0;
      for (int i = from; i < s.length; i++) {
         sum += s[i];
         n++;
      }

      return 1000.0 / (sum / n);
   }

   /** Frame times in order, oldest first, at most {@code max} of them. */
   public synchronized float[] recent(int max) {
      int n = Math.min(this.count, Math.max(0, max));
      float[] out = new float[n];
      int start = (this.head - n + this.ms.length * 2) % this.ms.length;
      for (int i = 0; i < n; i++) {
         out[i] = this.ms[(start + i) % this.ms.length];
      }

      return out;
   }
}
