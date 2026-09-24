package dev.swiftclient.core.badges;

import java.util.concurrent.TimeUnit;
import dev.swiftclient.core.net.Net;
import dev.swiftclient.core.cosmetics.CosmeticHttp;
import dev.swiftclient.core.cosmetics.OfflineNames;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public final class BadgeState {
   private static final long REFRESH_MS = 120000L;
   private static final long REFRESH_NEGATIVE_MS = 12000L;
   private static final Map<UUID, String> GRADES = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> QUERIED = new ConcurrentHashMap<>();
   private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();
   private static final ExecutorService IO = Net.IO;
   private static volatile boolean selfSwiftPlus = false;
   private static volatile long selfCheckedAt = 0L;
   private static volatile long lastFetchAt = 0L;

   private BadgeState() {
   }

   public static String gradeFor(UUID uuid, String pseudo) {
      OfflineNames.remember(uuid, pseudo);
      return gradeFor(uuid);
   }

   public static String gradeFor(UUID uuid) {
      if (uuid == null || !CosmeticHttp.backendConfigured()) {
         return null;
      }

      Long q = QUERIED.get(uuid);
      long now = System.currentTimeMillis();
      String g = GRADES.get(uuid);
      long ttl = g != null && !g.isEmpty() ? 120000L : 12000L;
      if ((q == null || now - q > ttl) && PENDING.add(uuid)) {
         scheduleFetch();
      }

      return g != null && !g.isEmpty() ? g : null;
   }

   public static void invalidate(UUID uuid) {
      if (uuid != null) {
         QUERIED.remove(uuid);
      }
   }

   public static int colorFor(String grade) {
      return switch (grade == null ? "member" : grade) {
         case "staff" -> -1096636;
         case "partner" -> -4160260;
         case "swift_plus" -> -10934;
         case "early_access" -> -11870592;
         case "bug_hunter" -> -680437;
         default -> -1;
      };
   }

   public static boolean isSwiftPlus(UUID uuid) {
      return uuid != null && "swift_plus".equals(gradeFor(uuid));
   }

   public static boolean selfSwiftPlus() {
      long now = System.currentTimeMillis();
      if (CosmeticHttp.backendConfigured() && now - selfCheckedAt > 120000L) {
         selfCheckedAt = now;
         IO.execute(() -> selfSwiftPlus = CosmeticHttp.subscriptionActiveSelf());
      }

      return selfSwiftPlus;
   }

   public static void invalidateSelfSwiftPlus() {
      selfCheckedAt = 0L;
   }

   private static void scheduleFetch() {
      // Batches lookups: at most one request per 1.5 s, without holding a pool thread while waiting.
      long wait = Math.max(0L, 1500L - (System.currentTimeMillis() - lastFetchAt));
      Net.SCHEDULER.schedule(() -> IO.execute(BadgeState::fetchPending), wait, TimeUnit.MILLISECONDS);
   }

   private static synchronized void fetchPending() {
      if (!PENDING.isEmpty()) {
         List<UUID> batch = new ArrayList<>(PENDING);
         PENDING.clear();
         if (!batch.isEmpty()) {
            lastFetchAt = System.currentTimeMillis();
            List<String> ids = new ArrayList<>(batch.size());

            for (UUID u : batch) {
               ids.add(u.toString().replace("-", ""));
            }

            Map<String, String> res = CosmeticHttp.gradesFor(ids);
            long now = System.currentTimeMillis();
            if (res == null) {
               for (UUID u : batch) {
                  QUERIED.put(u, now - 12000L + 4000L);
               }
            } else {
               for (UUID u : batch) {
                  String key = u.toString().replace("-", "");
                  GRADES.put(u, res.getOrDefault(key, ""));
                  QUERIED.put(u, now);
               }
            }
         }
      }
   }

   /** Forgets players that are no longer around; they are fetched again if they come back. */
   public static void retainOnly(Set<UUID> keep) {
      GRADES.keySet().removeIf(u -> !keep.contains(u));
      QUERIED.keySet().removeIf(u -> !keep.contains(u));
   }

   public static void clear() {
      GRADES.clear();
      QUERIED.clear();
   }
}
