package dev.swiftclient.core.badges;

import dev.swiftclient.core.cosmetics.CosmeticHttp;
import dev.swiftclient.core.cosmetics.OfflineNames;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BadgeState {
   private static final long REFRESH_MS = 120000L;
   private static final long REFRESH_NEGATIVE_MS = 12000L;
   private static final Map<UUID, String> GRADES = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> QUERIED = new ConcurrentHashMap<>();
   private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();
   private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "swiftclient-badges");
      t.setDaemon(true);
      return t;
   });
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
      String var1 = grade == null ? "member" : grade;

      return switch (var1) {
         case "staff" -> -1096636;
         case "partner" -> -4160260;
         case "light_plus" -> -10934;
         case "early_access" -> -11870592;
         case "bug_hunter" -> -680437;
         default -> -1;
      };
   }

   public static boolean isLightPlus(UUID uuid) {
      return uuid != null && "light_plus".equals(gradeFor(uuid));
   }

   public static boolean selfSwiftPlus() {
      long now = System.currentTimeMillis();
      if (now - selfCheckedAt > 120000L) {
         selfCheckedAt = now;
         IO.execute(() -> selfSwiftPlus = CosmeticHttp.subscriptionActiveSelf());
      }

      return selfSwiftPlus;
   }

   public static void invalidateSelfLightPlus() {
      selfCheckedAt = 0L;
   }

   private static void scheduleFetch() {
      IO.execute(() -> {
         if (!PENDING.isEmpty()) {
            long wait = 1500L - (System.currentTimeMillis() - lastFetchAt);
            if (wait > 0L) {
               try {
                  Thread.sleep(wait);
               } catch (InterruptedException var10) {
               }
            }

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
      });
   }
}
