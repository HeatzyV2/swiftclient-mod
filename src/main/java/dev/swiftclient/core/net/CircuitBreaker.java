package dev.swiftclient.core.net;

import dev.swiftclient.core.log.Log;
import org.slf4j.Logger;

/**
 * Stops calling a service that keeps failing. Closed: calls go through. After {@code threshold}
 * consecutive failures it opens and refuses calls for a delay that doubles on each new opening (capped).
 * When the delay is over, one trial call is let through (half-open): success closes it, failure reopens it.
 */
public final class CircuitBreaker {
   private static final Logger LOG = Log.get("Net");
   private final String name;
   private final int threshold;
   private final long baseMs;
   private final long capMs;
   private int failures;
   private int openings;
   private long openUntil;
   private boolean trialInFlight;

   public CircuitBreaker(String name, int threshold, long baseMs, long capMs) {
      this.name = name;
      this.threshold = Math.max(1, threshold);
      this.baseMs = baseMs;
      this.capMs = capMs;
   }

   /** True if a call may be made now. In half-open state only the first caller gets through. */
   public synchronized boolean allow() {
      if (this.failures < this.threshold) {
         return true;
      } else if (System.currentTimeMillis() < this.openUntil || this.trialInFlight) {
         return false;
      } else {
         this.trialInFlight = true;
         return true;
      }
   }

   /** Whether calls are currently refused (does not consume the half-open trial). */
   public synchronized boolean isOpen() {
      return this.failures >= this.threshold && (System.currentTimeMillis() < this.openUntil || this.trialInFlight);
   }

   /** Gives back a permit from {@link #allow()} without a verdict (the call was not made). */
   public synchronized void release() {
      this.trialInFlight = false;
   }

   public synchronized void success() {
      if (this.failures >= this.threshold) {
         LOG.info("{} : de nouveau disponible", this.name);
      }

      this.failures = 0;
      this.openings = 0;
      this.trialInFlight = false;
   }

   public synchronized void failure(String why) {
      this.trialInFlight = false;
      this.failures++;
      if (this.failures >= this.threshold) {
         long delay = this.baseMs << Math.min(this.openings, 20);
         if (delay <= 0L || delay > this.capMs) {
            delay = this.capMs;
         }

         this.openings++;
         this.openUntil = System.currentTimeMillis() + delay;
         LOG.warn("{} : indisponible ({}), nouvel essai dans {} s", this.name, why, delay / 1000L);
      } else {
         LOG.debug("{} : echec {}/{} ({})", this.name, this.failures, this.threshold, why);
      }
   }

   public synchronized int openings() {
      return this.openings;
   }
}
