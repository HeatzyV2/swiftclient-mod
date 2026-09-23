package dev.swiftclient.core.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.swiftclient.core.partner.PartnerTracking;
import dev.swiftclient.core.platform.Platform;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class CosmeticHttp {
   private static final String API = "http://127.0.0.1:28752/api/disabled";
   private static final String MOJANG_JOIN = "https://sessionserver.mojang.com/session/minecraft/join";
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).build();
   private static final SecureRandom RNG = new SecureRandom();
   private static volatile String token;
   private static volatile long tokenExp;
   private static volatile String tokenUuid;
   private static final String MC_PROFILE = "https://api.minecraftservices.com/minecraft/profile";
   private static final String MC_CAPE_ACTIVE = "https://api.minecraftservices.com/minecraft/profile/capes/active";

   private CosmeticHttp() {
   }

   public static synchronized void dropSession() {
      token = null;
      tokenUuid = null;
   }

   public static synchronized String ensureSession() {
      long now = System.currentTimeMillis();
      String uuid = Platform.game().getUuid();
      if (token != null && now < tokenExp - 60000L && uuid != null && uuid.equals(tokenUuid)) {
         return token;
      } else {
         try {
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
               HttpResponse<String> jr = HTTP.send(
                  post("https://sessionserver.mojang.com/session/minecraft/join", join.toString(), null), BodyHandlers.ofString()
               );
               if (jr.statusCode() / 100 != 2) {
                  System.out.println("[LC/Cape] join Mojang a échoué HTTP " + jr.statusCode());
                  return null;
               } else {
                  JsonObject body = new JsonObject();
                  body.addProperty("username", name);
                  body.addProperty("uuid", uuid);
                  body.addProperty("serverId", serverId);
                  HttpResponse<String> lr = HTTP.send(post("http://127.0.0.1:28752/api/disabled", body.toString(), null), BodyHandlers.ofString());
                  if (lr.statusCode() / 100 != 2) {
                     System.out.println("[LC/Cape] /api/auth/login a échoué HTTP " + lr.statusCode() + " : " + lr.body());
                     return null;
                  } else {
                     JsonObject j = new JsonParser().parse(lr.body()).getAsJsonObject();
                     token = j.get("token").getAsString();
                     tokenExp = j.has("expiresAt") ? j.get("expiresAt").getAsLong() : now + 3600000L;
                     tokenUuid = uuid;
                     System.out.println("[LC/Cape] session OK (auth réussie)");
                     return token;
                  }
               }
            } else {
               return null;
            }
         } catch (Exception var12) {
            System.out.println("[LC/Cape] auth exception : " + var12);
            return null;
         }
      }
   }

   private static HttpResponse<String> authedPost(String path, String body) {
      for (int attempt = 0; attempt < 2; attempt++) {
         try {
            String t = ensureSession();
            if (t == null) {
               return null;
            }

            HttpResponse<String> r = HTTP.send(post("http://127.0.0.1:28752/api/disabled" + path, body, t), BodyHandlers.ofString());
            if (r.statusCode() != 401 || attempt != 0) {
               return r;
            }

            dropSession();
         } catch (Exception var5) {
            return null;
         }
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

   public static Map<String, String> equippedFor(List<String> uuids) {
      try {
         String t = ensureSession();
         if (t == null) {
            return Map.of();
         } else {
            JsonObject body = batchBody(uuids);
            HttpResponse<String> r = authedPost("/api/cosmetics/equipped", body.toString());
            if (r != null && r.statusCode() / 100 == 2) {
               JsonObject j = new JsonParser().parse(r.body()).getAsJsonObject();
               Map<String, String> out = new HashMap<>();

               for (Entry<String, JsonElement> e : j.entrySet()) {
                  out.put(e.getKey(), e.getValue().getAsString());
               }

               return out;
            } else {
               return Map.of();
            }
         }
      } catch (Exception var8) {
         return Map.of();
      }
   }

   public static Map<String, Boolean> animatedFor(List<String> uuids) {
      try {
         String t = ensureSession();
         if (t == null) {
            return Map.of();
         } else {
            JsonObject body = batchBody(uuids);
            HttpResponse<String> r = authedPost("/api/cosmetics/animated", body.toString());
            if (r != null && r.statusCode() / 100 == 2) {
               JsonObject j = new JsonParser().parse(r.body()).getAsJsonObject();
               Map<String, Boolean> out = new HashMap<>();

               for (Entry<String, JsonElement> e : j.entrySet()) {
                  out.put(e.getKey(), e.getValue().getAsBoolean());
               }

               return out;
            } else {
               return Map.of();
            }
         }
      } catch (Exception var8) {
         return Map.of();
      }
   }

   public static boolean setCapeAnimated(boolean enabled) {
      try {
         String t = ensureSession();
         if (t == null) {
            return false;
         } else {
            JsonObject b = new JsonObject();
            b.addProperty("uuid", Platform.game().getUuid());
            b.addProperty("enabled", enabled);
            HttpResponse<String> r = HTTP.send(post("http://127.0.0.1:28752/api/disabled", b.toString(), t), BodyHandlers.ofString());
            return r.statusCode() / 100 == 2;
         }
      } catch (Exception var4) {
         return false;
      }
   }

   public static Map<String, String> petsFor(List<String> uuids) {
      try {
         String t = ensureSession();
         if (t == null) {
            return Map.of();
         } else {
            JsonObject body = batchBody(uuids);
            HttpResponse<String> r = authedPost("/api/cosmetics/pets", body.toString());
            if (r != null && r.statusCode() / 100 == 2) {
               JsonObject j = new JsonParser().parse(r.body()).getAsJsonObject();
               Map<String, String> out = new HashMap<>();

               for (Entry<String, JsonElement> e : j.entrySet()) {
                  out.put(e.getKey(), e.getValue().getAsString());
               }

               return out;
            } else {
               return Map.of();
            }
         }
      } catch (Exception var8) {
         return Map.of();
      }
   }

   public static boolean equipPet(String id) {
      try {
         String t = ensureSession();
         if (t == null) {
            return false;
         } else {
            JsonObject b = new JsonObject();
            b.addProperty("uuid", Platform.game().getUuid());
            b.addProperty("id", id);
            HttpResponse<String> r = HTTP.send(post("http://127.0.0.1:28752/api/disabled", b.toString(), t), BodyHandlers.ofString());
            return r.statusCode() / 100 == 2;
         }
      } catch (Exception var4) {
         return false;
      }
   }

   public static boolean heartbeat() {
      JsonObject b = new JsonObject();
      b.addProperty("uuid", Platform.game().getUuid());
      String srv = PartnerTracking.serveurCourant();
      if (srv != null && !srv.isBlank()) {
         b.addProperty("server", srv);
      }

      HttpResponse<String> r = authedPost("/api/users/heartbeat", b.toString());
      return r != null && r.statusCode() / 100 == 2;
   }

   public static Map<String, String> gradesFor(List<String> uuids) {
      try {
         String t = ensureSession();
         if (t == null) {
            return null;
         } else {
            JsonObject body = batchBody(uuids);
            HttpResponse<String> r = authedPost("/api/users/grades", body.toString());
            if (r != null && r.statusCode() / 100 == 2) {
               JsonObject j = new JsonParser().parse(r.body()).getAsJsonObject();
               Map<String, String> out = new HashMap<>();

               for (Entry<String, JsonElement> e : j.entrySet()) {
                  out.put(e.getKey(), e.getValue().getAsString());
               }

               return out;
            } else {
               return null;
            }
         }
      } catch (Exception var8) {
         return null;
      }
   }

   public static boolean subscriptionActiveSelf() {
      try {
         String uuid = Platform.game().getUuid();
         if (uuid != null && !uuid.isBlank()) {
            HttpResponse<String> r = HTTP.send(
               HttpRequest.newBuilder(URI.create("http://127.0.0.1:28752/api/disabled" + uuid))
                  .GET()
                  .timeout(Duration.ofSeconds(8L))
                  .build(),
               BodyHandlers.ofString()
            );
            if (r.statusCode() / 100 != 2) {
               return false;
            } else {
               JsonObject j = new JsonParser().parse(r.body()).getAsJsonObject();
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
         HttpResponse<String> r = HTTP.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:28752/api/disabled")).GET().timeout(Duration.ofSeconds(8L)).build(),
            BodyHandlers.ofString()
         );
         return r.statusCode() / 100 != 2 ? new JsonArray() : new JsonParser().parse(r.body()).getAsJsonArray();
      } catch (Exception var1) {
         return new JsonArray();
      }
   }

   public static JsonArray ingamePartners() {
      try {
         HttpResponse<String> r = HTTP.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:28752/api/disabled")).GET().timeout(Duration.ofSeconds(8L)).build(),
            BodyHandlers.ofString()
         );
         if (r.statusCode() / 100 != 2) {
            return new JsonArray();
         } else {
            JsonObject j = new JsonParser().parse(r.body()).getAsJsonObject();
            return j.has("slots") && j.get("slots").isJsonArray() ? j.getAsJsonArray("slots") : new JsonArray();
         }
      } catch (Exception var2) {
         return new JsonArray();
      }
   }

   public static JsonArray partnerServers() {
      try {
         HttpResponse<String> r = HTTP.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:28752/api/disabled")).GET().timeout(Duration.ofSeconds(8L)).build(),
            BodyHandlers.ofString()
         );
         if (r.statusCode() / 100 != 2) {
            return new JsonArray();
         } else {
            JsonElement el = new JsonParser().parse(r.body());
            return el.isJsonArray() ? el.getAsJsonArray() : new JsonArray();
         }
      } catch (Exception var2) {
         return new JsonArray();
      }
   }

   public static byte[] texture(String id) {
      try {
         String t = ensureSession();
         if (t == null) {
            return null;
         } else {
            HttpResponse<byte[]> r = HTTP.send(
               HttpRequest.newBuilder(URI.create("http://127.0.0.1:28752/api/disabled" + id))
                  .header("Authorization", "Bearer " + t)
                  .GET()
                  .timeout(Duration.ofSeconds(15L))
                  .build(),
               BodyHandlers.ofByteArray()
            );
            return r.statusCode() / 100 != 2 ? null : r.body();
         }
      } catch (Exception var3) {
         return null;
      }
   }

   public static JsonObject petModel(String id) {
      try {
         String t = ensureSession();
         if (t == null) {
            return null;
         } else {
            HttpResponse<String> r = HTTP.send(
               HttpRequest.newBuilder(URI.create("http://127.0.0.1:28752/api/disabled" + id))
                  .header("Authorization", "Bearer " + t)
                  .GET()
                  .timeout(Duration.ofSeconds(20L))
                  .build(),
               BodyHandlers.ofString()
            );
            return r.statusCode() / 100 != 2 ? null : new JsonParser().parse(r.body()).getAsJsonObject();
         }
      } catch (Exception var3) {
         return null;
      }
   }

   public static boolean postAuthed(String path, JsonObject body) {
      HttpResponse<String> r = authedPost(path, body.toString());
      return r != null && r.statusCode() / 100 == 2;
   }

   public static JsonObject ownedSelf() {
      try {
         String t = ensureSession();
         if (t == null) {
            return null;
         } else {
            HttpResponse<String> r = HTTP.send(
               HttpRequest.newBuilder(URI.create("http://127.0.0.1:28752/api/disabled" + Platform.game().getUuid()))
                  .header("Authorization", "Bearer " + t)
                  .GET()
                  .timeout(Duration.ofSeconds(8L))
                  .build(),
               BodyHandlers.ofString()
            );
            return r.statusCode() / 100 != 2 ? null : new JsonParser().parse(r.body()).getAsJsonObject();
         }
      } catch (Exception var2) {
         return null;
      }
   }

   public static int balance() {
      try {
         String uuid = Platform.game().getUuid();
         if (uuid != null && !uuid.isBlank()) {
            HttpResponse<String> r = HTTP.send(
               HttpRequest.newBuilder(URI.create("http://127.0.0.1:28752/api/disabled" + uuid)).GET().timeout(Duration.ofSeconds(8L)).build(),
               BodyHandlers.ofString()
            );
            if (r.statusCode() / 100 != 2) {
               return -1;
            } else {
               JsonObject j = new JsonParser().parse(r.body()).getAsJsonObject();
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
      try {
         String t = ensureSession();
         if (t == null) {
            return false;
         } else {
            JsonObject b = new JsonObject();
            b.addProperty("uuid", Platform.game().getUuid());
            b.addProperty("id", id);
            HttpResponse<String> r = HTTP.send(post("http://127.0.0.1:28752/api/disabled", b.toString(), t), BodyHandlers.ofString());
            return r.statusCode() / 100 == 2;
         }
      } catch (Exception var4) {
         return false;
      }
   }

   public static boolean equipCosmetic(String id) {
      try {
         String t = ensureSession();
         if (t == null) {
            return false;
         } else {
            JsonObject b = new JsonObject();
            b.addProperty("uuid", Platform.game().getUuid());
            b.addProperty("id", id);
            HttpResponse<String> r = HTTP.send(post("http://127.0.0.1:28752/api/disabled", b.toString(), t), BodyHandlers.ofString());
            return r.statusCode() / 100 == 2;
         }
      } catch (Exception var4) {
         return false;
      }
   }

   public static boolean declareMojangCape(String url) {
      try {
         String t = ensureSession();
         if (t == null) {
            return false;
         } else {
            JsonObject b = new JsonObject();
            b.addProperty("uuid", Platform.game().getUuid());
            if (url == null) {
               b.add("url", JsonNull.INSTANCE);
            } else {
               b.addProperty("url", url);
            }

            HttpResponse<String> r = HTTP.send(post("http://127.0.0.1:28752/api/disabled", b.toString(), t), BodyHandlers.ofString());
            return r.statusCode() / 100 == 2;
         }
      } catch (Exception var4) {
         return false;
      }
   }

   public static JsonArray mojangCapes() {
      try {
         String access = Platform.game().getAccessToken();
         if (access != null && !access.isBlank()) {
            HttpResponse<String> r = HTTP.send(
               HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                  .header("Authorization", "Bearer " + access)
                  .GET()
                  .timeout(Duration.ofSeconds(8L))
                  .build(),
               BodyHandlers.ofString()
            );
            if (r.statusCode() / 100 != 2) {
               return null;
            } else {
               JsonObject j = new JsonParser().parse(r.body()).getAsJsonObject();
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
               HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile/capes/active"))
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
               HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile/capes/active"))
                  .header("Authorization", "Bearer " + access)
                  .DELETE()
                  .timeout(Duration.ofSeconds(8L))
                  .build(),
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
