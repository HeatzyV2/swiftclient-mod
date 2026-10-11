package dev.swiftclient.core.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.net.Net;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.platform.Tr;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * Looks for a newer release of the mod on GitHub and tells the player once per session, in game. It never
 * downloads anything: the launcher installs mod updates, the toast just says one is waiting.
 */
public final class ModUpdate {
   public static final String MODULE_ID = "updates";
   private static final Logger LOG = Log.get("Update");
   private static final String API = "https://api.github.com/repos/HeatzyV2/swiftclient-mod/releases/latest";
   private static final long RECHECK_HOURS = 6L;

   public record Release(String tag, String name, String url, String notes) {
   }

   private static volatile Release latest;
   private static volatile String local = "?";
   private static volatile boolean toasted;
   private static volatile boolean started;

   private ModUpdate() {
   }

   public static void start(String version) {
      if (started) {
         return;
      }

      started = true;
      local = version == null ? "?" : version;
      Net.SCHEDULER.schedule(() -> check(false), 25L, TimeUnit.SECONDS);
      Net.SCHEDULER.scheduleWithFixedDelay(() -> check(false), RECHECK_HOURS, RECHECK_HOURS, TimeUnit.HOURS);
   }

   /** Reads a GitHub "latest release" document. Null when it is not a usable release. */
   public static Release parse(String json) {
      try {
         JsonElement root = JsonParser.parseString(json);
         if (!root.isJsonObject()) {
            return null;
         }

         JsonObject o = root.getAsJsonObject();
         if (!o.has("tag_name") || o.get("tag_name").isJsonNull() || (o.has("draft") && o.get("draft").getAsBoolean())) {
            return null;
         }

         String tag = o.get("tag_name").getAsString();
         String name = o.has("name") && !o.get("name").isJsonNull() ? o.get("name").getAsString() : tag;
         String url = o.has("html_url") && !o.get("html_url").isJsonNull() ? o.get("html_url").getAsString() : "";
         String notes = o.has("body") && !o.get("body").isJsonNull() ? o.get("body").getAsString() : "";
         return new Release(tag, name, url, notes);
      } catch (RuntimeException e) {
         return null;
      }
   }

   /** Checks now. {@code manual} also reports "up to date" and failures to the player. */
   public static void check(boolean manual) {
      Net.IO.execute(() -> {
         try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(API))
               .timeout(Duration.ofSeconds(10L))
               .header("Accept", "application/vnd.github+json")
               .header("User-Agent", "SwiftClient-Mod/" + local)
               .GET()
               .build();
            HttpResponse<String> res = Net.HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            Release r = res.statusCode() == 200 ? parse(res.body()) : null;
            if (r == null) {
               LOG.debug("Update check: no usable release (HTTP {})", res.statusCode());
               if (manual) {
                  Platform.game().notify("Swift Client", Tr.of("swift.update.failed"));
               }

               return;
            }

            latest = r;
            if (manual) {
               if (Versions.isNewer(local, r.tag())) {
                  announce(r);
               } else {
                  Platform.game().notify("Swift Client", Tr.of("swift.update.current", local));
               }
            }
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         } catch (Exception e) {
            LOG.debug("Update check failed: {}", e.toString());
            if (manual) {
               Platform.game().notify("Swift Client", Tr.of("swift.update.failed"));
            }
         }
      });
   }

   private static void announce(Release r) {
      Platform.game().notify(Tr.of("swift.update.title", r.tag()), Tr.of("swift.update.body"));
   }

   /** The newer release the player has not been told about yet, or null. */
   public static Release available() {
      Release r = latest;
      return r != null && Versions.isNewer(local, r.tag()) ? r : null;
   }

   /** Called every client tick: once per session, in a world, tells the player an update is waiting. */
   public static void tick(boolean inWorld) {
      if (!inWorld || toasted || !started) {
         return;
      }

      Release r = available();
      if (r != null && ModuleManager.isLoaded() && ModuleManager.active(MODULE_ID)) {
         toasted = true;
         announce(r);
      }
   }
}
