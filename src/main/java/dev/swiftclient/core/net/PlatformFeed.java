package dev.swiftclient.core.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.ui.Notifications;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * What the Swift admin platform controls in game, read from {@code GET /api/v1/launcher/bootstrap}:
 * announcements and the maintenance notice (shown once as in-game toasts), and a remote kill switch for
 * modules ({@code mod.disabledModules} in the remote config). Read-only and anonymous: no account, no
 * device id, no telemetry. If the backend is down or too old to know the route, nothing changes.
 */
public final class PlatformFeed {
   private static final Logger LOG = Log.get("Platform");
   private static final long REFRESH_MINUTES = 10L;
   private static final String SEEN_KEY = "platform.seen";
   private static final long TOAST_GAP_MS = 5000L;

   /** One announcement, already resolved for the player's language by the server. */
   public record Notice(String id, String type, String title, String body) {
   }

   /** Parsed bootstrap: what this class keeps from the answer. */
   public record Snapshot(List<Notice> announcements, String maintenance, Set<String> disabledModules) {
      public static final Snapshot EMPTY = new Snapshot(List.of(), null, Set.of());
   }

   private static volatile Snapshot snapshot = Snapshot.EMPTY;
   private static volatile boolean started;
   private static volatile boolean loading;
   private static volatile String modVersion = "?";
   private static volatile long lastToast;
   /** Announcements already toasted in this session. */
   private static final Set<String> shownThisSession = new HashSet<>();
   private static volatile boolean maintenanceShown;

   private PlatformFeed() {
   }

   public static void start(String version) {
      if (started || !Endpoints.backendEnabled()) {
         return;
      }

      started = true;
      modVersion = version == null ? "?" : version;
      refresh();
      Net.SCHEDULER.scheduleWithFixedDelay(PlatformFeed::refresh, REFRESH_MINUTES, REFRESH_MINUTES, TimeUnit.MINUTES);
   }

   public static Snapshot current() {
      return snapshot;
   }

   /** True when the admin switched this module off remotely. Cheap: called on render paths. */
   public static boolean isDisabled(String moduleId) {
      return snapshot.disabledModules().contains(moduleId);
   }

   private static void refresh() {
      if (loading || !Endpoints.backendEnabled()) {
         return;
      }

      loading = true;
      Net.IO.execute(() -> {
         try {
            String path = "/api/v1/launcher/bootstrap?v=" + enc(modVersion) + "&os=" + enc(os()) + "&arch=" + enc(arch())
               + "&locale=" + enc(locale()) + "&channel=stable";
            Backend.Response r = Backend.get(path, false);
            if (r.ok()) {
               Snapshot next = parse(r.json(), locale());
               if (next != null) {
                  snapshot = next;
               }
            }
         } catch (RuntimeException e) {
            LOG.debug("Platform feed unavailable: {}", e.toString());
         } finally {
            loading = false;
         }
      });
   }

   /**
    * Extracts the parts the mod uses from a bootstrap envelope ({@code {success, data}}). Returns null when
    * the answer is not a successful envelope, so the previous snapshot stays.
    */
   public static Snapshot parse(JsonElement root, String locale) {
      if (root == null || !root.isJsonObject()) {
         return null;
      }

      JsonObject env = root.getAsJsonObject();
      if (!env.has("success") || !env.get("success").getAsBoolean() || !env.has("data") || !env.get("data").isJsonObject()) {
         return null;
      }

      JsonObject data = env.getAsJsonObject("data");
      List<Notice> notices = new ArrayList<>();
      if (data.has("announcements") && data.get("announcements").isJsonArray()) {
         for (JsonElement el : data.getAsJsonArray("announcements")) {
            if (el.isJsonObject()) {
               JsonObject o = el.getAsJsonObject();
               String id = text(o, "id");
               String title = text(o, "title");
               if (!id.isEmpty() && !title.isEmpty()) {
                  notices.add(new Notice(id, text(o, "type"), title, text(o, "body")));
               }
            }
         }
      }

      String maintenance = null;
      if (data.has("maintenance") && data.get("maintenance").isJsonObject()) {
         JsonObject m = data.getAsJsonObject("maintenance");
         if (m.has("active") && m.get("active").getAsBoolean()) {
            maintenance = pickLocalized(m.get("message"), locale);
         }
      }

      Set<String> disabled = new LinkedHashSet<>();
      if (data.has("config") && data.get("config").isJsonObject()) {
         JsonObject cfg = data.getAsJsonObject("config");
         if (cfg.has("mod.disabledModules") && cfg.get("mod.disabledModules").isJsonArray()) {
            JsonArray arr = cfg.getAsJsonArray("mod.disabledModules");
            for (JsonElement el : arr) {
               if (el.isJsonPrimitive() && el.getAsString().matches("[a-z0-9_]{1,48}")) {
                  disabled.add(el.getAsString());
               }
            }
         }
      }

      return new Snapshot(List.copyOf(notices), maintenance == null ? null : maintenance, Set.copyOf(disabled));
   }

   /** Language, then English, French, anything: same rule as the launcher and the API. */
   static String pickLocalized(JsonElement map, String locale) {
      if (map == null || !map.isJsonObject()) {
         return "";
      }

      JsonObject o = map.getAsJsonObject();
      String l = locale == null ? "en" : locale.toLowerCase(Locale.ROOT).split("[-_]")[0];
      for (String key : new String[]{l, "en", "fr"}) {
         if (o.has(key) && o.get(key).isJsonPrimitive() && !o.get(key).getAsString().isBlank()) {
            return o.get(key).getAsString();
         }
      }

      for (var e : o.entrySet()) {
         if (e.getValue().isJsonPrimitive() && !e.getValue().getAsString().isBlank()) {
            return e.getValue().getAsString();
         }
      }

      return "";
   }

   /**
    * Called every client tick. When the player is in a world, shows at most one pending notice every few
    * seconds: maintenance and warnings once per session, info and success only the first time ever.
    */
   public static void tick(boolean inWorld) {
      if (!inWorld || !started) {
         return;
      }

      long now = System.currentTimeMillis();
      if (now - lastToast < TOAST_GAP_MS) {
         return;
      }

      Snapshot s = snapshot;
      if (s.maintenance() != null && !maintenanceShown) {
         maintenanceShown = true;
         lastToast = now;
         Notifications.push("Swift Client", clip(s.maintenance().isEmpty() ? Tr.of("swift.platform.maintenance") : s.maintenance()), null);
         return;
      }

      Set<String> seen = seenForever();
      for (Notice n : s.announcements()) {
         boolean loud = "warning".equals(n.type()) || "critical".equals(n.type());
         if (shownThisSession.contains(n.id()) || (!loud && seen.contains(n.id()))) {
            continue;
         }

         shownThisSession.add(n.id());
         if (!loud) {
            seen.add(n.id());
            rememberSeen(seen);
         }

         lastToast = now;
         Notifications.push(clip(n.title()), clip(n.body()), null);
         return;
      }
   }

   private static String clip(String s) {
      String t = s == null ? "" : s.strip();
      return t.length() > 90 ? t.substring(0, 89) + "…" : t;
   }

   private static Set<String> seenForever() {
      Set<String> out = new LinkedHashSet<>();
      try {
         for (String id : Platform.game().getConfig(SEEN_KEY, "").split(",")) {
            if (!id.isBlank()) {
               out.add(id.trim());
            }
         }
      } catch (RuntimeException ignored) {
      }

      return out;
   }

   private static void rememberSeen(Set<String> seen) {
      List<String> ids = new ArrayList<>(seen);
      if (ids.size() > 50) {
         ids = ids.subList(ids.size() - 50, ids.size());
      }

      try {
         Platform.game().setConfig(SEEN_KEY, String.join(",", ids));
      } catch (RuntimeException ignored) {
      }
   }

   private static String text(JsonObject o, String key) {
      return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
   }

   private static String locale() {
      try {
         String l = Platform.game().currentLanguage();
         return l == null || l.isBlank() ? "en" : l.toLowerCase(Locale.ROOT).split("[-_]")[0];
      } catch (RuntimeException e) {
         return "en";
      }
   }

   private static String os() {
      String n = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      return n.contains("win") ? "windows" : n.contains("mac") ? "macos" : "linux";
   }

   private static String arch() {
      String a = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
      return a.contains("aarch64") || a.contains("arm64") ? "arm64" : a.equals("x86") || a.equals("i386") ? "x86" : "x64";
   }

   private static String enc(String s) {
      return URLEncoder.encode(s, StandardCharsets.UTF_8);
   }
}
