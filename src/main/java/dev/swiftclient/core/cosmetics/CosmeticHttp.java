package dev.swiftclient.core.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.swiftclient.core.net.Backend;
import dev.swiftclient.core.net.Net;
import dev.swiftclient.core.platform.Platform;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/** Cosmetics, badges and shop calls to the Swift backend, plus the Mojang cape endpoints. */
public final class CosmeticHttp {
   private static final String MC_PROFILE = "https://api.minecraftservices.com/minecraft/profile";
   private static final String MC_CAPE_ACTIVE = "https://api.minecraftservices.com/minecraft/profile/capes/active";

   private CosmeticHttp() {
   }

   /** True only when a backend URL is configured. Every backend call is skipped otherwise. */
   public static boolean backendConfigured() {
      return Backend.enabled();
   }

   private static JsonObject batchBody(List<String> uuids) {
      JsonArray arr = new JsonArray();
      JsonObject names = new JsonObject();

      for (String u : uuids) {
         arr.add(u);
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

   private static JsonObject self() {
      JsonObject b = new JsonObject();
      b.addProperty("uuid", Platform.game().getUuid());
      return b;
   }

   /** Batch lookup returning {uuid: value}, or null when the call failed. */
   private static <V> Map<String, V> batch(String path, List<String> uuids, Function<JsonElement, V> value) {
      Backend.Response r = Backend.post(path, batchBody(uuids), true);
      JsonElement j = r.ok() ? r.json() : null;
      if (j == null || !j.isJsonObject()) {
         return null;
      } else {
         Map<String, V> out = new HashMap<>();

         for (Entry<String, JsonElement> e : j.getAsJsonObject().entrySet()) {
            try {
               out.put(e.getKey(), value.apply(e.getValue()));
            } catch (RuntimeException ignored) {
            }
         }

         return out;
      }
   }

   private static JsonArray array(Backend.Response r) {
      JsonElement j = r.ok() ? r.json() : null;
      return j != null && j.isJsonArray() ? j.getAsJsonArray() : new JsonArray();
   }

   private static JsonObject object(Backend.Response r) {
      JsonElement j = r.ok() ? r.json() : null;
      return j != null && j.isJsonObject() ? j.getAsJsonObject() : null;
   }

   // --- Per-player state ---

   public static Map<String, String> equippedFor(List<String> uuids) {
      Map<String, String> m = batch("/api/cosmetics/equipped", uuids, JsonElement::getAsString);
      return m == null ? Map.of() : m;
   }

   public static Map<String, Boolean> animatedFor(List<String> uuids) {
      Map<String, Boolean> m = batch("/api/cosmetics/animated", uuids, JsonElement::getAsBoolean);
      return m == null ? Map.of() : m;
   }

   public static Map<String, String> petsFor(List<String> uuids) {
      Map<String, String> m = batch("/api/cosmetics/pets", uuids, JsonElement::getAsString);
      return m == null ? Map.of() : m;
   }

   /** Null when the backend could not be reached (callers retry sooner in that case). */
   public static Map<String, String> gradesFor(List<String> uuids) {
      return batch("/api/users/grades", uuids, JsonElement::getAsString);
   }

   public static boolean heartbeat() {
      return Backend.post("/api/users/heartbeat", self(), true).ok();
   }

   public static boolean subscriptionActiveSelf() {
      String uuid = Platform.game().getUuid();
      if (uuid == null || uuid.isBlank()) {
         return false;
      } else {
         JsonObject j = object(Backend.get("/api/subscription/status/" + uuid, false));
         return j != null && j.has("active") && j.get("active").getAsBoolean();
      }
   }

   // --- Catalogue, assets ---

   public static JsonArray catalog() {
      return array(Backend.get("/api/cosmetics/catalog", false));
   }

   public static JsonArray ingamePartners() {
      JsonObject j = object(Backend.get("/api/partners/ingame", false));
      return j != null && j.has("slots") && j.get("slots").isJsonArray() ? j.getAsJsonArray("slots") : new JsonArray();
   }

   public static JsonArray partnerServers() {
      return array(Backend.get("/api/servers", false));
   }

   public static byte[] texture(String id) {
      return Backend.getBytes("/api/cosmetics/texture/" + id, true);
   }

   public static JsonObject petModel(String id) {
      return object(Backend.get("/api/cosmetics/pet-model/" + id, true));
   }

   public static boolean postAuthed(String path, JsonObject body) {
      return Backend.post(path, body, true).ok();
   }

   // --- Wardrobe and shop: these return the full response so the screen can show why it failed ---

   public static Backend.Response ownedSelfResponse() {
      return Backend.get("/api/cosmetics/owned/" + Platform.game().getUuid(), true);
   }

   public static JsonObject ownedSelf() {
      return object(ownedSelfResponse());
   }

   public static int balance() {
      String uuid = Platform.game().getUuid();
      if (uuid == null || uuid.isBlank()) {
         return -1;
      } else {
         JsonObject j = object(Backend.get("/api/shop/balance/" + uuid, false));
         return j != null && j.has("coins") ? j.get("coins").getAsInt() : -1;
      }
   }

   private static Backend.Response withId(String path, String id) {
      JsonObject b = self();
      b.addProperty("id", id);
      return Backend.post(path, b, true);
   }

   public static Backend.Response buyCosmetic(String id) {
      return withId("/api/cosmetics/buy", id);
   }

   public static Backend.Response equipCosmetic(String id) {
      return withId("/api/cosmetics/equip", id);
   }

   public static Backend.Response equipPet(String id) {
      return withId("/api/cosmetics/equip-pet", id);
   }

   public static boolean setCapeAnimated(boolean enabled) {
      JsonObject b = self();
      b.addProperty("enabled", enabled);
      return Backend.post("/api/cosmetics/cape-animated", b, true).ok();
   }

   public static Backend.Response declareMojangCape(String url) {
      JsonObject b = self();
      if (url == null) {
         b.add("url", JsonNull.INSTANCE);
      } else {
         b.addProperty("url", url);
      }

      return Backend.post("/api/cosmetics/mojang-cape", b, true);
   }

   // --- Mojang (api.minecraftservices.com), authenticated with the game's own access token ---

   private static HttpResponse<String> mojang(HttpRequest.Builder request) {
      String access = Platform.game().getAccessToken();
      if (access == null || access.isBlank()) {
         return null;
      } else {
         try {
            return Net.HTTP.send(request.header("Authorization", "Bearer " + access).timeout(Duration.ofSeconds(8L)).build(), BodyHandlers.ofString());
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
         } catch (Exception e) {
            return null;
         }
      }
   }

   public static JsonArray mojangCapes() {
      HttpResponse<String> r = mojang(HttpRequest.newBuilder(URI.create(MC_PROFILE)).GET());
      if (r == null || r.statusCode() / 100 != 2) {
         return null;
      } else {
         try {
            JsonObject j = JsonParser.parseString(r.body()).getAsJsonObject();
            return j.has("capes") ? j.getAsJsonArray("capes") : new JsonArray();
         } catch (RuntimeException e) {
            return null;
         }
      }
   }

   public static boolean setMojangCapeActive(String capeId) {
      JsonObject b = new JsonObject();
      b.addProperty("capeId", capeId);
      HttpResponse<String> r = mojang(
         HttpRequest.newBuilder(URI.create(MC_CAPE_ACTIVE)).header("Content-Type", "application/json").method("PUT", BodyPublishers.ofString(b.toString()))
      );
      return r != null && r.statusCode() / 100 == 2;
   }

   public static boolean disableMojangCape() {
      HttpResponse<String> r = mojang(HttpRequest.newBuilder(URI.create(MC_CAPE_ACTIVE)).DELETE());
      return r != null && r.statusCode() / 100 == 2;
   }

   public static byte[] rawTexture(String url) {
      if (url == null || !url.matches("^https?://textures\\.minecraft\\.net/texture/[0-9a-fA-F]+$")) {
         return null;
      } else {
         try {
            HttpResponse<byte[]> r = Net.HTTP.send(
               HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(15L)).build(), BodyHandlers.ofByteArray()
            );
            return r.statusCode() / 100 != 2 ? null : r.body();
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
         } catch (Exception e) {
            return null;
         }
      }
   }
}
