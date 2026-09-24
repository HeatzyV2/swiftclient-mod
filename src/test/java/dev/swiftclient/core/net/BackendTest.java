package dev.swiftclient.core.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import dev.swiftclient.testing.TestGame;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BackendTest {
   private HttpServer server;
   private final AtomicInteger hits = new AtomicInteger();
   private final AtomicReference<String> lastBody = new AtomicReference<>();
   private volatile int status = 200;
   private volatile String reply = "{\"ok\":true}";

   @BeforeEach
   void setUp() throws IOException {
      TestGame.install();
      this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      this.server.createContext("/", ex -> {
         this.hits.incrementAndGet();
         this.lastBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
         byte[] out = this.reply.getBytes(StandardCharsets.UTF_8);
         ex.sendResponseHeaders(this.status, out.length);
         ex.getResponseBody().write(out);
         ex.close();
      });
      this.server.start();
      Endpoints.overrideForTests("http://127.0.0.1:" + this.server.getAddress().getPort(), null);
      Backend.resetForTests();
   }

   @AfterEach
   void tearDown() {
      this.server.stop(0);
      Endpoints.overrideForTests(null, null);
   }

   @Test
   void postSerializesWithGsonIncludingCharactersThatBrokeTheOldConcatenation() {
      Backend.Response r = Backend.post("/api/dm", Map.of("content", "He said \"hi\" \\ <b>\n"), false);
      assertTrue(r.ok(), String.valueOf(r.message()));
      JsonObject sent = JsonParser.parseString(this.lastBody.get()).getAsJsonObject();
      assertEquals("He said \"hi\" \\ <b>\n", sent.get("content").getAsString());
      assertTrue(r.json().getAsJsonObject().get("ok").getAsBoolean());
   }

   @Test
   void serverErrorsOpenTheBreakerAndStopTheCalls() {
      this.status = 500;
      assertFalse(Backend.get("/x", false).ok());
      assertFalse(Backend.get("/x", false).ok());
      int afterTwo = this.hits.get();
      Backend.Response third = Backend.get("/x", false);
      assertEquals(2, afterTwo);
      assertEquals(2, this.hits.get(), "breaker open: no request sent");
      assertEquals("The Swift server can't be reached right now", third.message());
   }

   @Test
   void httpErrorsBecomeReadableMessages() {
      this.status = 403;
      assertEquals("Access refused by the Swift server (HTTP 403)", Backend.get("/x", false).message());
      this.status = 404;
      assertEquals("Service not found on the Swift server", Backend.get("/x", false).message());
      this.status = 200;
      assertNull(Backend.get("/x", false).message());
   }

   @Test
   void authenticatedCallsNeedAMicrosoftSession() {
      Backend.Response r = Backend.get("/api/friends/me", true);
      assertFalse(r.ok());
      assertNotNull(r.message());
      assertEquals(0, this.hits.get(), "no request without a session");
   }

   @Test
   void disabledBackendNeverCallsOut() {
      Endpoints.overrideForTests(null, null);
      Backend.Response r = Backend.get("/x", false);
      assertEquals("The Swift server is disabled", r.message());
      assertEquals(0, this.hits.get());
   }
}
