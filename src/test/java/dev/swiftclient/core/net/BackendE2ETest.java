package dev.swiftclient.core.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import dev.swiftclient.core.cosmetics.CosmeticHttp;
import dev.swiftclient.core.relay.RelayClient;
import dev.swiftclient.core.social.SocialApi;
import dev.swiftclient.testing.TestGame;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The mod against a real backend (skipped unless {@code -Pe2eApi} is given, see build.gradle). A fake
 * Mojang session server on port 18555 stands in for sessionserver.mojang.com on both sides.
 */
class BackendE2ETest {
   private static final String API = System.getProperty("swiftclient.e2e.api");
   private static final String RELAY = System.getProperty("swiftclient.e2e.relay", "");
   private static final String GATEWAY = System.getProperty("swiftclient.e2e.gateway", "");
   private static final int MOJANG_PORT = 18555;
   /** serverId -> "name uuid" of the player who joined with it. */
   private static final Map<String, String> JOINS = new ConcurrentHashMap<>();
   private static HttpServer mojang;
   private static final String RUN = Long.toString(System.currentTimeMillis() % 100000L, 36);
   private static final String[] ALICE = {UUID.randomUUID().toString(), "Alice" + RUN, "token-alice"};
   private static final String[] BOB = {UUID.randomUUID().toString(), "Bob" + RUN, "token-bob"};

   @BeforeAll
   static void start() throws IOException {
      assumeTrue(API != null && !API.isBlank(), "no -Pe2eApi");
      mojang = HttpServer.create(new InetSocketAddress("127.0.0.1", MOJANG_PORT), 0);
      mojang.createContext("/session/minecraft/join", ex -> {
         JsonObject body = Net.GSON.fromJson(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
         String token = body.get("accessToken").getAsString();
         String profile = body.get("selectedProfile").getAsString();
         String[] who = token.equals(ALICE[2]) ? ALICE : token.equals(BOB[2]) ? BOB : null;
         int status = who != null && who[0].equals(profile) ? 204 : 403;
         if (status == 204) {
            JOINS.put(body.get("serverId").getAsString(), who[1] + " " + who[0]);
         }

         ex.sendResponseHeaders(status, -1);
         ex.close();
      });
      mojang.createContext("/session/minecraft/hasJoined", ex -> {
         Map<String, String> q = new ConcurrentHashMap<>();
         for (String kv : ex.getRequestURI().getRawQuery().split("&")) {
            String[] p = kv.split("=", 2);
            q.put(p[0], URLDecoder.decode(p[1], StandardCharsets.UTF_8));
         }

         String joined = JOINS.get(q.get("serverId"));
         if (joined != null && joined.startsWith(q.get("username") + " ")) {
            String uuid = joined.substring(joined.indexOf(' ') + 1).replace("-", "");
            byte[] out = ("{\"id\":\"" + uuid + "\",\"name\":\"" + q.get("username") + "\"}").getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, out.length);
            ex.getResponseBody().write(out);
         } else {
            ex.sendResponseHeaders(204, -1);
         }

         ex.close();
      });
      mojang.start();
      TestGame.install();
      Backend.mojangSessionForTests("http://127.0.0.1:" + MOJANG_PORT);
      Endpoints.overrideForTests(API, RELAY.isBlank() ? null : RELAY);
   }

   @AfterAll
   static void stop() {
      if (mojang != null) {
         mojang.stop(0);
         Backend.mojangSessionForTests("https://sessionserver.mojang.com");
         Endpoints.overrideForTests(null, null);
      }
   }

   private static void playAs(String[] player) {
      TestGame.PLAYER[0] = player[0];
      TestGame.PLAYER[1] = player[1];
      TestGame.PLAYER[2] = player[2];
      Backend.resetForTests();
   }

   private static <T> T await(CompletableFuture<T> f) throws Exception {
      return f.get(20L, TimeUnit.SECONDS);
   }

   @Test
   void theModTalksToTheBackend() throws Exception {
      // Sign-in: Mojang join + /api/auth/minecraft, account created on first use.
      playAs(ALICE);
      assertNotNull(Backend.session(), "game sign-in");
      assertTrue(CosmeticHttp.heartbeat());
      Map<String, String> grades = CosmeticHttp.gradesFor(List.of(ALICE[0], BOB[0]));
      assertEquals(Map.of(ALICE[0], "member"), grades, "only Swift players get a grade");
      assertTrue(CosmeticHttp.catalog().isJsonArray());
      assertEquals(0, CosmeticHttp.balance());
      assertFalse(CosmeticHttp.subscriptionActiveSelf());
      assertEquals("elysiasmp.com", CosmeticHttp.ingamePartners().get(0).getAsJsonObject().get("ip").getAsString());
      JsonObject owned = CosmeticHttp.ownedSelf();
      assertNotNull(owned);
      assertTrue(owned.getAsJsonArray("owned").isEmpty());
      assertTrue(CosmeticHttp.equipCosmetic("none").ok());
      assertTrue(CosmeticHttp.setCapeAnimated(false));
      assertEquals(Map.of(ALICE[0], false), CosmeticHttp.animatedFor(List.of(ALICE[0])));
      assertEquals(404, CosmeticHttp.buyCosmetic("does-not-exist").status(), "unknown cosmetic refused");

      // Friends and messages: Bob adds Alice by her Minecraft name.
      playAs(BOB);
      assertNotNull(Backend.session());
      SocialApi.Result<Boolean> added = await(SocialApi.requestFriendByName(ALICE[1]));
      assertTrue(added.ok(), added.error());
      SocialApi.Result<List<SocialApi.Friend>> friends = await(SocialApi.listFriends());
      assertTrue(friends.ok(), friends.error());
      SocialApi.Friend alice = friends.value().stream().filter(f -> f.username().equals(ALICE[1])).findFirst().orElseThrow();
      assertTrue(alice.online(), "Alice sent a heartbeat");
      assertEquals(ALICE[0].replace("-", ""), alice.uuid().replace("-", ""));
      SocialApi.Result<Boolean> sent = await(SocialApi.sendDM(alice, "swift-invite://127.0.0.1:25565"));
      assertTrue(sent.ok(), sent.error());

      playAs(ALICE);
      SocialApi.Friend bob = await(SocialApi.listFriends()).value().stream().filter(f -> f.username().equals(BOB[1])).findFirst().orElseThrow();
      SocialApi.Result<List<SocialApi.Message>> dms = await(SocialApi.listDM(bob));
      assertTrue(dms.ok(), dms.error());
      assertEquals("swift-invite://127.0.0.1:25565", dms.value().get(dms.value().size() - 1).content());
   }

   @Test
   void mojangMustConfirmTheSession() {
      JsonObject forged = new JsonObject();
      forged.addProperty("username", ALICE[1]);
      forged.addProperty("uuid", ALICE[0]);
      forged.addProperty("serverId", "never-joined");
      assertEquals(401, Backend.post("/api/auth/minecraft", forged, false).status());
   }

   /** Minecraft handshake packet for `host` (protocol 775, next state 1 = status, 2 = login). */
   private static byte[] handshake(String host, int nextState) {
      java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream();
      body.write(0x00);
      writeVarInt(body, 775);
      byte[] h = host.getBytes(StandardCharsets.UTF_8);
      writeVarInt(body, h.length);
      body.writeBytes(h);
      body.write(0x63);
      body.write(0xDD);
      writeVarInt(body, nextState);
      java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
      writeVarInt(out, body.size());
      out.writeBytes(body.toByteArray());
      return out.toByteArray();
   }

   private static void writeVarInt(java.io.ByteArrayOutputStream out, int v) {
      while ((v & ~0x7F) != 0) {
         out.write(v & 0x7F | 0x80);
         v >>>= 7;
      }

      out.write(v);
   }

   @Test
   void hostedWorldIsReachableAtPlayerNameAddress() throws Exception {
      assumeTrue(!RELAY.isBlank() && !GATEWAY.isBlank(), "no -Pe2eRelay / -Pe2eGateway");
      JsonElement info = Backend.get("/api/relay", false).json();
      assertEquals(Endpoints.relayPort(), info.getAsJsonObject().get("port").getAsInt(), "relay announced by the backend");
      String domain = info.getAsJsonObject().get("domain").getAsString();
      playAs(ALICE);
      assertNotNull(Backend.session(), "the gateway name comes from the Mojang-confirmed account");

      try (ServerSocket lan = new ServerSocket(0)) {
         // The "world": echoes everything, so the player gets back its handshake then its bytes
         Thread echo = new Thread(() -> {
            try (Socket s = lan.accept()) {
               s.getInputStream().transferTo(s.getOutputStream());
            } catch (IOException ignored) {
            }
         });
         echo.setDaemon(true);
         echo.start();
         CompletableFuture<String> address = new CompletableFuture<>();
         RelayClient relay = new RelayClient(lan.getLocalPort(), address::complete, e -> address.completeExceptionally(new IOException(e)));
         relay.start();

         try {
            String expected = ALICE[1].toLowerCase(java.util.Locale.ROOT) + "." + domain;
            assertEquals(expected, await(address), "address shown to the host");
            String gwHost = GATEWAY.substring(0, GATEWAY.lastIndexOf(':'));
            int gwPort = Integer.parseInt(GATEWAY.substring(GATEWAY.lastIndexOf(':') + 1));

            // A player types "Alice….domain" (any case, trailing dot) and lands in the world
            try (Socket player = new Socket(gwHost, gwPort)) {
               player.setSoTimeout(10000);
               byte[] hs = handshake(ALICE[1] + "." + domain.toUpperCase(java.util.Locale.ROOT) + ".", 2);
               OutputStream out = player.getOutputStream();
               out.write(hs);
               out.write("hello relay".getBytes(StandardCharsets.UTF_8));
               out.flush();
               byte[] back = player.getInputStream().readNBytes(hs.length + 11);
               assertEquals("hello relay", new String(back, hs.length, 11, StandardCharsets.UTF_8));
               assertEquals(java.util.Arrays.toString(hs), java.util.Arrays.toString(java.util.Arrays.copyOf(back, hs.length)), "handshake forwarded as sent");
            }

            // Nobody hosts under this name: the server list shows why
            try (Socket ping = new Socket(gwHost, gwPort)) {
               ping.setSoTimeout(10000);
               OutputStream out = ping.getOutputStream();
               out.write(handshake("nobody-here." + domain, 1));
               out.write(new byte[]{1, 0}); // status request
               out.write(new byte[]{9, 1, 0, 0, 0, 0, 0, 0, 0, 42}); // ping: the server answers pong and closes
               out.flush();
               byte[] reply = ping.getInputStream().readAllBytes();
               assertTrue(new String(reply, StandardCharsets.UTF_8).contains("Aucun monde"), new String(reply, StandardCharsets.UTF_8));
            }
         } finally {
            relay.stop();
         }
      }
   }
}
