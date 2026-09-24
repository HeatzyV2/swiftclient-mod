package dev.swiftclient.core.cosmetics;

/**
 * Exponential backoff gate: after each consecutive failure the next attempt is pushed back
 * (base, 2x base, 4x base ... capped). A success resets it.
 */
public final class Backoff {
   private final String name;
   private final long baseMs;
   private final long capMs;
   private int failures;
   private long nextAllowedAt;

   public Backoff(String name, long baseMs, long capMs) {
      this.name = name;
      this.baseMs = baseMs;
      this.capMs = capMs;
   }

   public synchronized boolean blocked() {
      return System.currentTimeMillis() < this.nextAllowedAt;
   }

   public synchronized void success() {
      if (this.failures > 0) {
         System.out.println("[SwiftClient] " + this.name + " : OK apres " + this.failures + " echec(s)");
      }

      this.failures = 0;
      this.nextAllowedAt = 0L;
   }

   public synchronized void failure(String why) {
      this.failures++;
      long delay = this.baseMs << Math.min(this.failures - 1, 20);
      if (delay <= 0L || delay > this.capMs) {
         delay = this.capMs;
      }

      this.nextAllowedAt = System.currentTimeMillis() + delay;
      System.out.println("[SwiftClient] " + this.name + " : echec #" + this.failures + " (" + why + "), nouvel essai dans " + delay / 1000L + " s");
   }

   public synchronized int failures() {
      return this.failures;
   }
}
