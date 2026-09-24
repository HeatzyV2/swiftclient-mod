package dev.swiftclient.core.social;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.swiftclient.core.net.Backend;
import dev.swiftclient.core.platform.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Friends, direct messages and groups on the Swift backend. Every call is authenticated with the
 * backend token (the server knows who is calling, the client no longer just claims a UUID) and
 * returns a {@link Result} carrying the reason when it failed, for the UI.
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
            return new Result<>(fallback, r.message());
         } else {
            try {
               JsonElement j = r.json();
               return new Result<>(j == null ? fallback : read.apply(j), null);
            } catch (RuntimeException e) {
               return new Result<>(fallback, "Reponse illisible du serveur Swift");
            }
         }
      }
   }

   public record Friend(String uuid, String username, boolean online, String status) {
   }

   public record Group(int id, String name, String inviteCode) {
   }

   public record Message(String from, String fromName, String content, long ts) {
   }

   public static String myUuid() {
      String u = Platform.game().getUuid();
      return u != null && !u.isEmpty() ? u : null;
   }

   private static CompletableFuture<Result<Boolean>> postOk(String path, Map<String, Object> body) {
      return Backend.async(() -> Result.of(Backend.post(path, body, true), j -> true, false));
   }

   private static <T> CompletableFuture<Result<T>> get(String path, Function<JsonElement, T> read, T fallback) {
      return Backend.async(() -> Result.of(Backend.get(path, true), read, fallback));
   }

   // --- Friends ---

   public static CompletableFuture<Result<List<Friend>>> listFriends() {
      String me = myUuid();
      return me == null ? CompletableFuture.completedFuture(new Result<>(List.of(), "Aucun compte connecte")) : get("/api/friends/" + me, SocialApi::friends, List.of());
   }

   public static CompletableFuture<Result<Boolean>> requestFriendByName(String username) {
      return postOk("/api/friends/request", Map.of("from", String.valueOf(myUuid()), "to_username", username, "fromUsername", Platform.game().getUsername()));
   }

   public static CompletableFuture<Result<Boolean>> acceptFriend(String requesterUuid) {
      return postOk("/api/friends/accept", Map.of("user", String.valueOf(myUuid()), "requester", requesterUuid));
   }

   public static CompletableFuture<Result<Boolean>> declineFriend(String requesterUuid) {
      return postOk("/api/friends/decline", Map.of("user", String.valueOf(myUuid()), "requester", requesterUuid));
   }

   // --- Direct messages ---

   public static CompletableFuture<Result<List<Message>>> listDM(String otherUuid) {
      return get("/api/dm/" + myUuid() + "/" + otherUuid + "?limit=80", SocialApi::messages, List.of());
   }

   public static CompletableFuture<Result<Boolean>> sendDM(String toUuid, String content) {
      return postOk("/api/dm", Map.of("from", String.valueOf(myUuid()), "to", toUuid, "content", content));
   }

   // --- Groups ---

   public static CompletableFuture<Result<List<Group>>> listGroups() {
      return get("/api/groups/user/" + myUuid(), SocialApi::groups, List.of());
   }

   public static CompletableFuture<Result<List<Message>>> listGroupMessages(int groupId) {
      return get("/api/groups/" + groupId + "/messages?limit=80", SocialApi::messages, List.of());
   }

   public static CompletableFuture<Result<Boolean>> sendGroupMessage(int groupId, String content) {
      return postOk("/api/groups/" + groupId + "/messages", Map.of("from", String.valueOf(myUuid()), "content", content));
   }

   public static CompletableFuture<Result<Group>> createGroup(String name) {
      return Backend.async(
         () -> Result.of(Backend.post("/api/groups", Map.of("name", name, "owner", String.valueOf(myUuid())), true), j -> group(j.getAsJsonObject()), null)
      );
   }

   public static CompletableFuture<Result<Boolean>> joinGroup(String inviteCode) {
      return postOk("/api/groups/join", Map.of("invite_code", inviteCode, "uuid", String.valueOf(myUuid())));
   }

   public static CompletableFuture<Result<Boolean>> leaveGroup(int groupId) {
      return postOk("/api/groups/" + groupId + "/leave", Map.of("uuid", String.valueOf(myUuid())));
   }

   public static CompletableFuture<Result<Boolean>> addToGroup(int groupId, String targetUuid) {
      return postOk("/api/groups/" + groupId + "/add", Map.of("from", String.valueOf(myUuid()), "target", targetUuid));
   }

   // --- JSON ---

   private static String str(JsonObject o, String key) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
   }

   private static long num(JsonObject o, String key) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsLong() : 0L;
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

   private static List<Friend> friends(JsonElement j) {
      List<Friend> out = new ArrayList<>();

      for (JsonObject o : objects(j)) {
         String uuid = str(o, "uuid");
         if (uuid != null) {
            String name = str(o, "username");
            String status = str(o, "status");
            boolean online = o.has("online") && o.get("online").isJsonPrimitive() && o.get("online").getAsBoolean();
            out.add(new Friend(uuid, name != null ? name : uuid.substring(0, Math.min(8, uuid.length())), online, status != null ? status : "accepted"));
         }
      }

      return out;
   }

   private static Group group(JsonObject o) {
      int id = (int)num(o, "id");
      String name = str(o, "name");
      String code = str(o, "invite_code");
      return id <= 0 ? null : new Group(id, name != null ? name : "Groupe", code != null ? code : "");
   }

   private static List<Group> groups(JsonElement j) {
      List<Group> out = new ArrayList<>();

      for (JsonObject o : objects(j)) {
         Group g = group(o);
         if (g != null && str(o, "name") != null) {
            out.add(g);
         }
      }

      return out;
   }

   private static List<Message> messages(JsonElement j) {
      List<Message> out = new ArrayList<>();

      for (JsonObject o : objects(j)) {
         String from = str(o, "from_uuid");
         if (from == null) {
            from = str(o, "from");
         }

         String content = str(o, "content");
         if (from != null && content != null) {
            String fromName = str(o, "from_username");
            long ts = num(o, "created_at");
            if (ts == 0L) {
               ts = num(o, "ts");
            }

            out.add(new Message(from, fromName != null ? fromName : from.substring(0, Math.min(8, from.length())), content, ts));
         }
      }

      return out;
   }
}
