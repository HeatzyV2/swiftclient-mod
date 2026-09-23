package dev.swiftclient.core.social;

import dev.swiftclient.core.platform.Platform;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SocialApi {
   private static final String API = "https://api.swiftclient.dev";
   private static final String CDN = "https://cdn.swiftclient.dev";
   private static final HttpClient HTTP = HttpClient.newHttpClient();

   public static String myUuid() {
      String u = Platform.game().getUuid();
      return u != null && !u.isEmpty() ? u : null;
   }

   public static String myName() {
      return Platform.game().getUsername();
   }

   public static CompletableFuture<List<SocialApi.Friend>> listFriends() {
      String me = myUuid();
      return me == null
         ? CompletableFuture.completedFuture(List.of())
         : get("https://api.swiftclient.dev/api/friends/" + me).thenApply(SocialApi::parseFriends).exceptionally(e -> List.of());
   }

   public static CompletableFuture<Boolean> requestFriendByName(String username) {
      String me = myUuid();
      if (me == null) {
         return CompletableFuture.completedFuture(false);
      } else {
         String json = "{\"from\":\"" + me + "\",\"to_username\":" + jstr(username) + ",\"fromUsername\":" + jstr(myName()) + "}";
         return post("https://api.swiftclient.dev/api/friends/request", json).thenApply(r -> r.statusCode() < 300).exceptionally(e -> false);
      }
   }

   public static CompletableFuture<Boolean> acceptFriend(String requesterUuid) {
      String me = myUuid();
      if (me == null) {
         return CompletableFuture.completedFuture(false);
      } else {
         String json = "{\"user\":\"" + me + "\",\"requester\":\"" + requesterUuid + "\"}";
         return post("https://api.swiftclient.dev/api/friends/accept", json).thenApply(r -> r.statusCode() < 300).exceptionally(e -> false);
      }
   }

   public static CompletableFuture<Boolean> declineFriend(String requesterUuid) {
      String me = myUuid();
      if (me == null) {
         return CompletableFuture.completedFuture(false);
      } else {
         String json = "{\"user\":\"" + me + "\",\"requester\":\"" + requesterUuid + "\"}";
         return post("https://api.swiftclient.dev/api/friends/decline", json).thenApply(r -> r.statusCode() < 300).exceptionally(e -> false);
      }
   }

   public static CompletableFuture<List<SocialApi.Message>> listDM(String otherUuid) {
      String me = myUuid();
      return me == null
         ? CompletableFuture.completedFuture(List.of())
         : get("https://api.swiftclient.dev/api/dm/" + me + "/" + otherUuid + "?limit=80").thenApply(SocialApi::parseMessages).exceptionally(e -> List.of());
   }

   public static CompletableFuture<Boolean> sendDM(String toUuid, String content) {
      String me = myUuid();
      if (me == null) {
         return CompletableFuture.completedFuture(false);
      } else {
         String json = "{\"from\":\"" + me + "\",\"to\":\"" + toUuid + "\",\"content\":" + jstr(content) + "}";
         return post("https://api.swiftclient.dev/api/dm", json).thenApply(r -> r.statusCode() < 300).exceptionally(e -> false);
      }
   }

   public static CompletableFuture<List<SocialApi.Group>> listGroups() {
      String me = myUuid();
      return me == null
         ? CompletableFuture.completedFuture(List.of())
         : get("https://api.swiftclient.dev/api/groups/user/" + me).thenApply(SocialApi::parseGroups).exceptionally(e -> List.of());
   }

   public static CompletableFuture<List<SocialApi.Message>> listGroupMessages(int groupId) {
      return get("https://api.swiftclient.dev/api/groups/" + groupId + "/messages?limit=80").thenApply(SocialApi::parseMessages).exceptionally(e -> List.of());
   }

   public static CompletableFuture<Boolean> sendGroupMessage(int groupId, String content) {
      String me = myUuid();
      if (me == null) {
         return CompletableFuture.completedFuture(false);
      } else {
         String json = "{\"from\":\"" + me + "\",\"content\":" + jstr(content) + "}";
         return post("https://api.swiftclient.dev/api/groups/" + groupId + "/messages", json).thenApply(r -> r.statusCode() < 300).exceptionally(e -> false);
      }
   }

   public static CompletableFuture<SocialApi.Group> createGroup(String name) {
      String me = myUuid();
      if (me == null) {
         return CompletableFuture.completedFuture(null);
      } else {
         String json = "{\"name\":" + jstr(name) + ",\"owner\":\"" + me + "\"}";
         return post("https://api.swiftclient.dev/api/groups", json)
            .thenApply(r -> r.statusCode() >= 300 ? null : parseGroupSingle(r.body()))
            .exceptionally(e -> null);
      }
   }

   public static CompletableFuture<Boolean> joinGroup(String inviteCode) {
      String me = myUuid();
      if (me == null) {
         return CompletableFuture.completedFuture(false);
      } else {
         String json = "{\"invite_code\":" + jstr(inviteCode) + ",\"uuid\":\"" + me + "\"}";
         return post("https://api.swiftclient.dev/api/groups/join", json).thenApply(r -> r.statusCode() < 300).exceptionally(e -> false);
      }
   }

   public static CompletableFuture<Boolean> leaveGroup(int groupId) {
      String me = myUuid();
      if (me == null) {
         return CompletableFuture.completedFuture(false);
      } else {
         String json = "{\"uuid\":\"" + me + "\"}";
         return post("https://api.swiftclient.dev/api/groups/" + groupId + "/leave", json).thenApply(r -> r.statusCode() < 300).exceptionally(e -> false);
      }
   }

   public static CompletableFuture<Boolean> addToGroup(int groupId, String targetUuid) {
      String me = myUuid();
      if (me == null) {
         return CompletableFuture.completedFuture(false);
      } else {
         String json = "{\"from\":\"" + me + "\",\"target\":\"" + targetUuid + "\"}";
         return post("https://api.swiftclient.dev/api/groups/" + groupId + "/add", json).thenApply(r -> r.statusCode() < 300).exceptionally(e -> false);
      }
   }

   public static CompletableFuture<String> uploadImage(Path file) {
      return CompletableFuture.supplyAsync(
         () -> {
            try {
               byte[] data = Files.readAllBytes(file);
               String boundary = "----LC" + System.currentTimeMillis();
               String head = "--"
                  + boundary
                  + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                  + file.getFileName()
                  + "\"\r\nContent-Type: image/png\r\n\r\n";
               String tail = "\r\n--" + boundary + "--\r\n";
               byte[] body = concat(head.getBytes(), data, tail.getBytes());
               HttpRequest req = HttpRequest.newBuilder(URI.create("https://cdn.swiftclient.dev/upload"))
                  .timeout(Duration.ofSeconds(30L))
                  .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                  .POST(BodyPublishers.ofByteArray(body))
                  .build();
               HttpResponse<String> r = HTTP.send(req, BodyHandlers.ofString());
               if (r.statusCode() < 300) {
                  String b = r.body();
                  int i = b.indexOf("\"url\"");
                  if (i >= 0) {
                     int q1 = b.indexOf(34, i + 6);
                     int q2 = b.indexOf(34, q1 + 1);
                     if (q1 > 0 && q2 > q1) {
                        return b.substring(q1 + 1, q2);
                     }
                  }
               }

               return null;
            } catch (Exception var12) {
               return null;
            }
         }
      );
   }

   public static CompletableFuture<Boolean> sendImageDM(String toUuid, String imageUrl) {
      return sendDM(toUuid, imageUrl);
   }

   private static CompletableFuture<String> get(String url) {
      HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5L)).GET().build();
      return HTTP.sendAsync(req, BodyHandlers.ofString()).thenApply(HttpResponse::body);
   }

   private static CompletableFuture<HttpResponse<String>> post(String url, String json) {
      HttpRequest req = HttpRequest.newBuilder(URI.create(url))
         .timeout(Duration.ofSeconds(5L))
         .header("Content-Type", "application/json")
         .POST(BodyPublishers.ofString(json))
         .build();
      return HTTP.sendAsync(req, BodyHandlers.ofString());
   }

   private static String jstr(String s) {
      if (s == null) {
         return "\"\"";
      } else {
         StringBuilder sb = new StringBuilder("\"");

         for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
               case '\t':
                  sb.append("\\t");
                  break;
               case '\n':
                  sb.append("\\n");
                  break;
               case '\r':
                  sb.append("\\r");
                  break;
               case '"':
                  sb.append("\\\"");
                  break;
               case '\\':
                  sb.append("\\\\");
                  break;
               default:
                  if (c < ' ') {
                     sb.append(String.format("\\u%04x", Integer.valueOf(c)));
                  } else {
                     sb.append(c);
                  }
            }
         }

         return sb.append("\"").toString();
      }
   }

   private static List<SocialApi.Friend> parseFriends(String body) {
      List<SocialApi.Friend> out = new ArrayList<>();
      if (body != null && body.startsWith("[")) {
         for (String obj : splitObjects(body)) {
            String uuid = grab(obj, "uuid");
            String username = grab(obj, "username");
            String status = grab(obj, "status");
            boolean online = obj.contains("\"online\":true");
            if (uuid != null) {
               out.add(new SocialApi.Friend(uuid, username == null ? uuid.substring(0, 8) : username, online, status == null ? "accepted" : status));
            }
         }

         return out;
      } else {
         return out;
      }
   }

   private static List<SocialApi.Group> parseGroups(String body) {
      List<SocialApi.Group> out = new ArrayList<>();
      if (body != null && body.startsWith("[")) {
         for (String obj : splitObjects(body)) {
            int id = grabInt(obj, "id");
            String name = grab(obj, "name");
            String code = grab(obj, "invite_code");
            if (id > 0 && name != null) {
               out.add(new SocialApi.Group(id, name, code == null ? "" : code));
            }
         }

         return out;
      } else {
         return out;
      }
   }

   private static SocialApi.Group parseGroupSingle(String body) {
      if (body == null) {
         return null;
      } else {
         int id = grabInt(body, "id");
         String name = grab(body, "name");
         String code = grab(body, "invite_code");
         return id <= 0 ? null : new SocialApi.Group(id, name == null ? "Groupe" : name, code == null ? "" : code);
      }
   }

   private static List<SocialApi.Message> parseMessages(String body) {
      List<SocialApi.Message> out = new ArrayList<>();
      if (body != null && body.startsWith("[")) {
         for (String obj : splitObjects(body)) {
            String from = grab(obj, "from_uuid");
            if (from == null) {
               from = grab(obj, "from");
            }

            String fromName = grab(obj, "from_username");
            String content = grab(obj, "content");
            long ts = grabLong(obj, "created_at");
            if (ts == 0L) {
               ts = grabLong(obj, "ts");
            }

            if (from != null && content != null) {
               out.add(new SocialApi.Message(from, fromName == null ? from.substring(0, 8) : fromName, content, ts));
            }
         }

         return out;
      } else {
         return out;
      }
   }

   private static List<String> splitObjects(String body) {
      List<String> out = new ArrayList<>();
      int depth = 0;
      int start = -1;

      for (int i = 0; i < body.length(); i++) {
         char c = body.charAt(i);
         if (c == '{') {
            if (depth == 0) {
               start = i;
            }

            depth++;
         } else if (c == '}') {
            if (--depth == 0 && start >= 0) {
               out.add(body.substring(start, i + 1));
               start = -1;
            }
         }
      }

      return out;
   }

   private static String grab(String obj, String key) {
      int i = obj.indexOf("\"" + key + "\"");
      if (i < 0) {
         return null;
      } else {
         int colon = obj.indexOf(58, i);
         int q1 = obj.indexOf(34, colon + 1);
         if (q1 < 0) {
            return null;
         } else if (q1 > colon + 4) {
            return null;
         } else {
            StringBuilder sb = new StringBuilder();

            for (int j = q1 + 1; j < obj.length(); j++) {
               char c = obj.charAt(j);
               if (c == '\\' && j + 1 < obj.length()) {
                  char n = obj.charAt(j + 1);
                  switch (n) {
                     case '"':
                        sb.append('"');
                        break;
                     case '\\':
                        sb.append('\\');
                        break;
                     case 'n':
                        sb.append('\n');
                        break;
                     case 'r':
                        sb.append('\r');
                        break;
                     case 't':
                        sb.append('\t');
                        break;
                     default:
                        sb.append(n);
                  }

                  j++;
               } else {
                  if (c == '"') {
                     return sb.toString();
                  }

                  sb.append(c);
               }
            }

            return sb.toString();
         }
      }
   }

   private static int grabInt(String obj, String key) {
      try {
         int i = obj.indexOf("\"" + key + "\"");
         if (i < 0) {
            return 0;
         } else {
            int colon = obj.indexOf(58, i);
            int j = colon + 1;

            while (j < obj.length() && Character.isWhitespace(obj.charAt(j))) {
               j++;
            }

            int s = j;

            while (j < obj.length() && (Character.isDigit(obj.charAt(j)) || obj.charAt(j) == '-')) {
               j++;
            }

            return s == j ? 0 : Integer.parseInt(obj.substring(s, j));
         }
      } catch (Exception var6) {
         return 0;
      }
   }

   private static long grabLong(String obj, String key) {
      try {
         int i = obj.indexOf("\"" + key + "\"");
         if (i < 0) {
            return 0L;
         } else {
            int colon = obj.indexOf(58, i);
            int j = colon + 1;

            while (j < obj.length() && Character.isWhitespace(obj.charAt(j))) {
               j++;
            }

            int s = j;

            while (j < obj.length() && (Character.isDigit(obj.charAt(j)) || obj.charAt(j) == '-')) {
               j++;
            }

            return s == j ? 0L : Long.parseLong(obj.substring(s, j));
         }
      } catch (Exception var6) {
         return 0L;
      }
   }

   private static byte[] concat(byte[]... arrs) {
      int total = 0;

      for (byte[] a : arrs) {
         total += a.length;
      }

      byte[] out = new byte[total];
      int p = 0;

      for (byte[] a : arrs) {
         System.arraycopy(a, 0, out, p, a.length);
         p += a.length;
      }

      return out;
   }

   public record Friend(String uuid, String username, boolean online, String status) {
   }

   public record Group(int id, String name, String inviteCode) {
   }

   public record Message(String from, String fromName, String content, long ts) {
   }
}
