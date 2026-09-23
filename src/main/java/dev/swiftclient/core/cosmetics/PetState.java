package dev.swiftclient.core.cosmetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PetState {
   private static final long REFRESH_MS = 30000L;
   private static final long REFRESH_SELF_MS = 3000L;
   private static final Map<UUID, String> EQUIPPED = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> QUERIED = new ConcurrentHashMap<>();
   private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();
   private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "swiftclient-pets");
      t.setDaemon(true);
      return t;
   });

   private PetState() {
   }

   public static String petFor(UUID uuid, String pseudo) {
      OfflineNames.remember(uuid, pseudo);
      return petFor(uuid);
   }

   public static String petFor(UUID uuid) {
      if (uuid == null) {
         return null;
      } else {
         Long q = QUERIED.get(uuid);
         long now = System.currentTimeMillis();
         if ((q == null || now - q > (isSelf(uuid) ? 3000L : 30000L)) && PENDING.add(uuid)) {
            scheduleFetch();
         }

         String id = EQUIPPED.get(uuid);
         return id != null && !id.isEmpty() ? id : null;
      }
   }

   public static boolean hasPet(UUID uuid) {
      return petFor(uuid) != null;
   }

   private static boolean isSelf(UUID uuid) {
      return OfflineNames.isSelf(uuid);
   }

   public static void applySelf(String value) {
      long now = System.currentTimeMillis();

      for (UUID u : OfflineNames.selfUuids()) {
         EQUIPPED.put(u, value == null ? "" : value);
         QUERIED.put(u, now);
      }
   }

   private static void scheduleFetch() {
      IO.execute(() -> {
         List<UUID> batch = new ArrayList<>(PENDING);
         PENDING.clear();
         if (!batch.isEmpty()) {
            List<String> ids = new ArrayList<>(batch.size());

            for (UUID u : batch) {
               ids.add(u.toString().replace("-", ""));
            }

            Map<String, String> res = CosmeticHttp.petsFor(ids);
            long now = System.currentTimeMillis();

            for (UUID u : batch) {
               String val = res.getOrDefault(u.toString().replace("-", ""), "");
               EQUIPPED.put(u, val);
               QUERIED.put(u, now);
            }
         }
      });
   }
}
