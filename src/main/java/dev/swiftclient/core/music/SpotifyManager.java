package dev.swiftclient.core.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.platform.Platform;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SpotifyManager {
   private static final int PORT = 8888;
   public static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
   private static final String SCOPE = "user-read-playback-state user-read-currently-playing";
   private static volatile String clientId = "";
   private static volatile String clientSecret = "";
   private static volatile String accessToken = "";
   private static volatile String refreshToken = "";
   private static volatile long expiresAt = 0L;
   private static volatile boolean authorized = false;
   private static volatile String nextTitle = "";
   private static volatile String nextArtist = "";
   private static volatile Object nextArtHandle = null;
   private static volatile String lastNextArtUrl = "";
   private static volatile String tempId;
   private static volatile String tempSecret;
   private static HttpServer server;
   private static boolean started = false;

   private SpotifyManager() {
   }

   public static boolean authorized() {
      return authorized;
   }

   public static boolean hasNext() {
      return authorized && !nextTitle.isBlank();
   }

   public static String nextTitle() {
      return nextTitle;
   }

   public static String nextArtist() {
      return nextArtist;
   }

   public static Object nextArtHandle() {
      return nextArtHandle;
   }

   public static synchronized void ensureStarted() {
      if (!started) {
         started = true;

         try {
            clientId = cfg("spotify.clientId");
            clientSecret = cfg("spotify.clientSecret");
            accessToken = cfg("spotify.accessToken");
            refreshToken = cfg("spotify.refreshToken");

            try {
               expiresAt = Long.parseLong(cfg("spotify.expiresAt"));
            } catch (Throwable var1) {
            }

            authorized = !accessToken.isBlank() && !refreshToken.isBlank();
         } catch (Throwable var2) {
         }

         ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "swiftclient-spotify");
            t.setDaemon(true);
            return t;
         });
         exec.scheduleAtFixedRate(SpotifyManager::pollSafe, 2L, 5L, TimeUnit.SECONDS);
      }
   }

   public static synchronized void connect() {
      ensureStarted();
      startServer();
      openBrowser("http://127.0.0.1:8888/setup");
   }

   private static void openBrowser(String url) {
      try {
         String os = System.getProperty("os.name", "").toLowerCase();
         ProcessBuilder pb;
         if (os.contains("win")) {
            pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
         } else if (os.contains("mac")) {
            pb = new ProcessBuilder("open", url);
         } else {
            pb = new ProcessBuilder("xdg-open", url);
         }

         pb.start();
      } catch (Throwable var3) {
      }
   }

   public static synchronized void disconnect() {
      authorized = false;
      refreshToken = "";
      accessToken = "";
      nextArtist = "";
      nextTitle = "";
      nextArtHandle = null;
      lastNextArtUrl = "";
      setCfg("spotify.accessToken", "");
      setCfg("spotify.refreshToken", "");
      setCfg("spotify.expiresAt", "0");
   }

   private static synchronized void startServer() {
      if (server == null) {
         try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8888), 0);
            server.createContext("/setup", ex -> respondHtml(ex, 200, setupPage()));
            server.createContext(
               "/submit",
               ex -> {
                  Map<String, String> p = parseQuery(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                  String cid = p.getOrDefault("clientId", "").trim();
                  String csec = p.getOrDefault("clientSecret", "").trim();
                  if (!cid.isBlank() && !csec.isBlank()) {
                     tempId = cid;
                     tempSecret = csec;
                     String authUrl = "https://accounts.spotify.com/authorize?client_id="
                        + enc(cid)
                        + "&response_type=code&redirect_uri="
                        + enc("http://127.0.0.1:8888/callback")
                        + "&scope="
                        + enc("user-read-playback-state user-read-currently-playing");
                     ex.getResponseHeaders().set("Location", authUrl);
                     ex.sendResponseHeaders(303, -1L);
                  } else {
                     respondHtml(ex, 400, messagePage("Missing credentials", "Client ID and Client Secret are required.", false));
                  }
               }
            );
            server.createContext(
               "/callback",
               ex -> {
                  Map<String, String> q = parseQuery(ex.getRequestURI().getQuery());
                  String code = q.get("code");
                  if (code != null && !code.isBlank() && tempId != null) {
                     boolean ok = exchangeCode(code, tempId, tempSecret);
                     respondHtml(
                        ex,
                        ok ? 200 : 400,
                        ok
                           ? messagePage("Connected", "Spotify is now linked. You can close this tab and go back to the game.", true)
                           : messagePage("Authorization failed", "Could not exchange the code. Check your Client ID and Secret.", false)
                     );
                     if (ok) {
                        new Thread(() -> {
                           sleep(1200L);
                           stopServer();
                        }).start();
                     }
                  } else {
                     respondHtml(ex, 400, messagePage("Authorization failed", q.getOrDefault("error", "No code received."), false));
                  }
               }
            );
            server.start();
         } catch (Throwable var1) {
            server = null;
         }
      }
   }

   private static synchronized void stopServer() {
      if (server != null) {
         try {
            server.stop(0);
         } catch (Throwable var1) {
         }

         server = null;
      }
   }

   private static boolean exchangeCode(String code, String cid, String csec) {
      try {
         String body = "grant_type=authorization_code&code="
            + enc(code)
            + "&redirect_uri="
            + enc("http://127.0.0.1:8888/callback")
            + "&client_id="
            + enc(cid)
            + "&client_secret="
            + enc(csec);
         JsonObject j = postForm("https://accounts.spotify.com/api/token", body);
         if (j != null && j.has("access_token")) {
            accessToken = j.get("access_token").getAsString();
            if (j.has("refresh_token")) {
               refreshToken = j.get("refresh_token").getAsString();
            }

            expiresAt = System.currentTimeMillis() + j.get("expires_in").getAsLong() * 1000L;
            clientId = cid;
            clientSecret = csec;
            authorized = true;
            setCfg("spotify.clientId", cid);
            setCfg("spotify.clientSecret", csec);
            setCfg("spotify.accessToken", accessToken);
            setCfg("spotify.refreshToken", refreshToken);
            setCfg("spotify.expiresAt", Long.toString(expiresAt));
            return true;
         } else {
            return false;
         }
      } catch (Throwable var5) {
         return false;
      }
   }

   private static void refresh() {
      if (refreshToken.isBlank()) {
         authorized = false;
      } else {
         try {
            String body = "grant_type=refresh_token&refresh_token=" + enc(refreshToken) + "&client_id=" + enc(clientId) + "&client_secret=" + enc(clientSecret);
            JsonObject j = postForm("https://accounts.spotify.com/api/token", body);
            if (j == null || !j.has("access_token")) {
               return;
            }

            accessToken = j.get("access_token").getAsString();
            if (j.has("refresh_token")) {
               refreshToken = j.get("refresh_token").getAsString();
            }

            expiresAt = System.currentTimeMillis() + j.get("expires_in").getAsLong() * 1000L;
            setCfg("spotify.accessToken", accessToken);
            setCfg("spotify.refreshToken", refreshToken);
            setCfg("spotify.expiresAt", Long.toString(expiresAt));
         } catch (Throwable var2) {
         }
      }
   }

   private static void pollSafe() {
      try {
         poll();
      } catch (Throwable var1) {
      }
   }

   private static void poll() throws Exception {
      if (authorized && !accessToken.isBlank() && ModuleManager.active("hud_music")) {
         if (System.currentTimeMillis() + 60000L > expiresAt) {
            refresh();
         }

         pollCurrentlyPlaying();
         pollQueue();
      }
   }

   private static void pollCurrentlyPlaying() throws Exception {
      HttpResponse<String> res = HttpClient.newHttpClient()
         .send(
            HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/currently-playing"))
               .header("Authorization", "Bearer " + accessToken)
               .GET()
               .build(),
            BodyHandlers.ofString()
         );
      if (res.statusCode() == 401) {
         refresh();
         return;
      }
      if (res.statusCode() == 204 || res.body() == null || res.body().isBlank()) {
         return;
      }
      if (res.statusCode() != 200) {
         return;
      }
      JsonObject j = new JsonParser().parse(res.body()).getAsJsonObject();
      if (!j.has("item") || j.get("item").isJsonNull()) {
         return;
      }
      JsonObject item = j.getAsJsonObject("item");
      String title = str(item, "name");
      String artist = artists(item);
      boolean playing = j.has("is_playing") && j.get("is_playing").getAsBoolean();
      long progress = j.has("progress_ms") ? j.get("progress_ms").getAsLong() : 0L;
      long duration = item.has("duration_ms") ? item.get("duration_ms").getAsLong() : 0L;
      String tid = item.has("id") && !item.get("id").isJsonNull() ? item.get("id").getAsString() : title + "|" + artist;
      String artUrl = artUrl(item);
      String artPath = "";
      if (!artUrl.isBlank()) {
         artPath = cacheArtFile(tid, artUrl);
      }
      MusicState.apply(title, artist, playing, progress, duration, tid, artPath, !artPath.isBlank());
   }

   private static String cacheArtFile(String tid, String url) {
      try {
         Path dir = Path.of(System.getProperty("java.io.tmpdir"), "swiftclient");
         Files.createDirectories(dir);
         Path file = dir.resolve("spotify-art-" + tid.replaceAll("[^a-zA-Z0-9_-]", "_") + ".img");
         if (Files.exists(file) && Files.size(file) > 0L) {
            return file.toString();
         }
         HttpResponse<byte[]> res = HttpClient.newHttpClient()
            .send(HttpRequest.newBuilder(URI.create(url)).GET().build(), BodyHandlers.ofByteArray());
         if (res.statusCode() == 200 && res.body() != null && res.body().length > 0) {
            Files.write(file, res.body());
            return file.toString();
         }
      } catch (Throwable ignored) {
      }
      return "";
   }

   private static void pollQueue() throws Exception {
      HttpResponse<String> res = HttpClient.newHttpClient()
         .send(
            HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/queue")).header("Authorization", "Bearer " + accessToken).GET().build(),
            BodyHandlers.ofString()
         );
      if (res.statusCode() == 401) {
         refresh();
      } else if (res.statusCode() != 200) {
         clearNext();
      } else {
         JsonObject j = new JsonParser().parse(res.body()).getAsJsonObject();
         if (j.has("queue") && j.get("queue").isJsonArray()) {
            JsonArray queue = j.getAsJsonArray("queue");
            if (queue.size() == 0) {
               clearNext();
            } else {
               JsonObject item = queue.get(0).getAsJsonObject();
               nextTitle = str(item, "name");
               nextArtist = artists(item);
               String artUrl = artUrl(item);
               if (!artUrl.isBlank() && !artUrl.equals(lastNextArtUrl)) {
                  lastNextArtUrl = artUrl;
                  loadArt(artUrl);
               } else if (artUrl.isBlank()) {
                  nextArtHandle = null;
                  lastNextArtUrl = "";
               }
            }
         } else {
            clearNext();
         }
      }
   }

   private static void clearNext() {
      nextTitle = "";
      nextArtist = "";
      nextArtHandle = null;
      lastNextArtUrl = "";
   }

   private static void loadArt(String url) {
      try {
         byte[] png = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url)).GET().build(), BodyHandlers.ofByteArray()).body();
         if (png == null || png.length == 0) {
            return;
         }

         Platform.game().loadImage("spotify/next", png, (k, h) -> nextArtHandle = h);
      } catch (Throwable var2) {
      }
   }

   private static JsonObject postForm(String url, String body) throws Exception {
      HttpResponse<String> res = HttpClient.newHttpClient()
         .send(
            HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/x-www-form-urlencoded").POST(BodyPublishers.ofString(body)).build(),
            BodyHandlers.ofString()
         );
      return res.statusCode() != 200 ? null : new JsonParser().parse(res.body()).getAsJsonObject();
   }

   private static String artists(JsonObject item) {
      if (!item.has("artists")) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder();

         for (JsonElement e : item.getAsJsonArray("artists")) {
            if (sb.length() > 0) {
               sb.append(", ");
            }

            sb.append(e.getAsJsonObject().get("name").getAsString());
         }

         return sb.toString();
      }
   }

   private static String artUrl(JsonObject item) {
      if (item.has("album") && item.getAsJsonObject("album").has("images")) {
         JsonArray imgs = item.getAsJsonObject("album").getAsJsonArray("images");
         if (imgs.size() > 0) {
            return imgs.get(0).getAsJsonObject().get("url").getAsString();
         }
      }

      return "";
   }

   private static String str(JsonObject j, String k) {
      return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsString() : "";
   }

   private static Map<String, String> parseQuery(String q) {
      Map<String, String> m = new HashMap<>();
      if (q != null && !q.isBlank()) {
         for (String p : q.split("&")) {
            int i = p.indexOf(61);
            if (i > 0) {
               try {
                  m.put(URLDecoder.decode(p.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(p.substring(i + 1), StandardCharsets.UTF_8));
               } catch (Throwable var8) {
               }
            }
         }

         return m;
      } else {
         return m;
      }
   }

   private static void respondHtml(HttpExchange ex, int code, String html) {
      try {
         byte[] b = html.getBytes(StandardCharsets.UTF_8);
         ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
         ex.sendResponseHeaders(code, b.length);

         try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
         }
      } catch (Throwable var9) {
      }
   }

   private static String cfg(String k) {
      try {
         String v = Platform.game().getConfig(k, "");
         return v == null ? "" : v;
      } catch (Throwable var2) {
         return "";
      }
   }

   private static void setCfg(String k, String v) {
      try {
         Platform.game().setConfig(k, v);
      } catch (Throwable var3) {
      }
   }

   private static String enc(String s) {
      return URLEncoder.encode(s, StandardCharsets.UTF_8);
   }

   private static void sleep(long ms) {
      try {
         Thread.sleep(ms);
      } catch (InterruptedException var3) {
      }
   }

   private static String page(String bodyHtml) {
      return "<!doctype html><html><head><meta charset=utf-8><title>Swift Client · Spotify</title><style>*{box-sizing:border-box}body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:#0b0b0d;color:#e9ecef;font-family:Segoe UI,Roboto,system-ui,sans-serif}.card{width:100%;max-width:420px;background:#141416;border:1px solid #26272b;border-radius:14px;padding:26px;box-shadow:0 20px 60px #0009}h1{font-size:19px;margin:0 0 4px}p{color:#9aa3b2;font-size:13px;line-height:1.6;margin:6px 0}label{display:block;font-size:11px;text-transform:uppercase;letter-spacing:.06em;color:#9aa3b2;margin:14px 0 5px}input{width:100%;padding:10px 12px;background:#0b0b0d;border:1px solid #26272b;border-radius:8px;color:#fff;font-size:13px}input:focus{outline:none;border-color:#3ba55d}button{width:100%;margin-top:18px;padding:11px;background:#3ba55d;color:#04140a;border:0;border-radius:8px;font-weight:700;font-size:14px;cursor:pointer}code{background:#0b0b0d;border:1px solid #26272b;border-radius:5px;padding:2px 6px;color:#e9ecef;font-size:12px}ol{color:#9aa3b2;font-size:12.5px;line-height:1.7;padding-left:18px}a{color:#3ba55d}.ok{color:#3ba55d}.err{color:#e5484d}</style></head><body><div class=card>"
         + bodyHtml
         + "</div></body></html>";
   }

   private static String setupPage() {
      return page(
         "<h1>Connect Spotify</h1><p>Only used to show the <b>next track</b> in your Now playing widget. Read-only.</p><ol><li>Open the <a href=\"https://developer.spotify.com/dashboard\" target=_blank>Spotify Developer Dashboard</a> and create an app.</li><li>In the app settings, add this Redirect URI: <code>http://127.0.0.1:8888/callback</code></li><li>Copy the Client ID and Client Secret below.</li></ol><form action=/submit method=post><label>Client ID</label><input name=clientId value=\""
            + esc(clientId)
            + "\" placeholder=\"Client ID\" required><label>Client Secret</label><input name=clientSecret type=password value=\""
            + esc(clientSecret)
            + "\" placeholder=\"Client Secret\" required><button type=submit>Connect Spotify account</button></form>"
      );
   }

   private static String messagePage(String title, String msg, boolean ok) {
      return page("<h1 class=" + (ok ? "ok" : "err") + ">" + esc(title) + "</h1><p>" + esc(msg) + "</p>");
   }

   private static String esc(String s) {
      return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
   }
}
