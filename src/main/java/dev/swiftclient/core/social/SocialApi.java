package dev.swiftclient.core.social;

import dev.swiftclient.core.platform.Tr;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.swiftclient.core.net.Backend;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Friends and direct messages on the Swift backend ({@code /api/social}, shared with the launcher).
 * Every call is authenticated with the backend token (the server knows who is calling) and returns a
 * {@link Result} carrying the reason when it failed, for the UI.
 */
public final class SocialApi {
   private SocialApi() {
   }

   /** Value of a call, or {@code error} (a sentence for the player) when it failed. */
   public record Result<T>(T value, String error) {
      public boolean ok() {
         return this.error == null;
      }

      static <T> Result<T> of(Backend.Response r, Function<JsonElement, T> read, T fallback) {
         if (!r.ok()) {
            return new Result<>(fallback, serverMessage(r));
         } else {
            try {
               JsonElement j = r.json();
               return new Result<>(j == null ? fallback : read.apply(j), null);
            } catch (RuntimeException e) {
               return new Result<>(fallback, Tr.of("swift.net.bad_reply"));
            }
         }
      }
   }

   /**
    * A friend. {@code id} is the friend entry on the backend (what conversations are opened with),
    * {@code uuid} their Minecraft UUID when known.
    */
   public record Friend(String id, String uuid, String username, boolean online, String status) {
   }

   public record Message(String from, String content, String ts) {
   }

   /** The backend's own explanation ("friend not found"...) when it gave one. */
   private static String serverMessage(Backend.Response r) {
      JsonElement j = r.status() > 0 ? r.json() : null;
      if (j != null && j.isJsonObject()) {
         String m = str(j.getAsJsonObject(), "message");
         if (m != null && !m.isBlank()) {
            return m;
         }
      }

      return r.message();
   }

   // --- Friends ---

   public static CompletableFuture<Result<List<Friend>>> listFriends() {
      return Backend.async(() -> Result.of(Backend.get("/api/social/friends", true), SocialApi::friends, List.of()));
   }

   /** Adds a friend by Swift username or Minecraft name. */
   public static CompletableFuture<Result<Boolean>> requestFriendByName(String username) {
      return Backend.async(() -> Result.of(Backend.post("/api/social/friends/request", Map.of("username", username), true), j -> true, false));
   }

   public static CompletableFuture<Result<Boolean>> removeFriend(Friend friend) {
      return Backend.async(() -> Result.of(Backend.post("/api/social/friends/" + friend.id() + "/remove", Map.of(), true), j -> true, false));
   }

   // --- Direct messages ---

   /** Conversation id with this friend, opened on first use. */
   private static Result<String> conversation(Friend friend) {
      return Result.of(Backend.post("/api/social/conversations", Map.of("friend_id", friend.id()), true), j -> str(j.getAsJsonObject(), "id"), null);
   }

   public static CompletableFuture<Result<List<Message>>> listDM(Friend friend) {
      return Backend.async(() -> {
         Result<String> conv = conversation(friend);
         return conv.value() == null
            ? new Result<>(List.of(), conv.error() != null ? conv.error() : Tr.of("swift.net.bad_reply"))
            : Result.of(Backend.get("/api/social/conversations/" + conv.value() + "/messages?limit=80", true), SocialApi::messages, List.of());
      });
   }

   public static CompletableFuture<Result<Boolean>> sendDM(Friend friend, String content) {
      return Backend.async(() -> {
         Result<String> conv = conversation(friend);
         return conv.value() == null
            ? new Result<>(false, conv.error() != null ? conv.error() : Tr.of("swift.net.bad_reply"))
            : Result.of(Backend.post("/api/social/conversations/" + conv.value() + "/messages", Map.of("text", content), true), j -> true, false);
      });
   }

   // --- JSON ---

   private static String str(JsonObject o, String key) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
   }

   private static List<JsonObject> objects(JsonElement j) {
      List<JsonObject> out = new ArrayList<>();
      if (j != null && j.isJsonArray()) {
         for (JsonElement e : (JsonArray)j) {
            if (e.isJsonObject()) {
               out.add(e.getAsJsonObject());
            }
         }
      }

      return out;
   }

   static List<Friend> friends(JsonElement j) {
      List<Friend> out = new ArrayList<>();

      for (JsonObject o : objects(j)) {
         String id = str(o, "id");
         String name = str(o, "username");
         if (id != null && name != null) {
            String status = str(o, "status");
            boolean online = o.has("online") && o.get("online").isJsonPrimitive() && o.get("online").getAsBoolean();
            out.add(new Friend(id, str(o, "uuid"), name, online, status != null ? status : "accepted"));
         }
      }

      return out;
   }

   static List<Message> messages(JsonElement j) {
      List<Message> out = new ArrayList<>();

      for (JsonObject o : objects(j)) {
         String from = str(o, "from");
         String text = str(o, "text");
         if (from != null && text != null) {
            String ts = str(o, "created_at");
            out.add(new Message(from, text, ts != null ? ts : ""));
         }
      }

      return out;
   }
}
