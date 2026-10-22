package dev.swiftclient.core.net;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NetTest {
   /** Sharing the IO pool with the HTTP client starved it: every backend call timed out once four were in flight. */
   @Test
   void httpClientDoesNotRunOnTheIoPool() {
      assertTrue(Net.HTTP.executor().isEmpty(), "Net.HTTP must keep its own executor");
   }
}
