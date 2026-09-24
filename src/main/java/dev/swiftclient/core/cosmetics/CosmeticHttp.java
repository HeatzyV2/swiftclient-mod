package dev.swiftclient.core.cosmetics;

import dev.swiftclient.core.log.Log;
import org.slf4j.Logger;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.swiftclient.core.platform.Platform;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class CosmeticHttp {
   private static final Logger LOG = Log.get("Backend");
   /** Production backend. Override with {@code -Dswiftclient.api=...} or {@code SWIFTCLIENT_API}; "off" disables it. */
   private static final String DEFAULT_API = "http://151.240.30.3:10049";
   private static final String API = resolveApi();
   private static final String MOJANG_JOIN = "https://sessionserver.mojang.com/session/minecraft/join";
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).build();
   private static final SecureRandom RNG = new SecureRandom();
   /** Mojang join + backend login. Each attempt costs a sessionserver call, so back off hard. */
   private static final Backoff SESSION = new Backoff("session cosmetiques", 30000L, 900000L);
   /** Backend reachability (connection errors, 5xx, 429). */
   private static final Backoff BACKEND = new Backoff("backend Swift", 15000L, 600000L);
   private static volatile String token;
   private static volatile long tokenExp;
   private static volatile String tokenUuid;
   private static final String MC_PROFILE = "https://api.minecraftservices.com/minecraft/profile";
   private static final String MC_CAPE_ACTIVE = "https://api.minecraftservices.com/minecraft/profile/capes/active";

   private CosmeticHttp() {
   }

   private static String resolveApi() {
      String v = System.getProperty("swiftclient.api");
      if (v == null || v.isBlank()) {
         v = System.getenv("SWIFTCLIENT_API");
      }

      if (v == null || v.isBlank()) {
         v = DEFAULT_API;
      }

      if ("off".equalsIgnoreCase(v.trim())) {
         LOG.info("Backend desactive : cosmetiques, badges et heartbeat coupes");
         return null;
      } else {
         v = v.trim();
         while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
         }

         if (!v.startsWith("https://") && !v.startsWith("http://")) {
            LOG.warn("swiftclient.api invalide (http/https attendu) : {}", v);
            return null;
         } else {
            LOG.info("Backend : {}", v);
            return v;
         }
      }
   }

   /** True only when a backend URL is configured. Every backend call is skipped otherwise. */
   public static boolean backendConfigured() {
      return API != null;
   }

   private static String url(String path) {
      return API + path;
   }

   /** Backend call guarded by the reachability backoff. Returns null when skipped or failed. */
   private static <T> HttpResponse<T> send(HttpRequest req, BodyHandler<T> handler) {
      if (!backendConfigured() || BACKEND.blocked()) {
         return null;
      } else {
         try {
            HttpResponse<T> r = HTTP.send(req, handler);
            int code = r.statusCode();
            if (code / 100 != 5 && code != 429) {
               BACKEND.success();
            } else {
               BACKEND.failure("HTTP " + code);
            }

            return r;
         } catch (InterruptedException var4) {
            Thread.currentThread().interrupt();
            return null;
         } catch (Exception var5) {
            BACKEND.failure(var5.getClass().getSimpleName());
            return null;
         }
      }
   }

   private static HttpRequest get(String url, String bearer, long timeoutS) {
      Builder b = HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(timeoutS));
      if (bearer != null) {
         b.header("Authorization", "Bearer " + bearer);
      }

      return b.build();
   }

   private static boolean ok(HttpResponse<?> r) {
      return r != null && r.statusCode() / 100 == 2;
   }

   public static synchronized void dropSession() {
      token = null;
      tokenUuid = null;
   }

   public static synchronized String ensureSession() {
      if (!backendConfigured()) {
         return null;
      } else {
         long now = System.currentTimeMillis();
         String uuid = Platform.game().getUuid();
         if (token != null && now < tokenExp - 60000L && uuid != null && uuid.equals(tokenUuid)) {
            return token;
         } else if (!SESSION.blocked() && !BACKEND.blocked()) {
            String access = Platform.game().getAccessToken();
            String name = Platform.game().getUsername();
            if (access != null && !access.isBlank() && uuid != null && !uuid.isBlank()) {
               byte[] rnd = new byte[20];
               RNG.nextBytes(rnd);
               String serverId = HexFormat.of().formatHex(rnd);
               JsonObject join = new JsonObject();
               join.addProperty("accessToken", access);
               join.addProperty("selectedProfile", uuid);
               join.addProperty("serverId", serverId);

               try {
                  HttpResponse<String> jr = HTTP.send(post(MOJANG_JOIN, join.toString(), null), BodyHandlers.ofString());
                  if (jr.statusCode() / 100 != 2) {
                     SESSION.failure("join Mojang HTTP " + jr.statusCode());
                     return null;
                  }
               } catch (InterruptedException var11) {
                  Thread.currentThread().interrupt();
                  return null;
               } catch (Exception var12) {
                  SESSION.failure("join Mojang " + var12.getClass().getSimpleName());
                  return null;
               }

               JsonObject body = new JsonObject();
               body.addProperty("username", name);
               body.addProperty("uuid", uuid);
               body.addProperty("serverId", serverId);
               HttpResponse<String> lr = send(post(url("/api/auth/login"), body.toString(), null), BodyHandlers.ofString());
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
                  } catch (Exception var10) {
                     SESSION.failure("reponse login illisible");
                     return null;
                  }
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   private static HttpResponse<String> authedPost(String path, String body) {
      for (int attempt = 0; attempt < 2; attempt++) {
         String t = ensureSession();
         if (t == null) {
            return null;
         }

         HttpResponse<String> r = send(post(url(path), body, t), BodyHandlers.ofString());
         if (r == null || r.statusCode() != 401 || attempt != 0) {
            return r;
         }

         dropSession();
      }

      return null;
   }

   private static JsonObject batchBody(List<String> uuids) {
      JsonArray arr = new JsonArray();
      JsonObject names = new JsonObject();

      for (String u : uuids) {
         arr.add(new JsonPrimitive(u));
         String pseudo = OfflineNames.of(u);
         if (pseudo != null) {
            names.addProperty(u, pseudo);
         }
      }

      JsonObject body = new JsonObject();
      body.add("uuids", arr);
      if (!names.entrySet().isEmpty()) {
         body.add("names", names);
      }

      return body;
   }

   private static Map<String, String> stringMap(String path, List<String> uuids) {
      try {
         HttpResponse<String> r = authedPost(path, batchBody(uuids).toString());
         if (!ok(r)) {
            return null;
         } else {
            JsonObject j = JsonParser.parseString(r.body()).getAsJsonObject();
            Map<String, String> out = new HashMap<>();

            for (Entry<String, JsonElement> e : j.entrySet()) {
               out.put(e.getKey(), e.getValue().getAsString());
            }

            return out;
         }
      } catch (Exception var6) {
         return null;
      }
   }

   public static Map<String, String> equippedFor(List<String> uuids) {
      Map<String, String> m = stringMap("/api/cosmetics/equipped", uuids);
      return m == null ? Map.of() : m;
   }

   public static Map<String, Boolean> animatedFor(List<String> uuids) {
      try {
         HttpResponse<String> r = authedPost("/api/cosmetics/animated", batchBody(uuids).toString());
         if (!ok(r)) {
            return Map.of();
         } else {
            JsonObject j = JsonParser.parseString(r.body()).getAsJsonObject();
            Map<String, Boolean> out = new HashMap<>();

            for (Entry<String, JsonElement> e : j.entrySet()) {
               out.put(e.getKey(), e.getValue().getAsBoolean());
            }

            return out;
         }
      } catch (Exception var6) {
         return Map.of();
      }
   }

   public static boolean setCapeAnimated(boolean enabled) {
      JsonObject b = new JsonObject();
      b.addProperty("uuid", Platform.game().getUuid());
      b.addProperty("enabled", enabled);
      return ok(authedPost("/api/cosmetics/cape-animated", b.toString()));
   }

   public static Map<String, String> petsFor(List<String> uuids) {
      Map<String, String> m = stringMap("/api/cosmetics/pets", uuids);
      return m == null ? Map.of() : m;
   }

   public static boolean equipPet(String id) {
      JsonObject b = new JsonObject();
      b.addProperty("uuid", Platform.game().getUuid());
      b.addProperty("id", id);
      return ok(authedPost("/api/cosmetics/equip-pet", b.toString()));
   }

   public static boolean heartbeat() {
      JsonObject b = new JsonObject();
      b.addProperty("uuid", Platform.game().getUuid());
      return ok(authedPost("/api/users/heartbeat", b.toString()));
   }

   public static Map<String, String> gradesFor(List<String> uuids) {
      return stringMap("/api/users/grades", uuids);
   }

   public static boolean subscriptionActiveSelf() {
      try {
         String uuid = Platform.game().getUuid();
         if (backendConfigured() && uuid != null && !uuid.isBlank()) {
            HttpResponse<String> r = send(get(url("/api/subscription/status/" + uuid), null, 8L), BodyHandlers.ofString());
            if (!ok(r)) {
               return false;
            } else {
               JsonObject j = JsonParser.parseString(r.body()).getAsJsonObject();
               return j.has("active") && j.get("active").getAsBoolean();
            }
         } else {
            return false;
         }
      } catch (Exception var3) {
         return false;
      }
   }

   public static JsonArray catalog() {
      try {
         if (!backendConfigured()) {
            return new JsonArray();
         } else {
            HttpResponse<String> r = send(get(url("/api/cosmetics/catalog"), null, 8L), BodyHandlers.ofString());
            return !ok(r) ? new JsonArray() : JsonParser.parseString(r.body()).getAsJsonArray();
         }
      } catch (Exception var1) {
         return new JsonArray();
      }
   }

   public static JsonArray ingamePartners() {
      try {
         if (!backendConfigured()) {
            return new JsonArray();
         } else {
            HttpResponse<String> r = send(get(url("/api/partners/ingame"), null, 8L), BodyHandlers.ofString());
            if (!ok(r)) {
               return new JsonArray();
            } else {
               JsonObject j = JsonParser.parseString(r.body()).getAsJsonObject();
               return j.has("slots") && j.get("slots").isJsonArray() ? j.getAsJsonArray("slots") : new JsonArray();
            }
         }
      } catch (Exception var2) {
         return new JsonArray();
      }
   }

   public static JsonArray partnerServers() {
      try {
         if (!backendConfigured()) {
            return new JsonArray();
         } else {
            HttpResponse<String> r = send(get(url("/api/servers"), null, 8L), BodyHandlers.ofString());
            if (!ok(r)) {
               return new JsonArray();
            } else {
               JsonElement el = JsonParser.parseString(r.body());
               return el.isJsonArray() ? el.getAsJsonArray() : new JsonArray();
            }
         }
      } catch (Exception var2) {
         return new JsonArray();
      }
   }

   public static byte[] texture(String id) {
      String t = ensureSession();
      if (t == null) {
         return null;
      } else {
         HttpResponse<byte[]> r = send(get(url("/api/cosmetics/texture/" + id), t, 15L), BodyHandlers.ofByteArray());
         return !ok(r) ? null : r.body();
      }
   }

   public static JsonObject petModel(String id) {
      try {
         String t = ensureSession();
         if (t == null) {
            return null;
         } else {
            HttpResponse<String> r = send(get(url("/api/cosmetics/pet-model/" + id), t, 20L), BodyHandlers.ofString());
            return !ok(r) ? null : JsonParser.parseString(r.body()).getAsJsonObject();
         }
      } catch (Exception var3) {
         return null;
      }
   }

   public static boolean postAuthed(String path, JsonObject body) {
      return ok(authedPost(path, body.toString()));
   }

   public static JsonObject ownedSelf() {
      try {
         String t = ensureSession();
         if (t == null) {
            return null;
         } else {
            HttpResponse<String> r = send(get(url("/api/cosmetics/owned/" + Platform.game().getUuid()), t, 8L), BodyHandlers.ofString());
            return !ok(r) ? null : JsonParser.parseString(r.body()).getAsJsonObject();
         }
      } catch (Exception var2) {
         return null;
      }
   }

   public static int balance() {
      try {
         String uuid = Platform.game().getUuid();
         if (backendConfigured() && uuid != null && !uuid.isBlank()) {
            HttpResponse<String> r = send(get(url("/api/shop/balance/" + uuid), null, 8L), BodyHandlers.ofString());
            if (!ok(r)) {
               return -1;
            } else {
               JsonObject j = JsonParser.parseString(r.body()).getAsJsonObject();
               return j.has("coins") ? j.get("coins").getAsInt() : -1;
            }
         } else {
            return -1;
         }
      } catch (Exception var3) {
         return -1;
      }
   }

   public static boolean buyCosmetic(String id) {
      JsonObject b = new JsonObject();
      b.addProperty("uuid", Platform.game().getUuid());
      b.addProperty("id", id);
      return ok(authedPost("/api/cosmetics/buy", b.toString()));
   }

   public static boolean equipCosmetic(String id) {
      JsonObject b = new JsonObject();
      b.addProperty("uuid", Platform.game().getUuid());
      b.addProperty("id", id);
      return ok(authedPost("/api/cosmetics/equip", b.toString()));
   }

   public static boolean declareMojangCape(String url) {
      JsonObject b = new JsonObject();
      b.addProperty("uuid", Platform.game().getUuid());
      if (url == null) {
         b.add("url", JsonNull.INSTANCE);
      } else {
         b.addProperty("url", url);
      }

      return ok(authedPost("/api/cosmetics/mojang-cape", b.toString()));
   }

   public static JsonArray mojangCapes() {
      try {
         String access = Platform.game().getAccessToken();
         if (access != null && !access.isBlank()) {
            HttpResponse<String> r = HTTP.send(get(MC_PROFILE, access, 8L), BodyHandlers.ofString());
            if (r.statusCode() / 100 != 2) {
               return null;
            } else {
               JsonObject j = JsonParser.parseString(r.body()).getAsJsonObject();
               return j.has("capes") ? j.getAsJsonArray("capes") : new JsonArray();
            }
         } else {
            return null;
         }
      } catch (Exception var3) {
         return null;
      }
   }

   public static boolean setMojangCapeActive(String capeId) {
      try {
         String access = Platform.game().getAccessToken();
         if (access != null && !access.isBlank()) {
            JsonObject b = new JsonObject();
            b.addProperty("capeId", capeId);
            HttpResponse<String> r = HTTP.send(
               HttpRequest.newBuilder(URI.create(MC_CAPE_ACTIVE))
                  .header("Authorization", "Bearer " + access)
                  .header("Content-Type", "application/json")
                  .method("PUT", BodyPublishers.ofString(b.toString()))
                  .timeout(Duration.ofSeconds(8L))
                  .build(),
               BodyHandlers.ofString()
            );
            return r.statusCode() / 100 == 2;
         } else {
            return false;
         }
      } catch (Exception var4) {
         return false;
      }
   }

   public static boolean disableMojangCape() {
      try {
         String access = Platform.game().getAccessToken();
         if (access != null && !access.isBlank()) {
            HttpResponse<String> r = HTTP.send(
               HttpRequest.newBuilder(URI.create(MC_CAPE_ACTIVE)).header("Authorization", "Bearer " + access).DELETE().timeout(Duration.ofSeconds(8L)).build(),
               BodyHandlers.ofString()
            );
            return r.statusCode() / 100 == 2;
         } else {
            return false;
         }
      } catch (Exception var2) {
         return false;
      }
   }

   public static byte[] rawTexture(String url) {
      try {
         if (url != null && url.matches("^https?://textures\\.minecraft\\.net/texture/[0-9a-fA-F]+$")) {
            HttpResponse<byte[]> r = HTTP.send(
               HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(15L)).build(), BodyHandlers.ofByteArray()
            );
            return r.statusCode() / 100 != 2 ? null : r.body();
         } else {
            return null;
         }
      } catch (Exception var2) {
         return null;
      }
   }

   private static HttpRequest post(String url, String json, String bearer) {
      Builder b = HttpRequest.newBuilder(URI.create(url))
         .header("Content-Type", "application/json")
         .timeout(Duration.ofSeconds(8L))
         .POST(BodyPublishers.ofString(json));
      if (bearer != null) {
         b.header("Authorization", "Bearer " + bearer);
      }

      return b.build();
   }
}
