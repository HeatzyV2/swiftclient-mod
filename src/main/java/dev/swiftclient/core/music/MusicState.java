package dev.swiftclient.core.music;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.swiftclient.core.platform.Platform;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MusicState {
   private static volatile String title = "";
   private static volatile String artist = "";
   private static volatile boolean playing = false;
   private static volatile long durationMs = 0L;
   private static volatile long updatedAt = 0L;
   private static volatile long anchorPos = 0L;
   private static volatile long anchorAt = 0L;
   private static volatile String anchorTrackId = "\u0000";
   private static volatile String trackId = "";
   private static volatile Object artHandle = null;
   private static volatile String lastArtLoaded = "";
   private static volatile boolean started = false;

   private MusicState() {
   }

   /** Start the Spotify poller and the optional launcher file bridge (safe to call from any thread). */
   public static void boot() {
      ensureStarted();
   }

   private static synchronized void ensureStarted() {
      if (started) {
         return;
      }
      started = true;

      try {
         SpotifyManager.ensureStarted();
      } catch (Throwable ignored) {
      }

      // Windows SMTC is not started here: WindowsSmtc.setActive() runs it only while the
      // "Now playing" module is enabled (see SwiftClient tick).

      Path file = resolveNowPlayingFile();
      if (file != null) {
         ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "swiftclient-music-file");
            t.setDaemon(true);
            return t;
         });
         exec.scheduleAtFixedRate(() -> pollFile(file), 0L, 1L, TimeUnit.SECONDS);
      }
   }

   /** Optional launcher bridge: {@code -Dswiftclient.nowPlayingFile=} (legacy lightclient name still accepted). */
   private static Path resolveNowPlayingFile() {
      for (String key : new String[]{"swiftclient.nowPlayingFile", "lightclient.nowPlayingFile"}) {
         String path = System.getProperty(key);
         if (path != null && !path.isBlank()) {
            return Path.of(path);
         }
      }
      return null;
   }

   private static void pollFile(Path file) {
      try {
         if (!Files.exists(file)) {
            return;
         }
         String body = Files.readString(file);
         if (!body.isBlank()) {
            applyJson(body, true);
         }
      } catch (Throwable ignored) {
      }
   }

   /** Apply a now-playing JSON blob from SMTC, Spotify, or an external file. */
   public static void applyJson(String body, boolean loadArt) {
      try {
         JsonObject j = JsonParser.parseString(body).getAsJsonObject();
         apply(
            str(j, "title"),
            str(j, "artist"),
            j.has("playing") && !j.get("playing").isJsonNull() && j.get("playing").getAsBoolean(),
            lng(j, "position"),
            lng(j, "duration"),
            str(j, "trackId"),
            loadArt ? str(j, "art") : "",
            loadArt
         );
      } catch (Throwable ignored) {
      }
   }

   public static void apply(
      String newTitle,
      String newArtist,
      boolean newPlaying,
      long positionMs,
      long newDurationMs,
      String newTrackId,
      String artPath,
      boolean loadArt
   ) {
      if (newTitle == null || newTitle.isBlank()) {
         return;
      }
      long now = System.currentTimeMillis();
      title = newTitle;
      artist = newArtist == null ? "" : newArtist;
      playing = newPlaying;
      durationMs = Math.max(0L, newDurationMs);
      updatedAt = now;
      String tid = newTrackId == null || newTrackId.isBlank() ? newTitle + "|" + artist : newTrackId;
      trackId = tid;
      long sp = Math.max(0L, positionMs);
      if (!tid.equals(anchorTrackId)) {
         anchorPos = sp;
         anchorAt = now;
         anchorTrackId = tid;
      } else if (!playing) {
         anchorPos = sp;
         anchorAt = now;
      } else {
         long expected = anchorPos + (now - anchorAt);
         if (sp > expected + 2000L || sp + 2000L < expected) {
            anchorPos = sp;
            anchorAt = now;
         }
      }

      if (!loadArt) {
         return;
      }
      if (title.isEmpty()) {
         artHandle = null;
         lastArtLoaded = "";
         } else if (!tid.isEmpty() && !tid.equals(lastArtLoaded) && artPath != null && !artPath.isEmpty()) {
            try {
               Path ap = Path.of(artPath);
               if (Files.exists(ap)) {
                  byte[] bytes = Files.readAllBytes(ap);
                  byte[] png = toPng(bytes);
                  if (png != null) {
                     lastArtLoaded = tid;
                     Platform.game().loadImage(tid, png, (k, h) -> {
                        if (tid.equals(trackId)) {
                           artHandle = h;
                        }
                     });
                  }
               }
            } catch (Throwable ignored) {
            }
         }
   }

   private static byte[] toPng(byte[] bytes) {
      if (bytes.length >= 8
         && (bytes[0] & 0xFF) == 0x89
         && bytes[1] == 0x50
         && bytes[2] == 0x4E
         && bytes[3] == 0x47) {
         return bytes;
      }
      try {
         java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
         if (img == null) {
            return null;
         }
         java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
         javax.imageio.ImageIO.write(img, "png", bos);
         return bos.toByteArray();
      } catch (Throwable t) {
         return null;
      }
   }

   private static String str(JsonObject j, String k) {
      return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsString() : "";
   }

   private static long lng(JsonObject j, String k) {
      try {
         return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsLong() : 0L;
      } catch (Throwable var3) {
         return 0L;
      }
   }

   public static boolean hasMusic() {
      ensureStarted();
      return !title.isEmpty() && System.currentTimeMillis() - updatedAt < 10000L;
   }

   public static Object artHandle() {
      return artHandle;
   }

   public static String title() {
      return title;
   }

   public static String artist() {
      return artist;
   }

   public static boolean playing() {
      return playing;
   }

   public static long durationMs() {
      return durationMs;
   }

   public static long positionMs() {
      long p = playing ? anchorPos + (System.currentTimeMillis() - anchorAt) : anchorPos;
      if (durationMs > 0L) {
         p = Math.min(p, durationMs);
      }
      return Math.max(0L, p);
   }

   public static float progress() {
      long d = durationMs;
      return d > 0L ? Math.max(0.0F, Math.min(1.0F, (float)positionMs() / (float)d)) : 0.0F;
   }
}
