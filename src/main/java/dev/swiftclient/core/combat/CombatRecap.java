package dev.swiftclient.core.combat;

import java.util.function.LongSupplier;

/**
 * Sums up a fight: it starts with the first swing or hit, ends after a few quiet seconds, and leaves a
 * {@link Summary} behind. The clock is injected so the rules can be tested without waiting.
 */
public final class CombatRecap {
   /** Milliseconds of silence after which a fight is over. */
   public static final long IDLE_MS = 8000L;

   public record Summary(long durationMs, int swings, int hits, int hurt, double damageTaken, int bestCombo) {
      /** Share of your attacks that landed, 0-100. */
      public int accuracy() {
         return this.swings == 0 ? 0 : (int)Math.round(100.0 * this.hits / this.swings);
      }

      public double hitsPerSecond() {
         return this.durationMs <= 0L ? 0.0 : this.hits / (this.durationMs / 1000.0);
      }

      /** Worth showing: at least a few blows exchanged. */
      public boolean meaningful() {
         return this.swings + this.hurt >= 3;
      }
   }

   private final LongSupplier clock;
   private boolean active;
   private long startMs;
   private long lastMs;
   private int swings;
   private int hits;
   private int hurt;
   private double damage;
   private int bestCombo;
   private Summary last;

   public CombatRecap(LongSupplier clock) {
      this.clock = clock;
   }

   private void touch() {
      long now = this.clock.getAsLong();
      if (!this.active) {
         this.active = true;
         this.startMs = now;
         this.swings = 0;
         this.hits = 0;
         this.hurt = 0;
         this.damage = 0.0;
         this.bestCombo = 0;
      }

      this.lastMs = now;
   }

   public synchronized void swing() {
      this.touch();
      this.swings++;
   }

   public synchronized void hit(int combo) {
      this.touch();
      this.hits++;
      this.bestCombo = Math.max(this.bestCombo, combo);
   }

   public synchronized void tookDamage(double amount) {
      this.touch();
      this.hurt++;
      this.damage += Math.max(0.0, amount);
   }

   /** Call every tick. Returns the summary exactly once, when a fight has just ended and was worth showing. */
   public synchronized Summary tick() {
      if (this.active && this.clock.getAsLong() - this.lastMs >= IDLE_MS) {
         this.active = false;
         Summary s = new Summary(this.lastMs - this.startMs, this.swings, this.hits, this.hurt, this.damage, this.bestCombo);
         if (s.meaningful()) {
            this.last = s;
            return s;
         }
      }

      return null;
   }

   public synchronized boolean inFight() {
      return this.active;
   }

   /** The last fight that ended, or null. */
   public synchronized Summary last() {
      return this.last;
   }

   public synchronized void reset() {
      this.active = false;
   }
}
