package dev.swiftclient.core.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CircuitBreakerTest {
   @Test
   void opensAfterThresholdAndLetsOneTrialThroughAfterTheDelay() throws Exception {
      CircuitBreaker b = new CircuitBreaker("test", 2, 50L, 1000L);
      assertTrue(b.allow());
      b.failure("x");
      assertTrue(b.allow(), "one failure is below the threshold");
      b.failure("x");
      assertFalse(b.allow(), "open after two consecutive failures");
      assertTrue(b.isOpen());

      Thread.sleep(80L);
      assertTrue(b.allow(), "half-open: first caller gets the trial");
      assertFalse(b.allow(), "only one trial at a time");
      b.success();
      assertTrue(b.allow());
      assertFalse(b.isOpen());
   }

   @Test
   void delayDoublesOnEachReopeningAndIsCapped() throws Exception {
      CircuitBreaker b = new CircuitBreaker("test", 1, 40L, 100L);
      b.failure("x");
      Thread.sleep(50L);
      assertTrue(b.allow());
      b.failure("x");
      Thread.sleep(50L);
      assertFalse(b.allow(), "second opening waits 80 ms");
      Thread.sleep(40L);
      assertTrue(b.allow());
      assertEquals(2, b.openings());
   }

   @Test
   void releaseGivesBackTheTrialWithoutAVerdict() throws Exception {
      CircuitBreaker b = new CircuitBreaker("test", 1, 20L, 100L);
      b.failure("x");
      Thread.sleep(30L);
      assertTrue(b.allow());
      b.release();
      assertTrue(b.allow(), "trial released, still half-open");
   }
}
