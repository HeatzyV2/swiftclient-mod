package dev.swiftclient.core.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class CircuitBreakerTest {
   @Test
   void opensAfterThresholdAndLetsOneTrialThroughAfterTheDelay() {
      AtomicLong now = new AtomicLong();
      CircuitBreaker b = new CircuitBreaker("test", 2, 50L, 1000L, now::get);
      assertTrue(b.allow());
      b.failure("x");
      assertTrue(b.allow(), "one failure is below the threshold");
      b.failure("x");
      assertFalse(b.allow(), "open after two consecutive failures");
      assertTrue(b.isOpen());

      now.addAndGet(50L);
      assertTrue(b.allow(), "half-open: first caller gets the trial");
      assertFalse(b.allow(), "only one trial at a time");
      b.success();
      assertTrue(b.allow());
      assertFalse(b.isOpen());
   }

   @Test
   void delayDoublesOnEachReopeningAndIsCapped() {
      AtomicLong now = new AtomicLong();
      CircuitBreaker b = new CircuitBreaker("test", 1, 40L, 100L, now::get);
      b.failure("x");
      now.addAndGet(40L);
      assertTrue(b.allow());
      b.failure("x");
      now.addAndGet(79L);
      assertFalse(b.allow(), "second opening waits 80 ms");
      now.incrementAndGet();
      assertTrue(b.allow());
      b.failure("x");
      now.addAndGet(99L);
      assertFalse(b.allow(), "the next delay is capped at 100 ms");
      now.incrementAndGet();
      assertTrue(b.allow());
      assertEquals(3, b.openings());
   }

   @Test
   void releaseGivesBackTheTrialWithoutAVerdict() {
      AtomicLong now = new AtomicLong();
      CircuitBreaker b = new CircuitBreaker("test", 1, 20L, 100L, now::get);
      b.failure("x");
      now.addAndGet(20L);
      assertTrue(b.allow());
      b.release();
      assertTrue(b.allow(), "trial released, still half-open");
   }
}
