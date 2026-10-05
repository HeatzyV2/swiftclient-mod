package dev.swiftclient.core.session;

import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.platform.Platform;

/**
 * What the player did: time spent in a world and distance walked, for this game session, the one before it,
 * and all time. Sampled once per frame from the HUD data and written to the config every few seconds, so a
 * crash loses almost nothing. Nothing leaves the machine.
 */
public final class SessionStats {
   private static final long SAVE_EVERY_MS = 15_000L;
   /** A jump bigger than this between two frames is a teleport (or a long lag spike), not walking. */
   private static final double MAX_STEP = 12.0;

   private static boolean loaded;
   private static boolean inWorld;
   private static boolean haveLast;
   private static double lastX;
   private static double lastZ;
   private static long lastFrameMs;
   private static long lastSaveMs;
   private static boolean counted;

   private static long curMs;
   private static double curDistance;
   private static long prevSeconds;
   private static double prevDistance;
   private static long totalSeconds;
   private static double totalDistance;
   private static int sessions;
   /** What was already added to the totals for the current session. */
   private static long bankedSeconds;
   private static double bankedDistance;

   private SessionStats() {
   }

   private static void load() {
      if (loaded) {
         return;
      }
      loaded = true;
      prevSeconds = (long)num("session.cur.seconds");
      prevDistance = num("session.cur.distance");
      totalSeconds = (long)num("session.total.seconds");
      totalDistance = num("session.total.distance");
      sessions = (int)num("session.count");
   }

   private static double num(String key) {
      try {
         return Double.parseDouble(Platform.game().getConfig(key, "0"));
      } catch (Throwable ignored) {
         return 0.0;
      }
   }

   private static void put(String key, String value) {
      try {
         Platform.game().setConfig(key, value);
      } catch (Throwable ignored) {
      }
   }

   /** Called every frame with the latest HUD data. */
   public static void sample(HudData d) {
      load();
      long now = System.currentTimeMillis();
      boolean up = d != null && d.inWorld();
      if (up) {
         if (!inWorld) {
            inWorld = true;
            haveLast = false;
            lastFrameMs = now;
            lastSaveMs = now;
            if (!counted) {
               counted = true;
               sessions++;
            }
         }
         // A long gap (pause, alt-tab, lag) is not counted as play time
         curMs += Math.min(Math.max(0L, now - lastFrameMs), 1000L);
         lastFrameMs = now;
         if (haveLast) {
            double dx = d.x() - lastX;
            double dz = d.z() - lastZ;
            double step = Math.sqrt(dx * dx + dz * dz);
            if (step < MAX_STEP) {
               curDistance += step;
            }
         }
         lastX = d.x();
         lastZ = d.z();
         haveLast = true;
         if (now - lastSaveMs >= SAVE_EVERY_MS) {
            save();
            lastSaveMs = now;
         }
      } else if (inWorld) {
         inWorld = false;
         save();
      }
   }

   private static void save() {
      long seconds = curMs / 1000L;
      totalSeconds += seconds - bankedSeconds;
      totalDistance += curDistance - bankedDistance;
      bankedSeconds = seconds;
      bankedDistance = curDistance;
      put("session.cur.seconds", Long.toString(seconds));
      put("session.cur.distance", Long.toString(Math.round(curDistance)));
      put("session.total.seconds", Long.toString(totalSeconds));
      put("session.total.distance", Long.toString(Math.round(totalDistance)));
      put("session.count", Integer.toString(sessions));
   }

   public static long currentSeconds() {
      return curMs / 1000L;
   }

   public static double currentDistance() {
      return curDistance;
   }

   public static long previousSeconds() {
      load();
      return prevSeconds;
   }

   public static double previousDistance() {
      load();
      return prevDistance;
   }

   public static long totalSeconds() {
      load();
      return totalSeconds + (curMs / 1000L - bankedSeconds);
   }

   public static double totalDistance() {
      load();
      return totalDistance + (curDistance - bankedDistance);
   }

   public static int sessions() {
      load();
      return sessions;
   }

   /** "2h 05m", "14m 03s" or "42s". */
   public static String duration(long seconds) {
      long h = seconds / 3600L;
      long m = seconds % 3600L / 60L;
      long s = seconds % 60L;
      if (h > 0) {
         return h + "h " + String.format("%02d", m) + "m";
      }
      return m > 0 ? m + "m " + String.format("%02d", s) + "s" : s + "s";
   }

   /** Blocks below a kilometre, kilometres above. */
   public static String distance(double blocks) {
      return blocks < 1000.0 ? Math.round(blocks) + " m" : String.format(java.util.Locale.ROOT, "%.1f km", blocks / 1000.0);
   }
}
