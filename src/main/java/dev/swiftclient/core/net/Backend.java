package dev.swiftclient.core.net;

import dev.swiftclient.core.platform.Tr;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.platform.Platform;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.slf4j.Logger;

/**
 * Client for the Swift Client backend ({@link Endpoints#api()}). Authenticates with a backend token
 * obtained by proving the Minecraft session (Mojang "join" then {@code /api/auth/login}), sends JSON
 * with Gson, and goes through circuit breakers so a dead backend is not hammered.
 */
public final class Backend {
   private static final Logger LOG = Log.get("Backend");
   private static final String MOJANG_JOIN = "https://sessionserver.mojang.com/session/minecraft/join";
   private static final SecureRandom RNG = new SecureRandom();
   /** Backend reachability: connection errors, 5xx, 429. */
   private static final CircuitBreaker AVAILABILITY = new CircuitBreaker("Backend Swift", 2, 15000L, 600000L);
   /** Session opening: each attempt costs a Mojang sessionserver call, so back off hard. */
   private static final CircuitBreaker SESSION = new CircuitBreaker("Session backend", 1, 30000L, 900000L);
   private static volatile String token;
   private static volatile long tokenExp;
   private static volatile String tokenUuid;

   private Backend() {
   }

   /** Outcome of a backend call. {@code error} is a short sentence for the player when it failed. */
   public record Response(int status, String body, String error) {
      public boolean ok() {
         return this.error == null && this.status / 100 == 2;
      }

      public JsonElement json() {
         try {
            return this.body == null || this.body.isBlank() ? null : JsonParser.parseString(this.body);
         } catch (RuntimeException e) {
            return null;
         }
      }

      public <T> T as(Class<T> type) {
         try {
            return this.body == null ? null : Net.GSON.fromJson(this.body, type);
         } catch (RuntimeException e) {
            return null;
         }
      }

      /** Message to show when the call failed, e.g. in a toast. */
      public String message() {
         if (this.ok()) {
            return null;
         } else if (this.error != null) {
            return this.error;
         } else if (this.status == 401 || this.status == 403) {
            return Tr.of("swift.net.denied", this.status);
         } else if (this.status == 404) {
            return Tr.of("swift.net.not_found");
         } else {
            return Tr.of("swift.net.http_error", this.status);
         }
      }

      static Response failed(String error) {
         return new Response(0, null, error);
      }
   }

   public static boolean enabled() {
      return Endpoints.backendEnabled();
   }

   /** Runs a blocking backend call on the shared pool. */
   public static <T> CompletableFuture<T> async(Supplier<T> call) {
      return CompletableFuture.supplyAsync(call, Net.IO);
   }

   // --- Requests ---

   public static Response get(String path, boolean authenticated) {
      return call(path, authenticated, t -> base(path, t).GET().build(), BodyHandlers.ofString());
   }

   /** POST with {@code body} serialized by Gson (a JsonElement, a record, a Map...). */
   public static Response post(String path, Object body, boolean authenticated) {
      String json = body instanceof JsonElement e ? e.toString() : Net.GSON.toJson(body);
      return call(
         path, authenticated, t -> base(path, t).header("Content-Type", "application/json").POST(BodyPublishers.ofString(json)).build(), BodyHandlers.ofString()
      );
   }

   /** GET returning raw bytes (textures), null on failure. */
   public static byte[] getBytes(String path, boolean authenticated) {
      HttpResponse<byte[]> r = send(path, authenticated, t -> base(path, t).timeout(Duration.ofSeconds(20L)).GET().build(), BodyHandlers.ofByteArray());
      return r != null && r.statusCode() / 100 == 2 ? r.body() : null;
   }

   private interface RequestFactory {
      HttpRequest build(String token);
   }

   private static HttpRequest.Builder base(String path, String bearer) {
      HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(Endpoints.api() + path)).timeout(Duration.ofSeconds(8L));
      if (bearer != null) {
         b.header("Authorization", "Bearer " + bearer);
      }

      return b;
   }

   private static Response call(String path, boolean authenticated, RequestFactory factory, BodyHandler<String> handler) {
      if (!enabled()) {
         return Response.failed(Tr.of("swift.net.disabled"));
      } else if (AVAILABILITY.isOpen()) {
         return Response.failed(Tr.of("swift.net.unreachable"));
      } else if (authenticated && session() == null) {
         return Response.failed(AVAILABILITY.isOpen() ? Tr.of("swift.net.unreachable") : Tr.of("swift.net.need_account"));
      } else {
         HttpResponse<String> r = send(path, authenticated, factory, handler);
         return r == null ? Response.failed(Tr.of("swift.net.unreachable")) : new Response(r.statusCode(), r.body(), null);
      }
   }

   /** Sends with breaker bookkeeping; on 401 drops the session and retries once with a fresh token. */
   private static <T> HttpResponse<T> send(String path, boolean authenticated, RequestFactory factory, BodyHandler<T> handler) {
      if (!enabled() || !AVAILABILITY.allow()) {
         return null;
      } else {
         for (int attempt = 0; attempt < 2; attempt++) {
            String t = null;
            if (authenticated) {
               t = session();
               if (t == null) {
                  AVAILABILITY.release();
                  return null;
               }
            }

            HttpResponse<T> r = raw(factory.build(t), handler);
            if (r == null) {
               return null;
            }

            if (r.statusCode() != 401 || !authenticated || attempt == 1) {
               return r;
            }

            dropSession();
         }

         return null;
      }
   }

   private static <T> HttpResponse<T> raw(HttpRequest request, BodyHandler<T> handler) {
      try {
         HttpResponse<T> r = Net.HTTP.send(request, handler);
         if (r.statusCode() / 100 == 5 || r.statusCode() == 429) {
            AVAILABILITY.failure("HTTP " + r.statusCode());
         } else {
            AVAILABILITY.success();
         }

         return r;
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         return null;
      } catch (Exception e) {
         AVAILABILITY.failure(e.getClass().getSimpleName());
         return null;
      }
   }

   // --- Session ---

   /** Test support: forget the session and close both breakers. */
   static synchronized void resetForTests() {
      dropSession();
      AVAILABILITY.success();
      SESSION.success();
   }

   public static synchronized void dropSession() {
      token = null;
      tokenUuid = null;
   }

   /** Backend token for the current Minecraft account, opened on demand. Null if unavailable. */
   public static synchronized String session() {
      if (!enabled()) {
         return null;
      }

      long now = System.currentTimeMillis();
      String uuid = Platform.game().getUuid();
      if (token != null && now < tokenExp - 60000L && uuid != null && uuid.equals(tokenUuid)) {
         return token;
      }

      String access = Platform.game().getAccessToken();
      if (access == null || access.isBlank() || uuid == null || uuid.isBlank() || AVAILABILITY.isOpen() || !SESSION.allow()) {
         return null;
      }

      String serverId = HexFormat.of().formatHex(randomBytes());
      JsonObject join = new JsonObject();
      join.addProperty("accessToken", access);
      join.addProperty("selectedProfile", uuid);
      join.addProperty("serverId", serverId);

      try {
         HttpResponse<String> jr = Net.HTTP.send(
            HttpRequest.newBuilder(URI.create(MOJANG_JOIN))
               .timeout(Duration.ofSeconds(8L))
               .header("Content-Type", "application/json")
               .POST(BodyPublishers.ofString(join.toString()))
               .build(),
            BodyHandlers.ofString()
         );
         if (jr.statusCode() / 100 != 2) {
            SESSION.failure("join Mojang HTTP " + jr.statusCode());
            return null;
         }
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         return null;
      } catch (Exception e) {
         SESSION.failure("join Mojang " + e.getClass().getSimpleName());
         return null;
      }

      JsonObject login = new JsonObject();
      login.addProperty("username", Platform.game().getUsername());
      login.addProperty("uuid", uuid);
      login.addProperty("serverId", serverId);
      HttpResponse<String> lr = raw(
         base("/api/auth/login", null).header("Content-Type", "application/json").POST(BodyPublishers.ofString(login.toString())).build(),
         BodyHandlers.ofString()
      );
      if (lr == null) {
         SESSION.failure("backend injoignable");
         return null;
      } else if (lr.statusCode() / 100 != 2) {
         SESSION.failure("/api/auth/login HTTP " + lr.statusCode());
         return null;
      } else {
         try {
            JsonObject j = JsonParser.parseString(lr.body()).getAsJsonObject();
            token = j.get("token").getAsString();
            tokenExp = j.has("expiresAt") ? j.get("expiresAt").getAsLong() : now + 3600000L;
            tokenUuid = uuid;
            SESSION.success();
            LOG.info("Session backend ouverte");
            return token;
         } catch (RuntimeException e) {
            SESSION.failure("reponse login illisible");
            return null;
         }
      }
   }

   private static byte[] randomBytes() {
      byte[] b = new byte[20];
      RNG.nextBytes(b);
      return b;
   }
}
