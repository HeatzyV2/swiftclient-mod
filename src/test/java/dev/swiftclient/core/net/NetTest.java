package dev.swiftclient.core.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class NetTest {
   /** Sharing the IO pool with the HTTP client starved it: every backend call timed out once four were in flight. */
   @Test
   void httpClientDoesNotRunOnTheIoPool() {
      assertTrue(Net.HTTP.executor().isEmpty(), "Net.HTTP must keep its own executor");
   }

   /** The API sits behind Cloudflare, which does not carry the raw TCP of the relay. */
   @Test
   void relayIsNotReachedThroughTheApiHost() {
      String api = "api.swiftclient.fr";
      assertEquals("relay.swiftclient.fr", Endpoints.announcedRelayHost(JsonParser.parseString("{\"port\":10050,\"domain\":\"swiftclient.fr\"}").getAsJsonObject(), api));
      assertEquals("82.22.7.62", Endpoints.announcedRelayHost(JsonParser.parseString("{\"port\":10050,\"host\":\"82.22.7.62\",\"domain\":\"swiftclient.fr\"}").getAsJsonObject(), api));
      assertEquals(api, Endpoints.announcedRelayHost(JsonParser.parseString("{\"port\":10050,\"domain\":null}").getAsJsonObject(), api));
   }
}
