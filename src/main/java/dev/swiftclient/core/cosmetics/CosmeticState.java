package dev.swiftclient.core.cosmetics;

import dev.swiftclient.core.net.Net;
import dev.swiftclient.core.log.Log;
import org.slf4j.Logger;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.swiftclient.core.platform.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public final class CosmeticState {
   private static final Logger LOG = Log.get("Cape");
   private static final long REFRESH_MS = 30000L;
   private static final long REFRESH_SELF_MS = 3000L;
   private static final Map<String, int[]> META = new ConcurrentHashMap<>();
   private static final Map<UUID, String> EQUIPPED = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> QUERIED = new ConcurrentHashMap<>();
   private static final Map<String, Object[]> FRAMES = new ConcurrentHashMap<>();
   private static final Map<UUID, Boolean> ANIMATED = new ConcurrentHashMap<>();
   private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
   private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();
   private static volatile boolean selfAnimated = true;
   private static volatile boolean selfAnimatedInit = false;
   private static final ExecutorService IO = Net.IO;
   private static volatile boolean catalogLoaded = false;

   private CosmeticState() {
   }

   private static void ensureCatalog() {
      if (!catalogLoaded && CosmeticHttp.backendConfigured()) {
         catalogLoaded = true;
         IO.execute(
            () -> {
               for (JsonElement el : CosmeticHttp.catalog()) {
                  if (el.isJsonObject()) {
                     JsonObject o = el.getAsJsonObject();
                     if ((!o.has("type") || "cape".equals(o.get("type").getAsString())) && o.has("id")) {
                        META.put(
                           o.get("id").getAsString(),
                           new int[]{
                              o.has("frames") ? o.get("frames").getAsInt() : 1,
                              o.has("fps") ? o.get("fps").getAsInt() : 10,
                              o.has("frameW") ? o.get("frameW").getAsInt() : 64,
                              o.has("frameH") ? o.get("frameH").getAsInt() : 32
                           }
                        );
                     }
                  }
               }
            }
         );
      }
   }

   public static String capeFor(UUID uuid, String pseudo) {
      OfflineNames.remember(uuid, pseudo);
      return capeFor(uuid);
   }

   public static String capeFor(UUID uuid) {
      if (uuid == null || !CosmeticHttp.backendConfigured()) {
         return null;
      }

      ensureCatalog();
      Long q = QUERIED.get(uuid);
      long now = System.currentTimeMillis();
      if ((q == null || now - q > (isSelf(uuid) ? 3000L : 30000L)) && PENDING.add(uuid)) {
         scheduleFetch();
      }

      String id = EQUIPPED.get(uuid);
      return id != null && !id.isEmpty() ? id : null;
   }

   private static boolean isSelf(UUID uuid) {
      return OfflineNames.isSelf(uuid);
   }

   public static void warmup() {
      ensureCatalog();
   }

   public static int[] metaFor(String capeId) {
      int[] m = META.get(capeId);
      return m != null ? m : new int[]{1, 10, 64, 32};
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

            Map<String, String> res = CosmeticHttp.equippedFor(ids);
            Map<String, Boolean> anim = CosmeticHttp.animatedFor(ids);
            long now = System.currentTimeMillis();

            for (UUID u : batch) {
               String key = u.toString().replace("-", "");
               String val = res.getOrDefault(key, "");
               String old = EQUIPPED.put(u, val);
               boolean a = anim.getOrDefault(key, Boolean.TRUE);
               ANIMATED.put(u, a);
               if (isSelf(u) && !selfAnimatedInit) {
                  selfAnimated = a;
                  selfAnimatedInit = true;
               }

               QUERIED.put(u, now);
               if (old != null && !old.equals(val)) {
                  LOG.debug("Cape de {} : {} -> {}", key, old, val);
               }
            }
         }
      });
   }

   public static Object currentFrame(String capeId) {
      Object[] fr = FRAMES.get(capeId);
      if (fr == null) {
         loadCape(capeId);
         return null;
      } else if (fr.length == 0) {
         return null;
      } else {
         int fps = Math.max(1, META.getOrDefault(capeId, new int[]{fr.length, 10, 64, 32})[1]);
         int idx = (int)(System.currentTimeMillis() / (1000L / fps) % fr.length);
         Object h = fr[idx];
         return h != null ? h : firstLoaded(fr);
      }
   }

   private static Object firstLoaded(Object[] fr) {
      for (Object o : fr) {
         if (o != null) {
            return o;
         }
      }

      return null;
   }

   public static Object frameFor(UUID uuid, String capeId) {
      if (!isAnimated(uuid)) {
         Object[] fr = FRAMES.get(capeId);
         if (fr == null) {
            loadCape(capeId);
            return null;
         } else if (fr.length == 0) {
            return null;
         } else {
            return fr[0] != null ? fr[0] : firstLoaded(fr);
         }
      } else {
         return currentFrame(capeId);
      }
   }

   public static boolean capeHasElytra(String capeId) {
      if (capeId == null || capeId.isBlank()) {
         return false;
      } else if (FRAMES.get(capeId) == null) {
         loadCape(capeId);
         return false;
      } else {
         return Platform.game().capeHasElytra(capeId);
      }
   }

   public static boolean isAnimated(UUID uuid) {
      return isSelf(uuid) ? selfAnimated : ANIMATED.getOrDefault(uuid, Boolean.TRUE);
   }

   public static boolean selfAnimated() {
      return selfAnimated;
   }

   public static void setSelfAnimated(boolean on) {
      selfAnimated = on;
      IO.execute(() -> CosmeticHttp.setCapeAnimated(on));
   }

   private static int[] pngSize(byte[] png) {
      if (png != null && png.length >= 24) {
         int w = (png[16] & 255) << 24 | (png[17] & 255) << 16 | (png[18] & 255) << 8 | png[19] & 255;
         int h = (png[20] & 255) << 24 | (png[21] & 255) << 16 | (png[22] & 255) << 8 | png[23] & 255;
         return w > 0 && h > 0 ? new int[]{w, h} : null;
      } else {
         return null;
      }
   }

   private static void loadCape(String capeId) {
      if (LOADING.add(capeId)) {
         IO.execute(
            () -> {
               byte[] png = capeId.startsWith("mojang:") ? CosmeticHttp.rawTexture(capeId.substring("mojang:".length())) : CosmeticHttp.texture(capeId);
               if (png == null) {
                  LOG.warn("Texture de cape {} introuvable", capeId);
                  LOADING.remove(capeId);
               } else {
                  LOG.debug("Texture de cape {} telechargee ({} octets)", capeId, png.length);
                  int[] m = META.get(capeId);
                  int frameW = m != null ? m[2] : 0;
                  int frameH = m != null ? m[3] : 0;
                  int[] size = pngSize(png);
                  if (size != null && size[0] % 2 == 0 && size[1] % (size[0] / 2) == 0 && (frameW != size[0] || frameH != size[0] / 2)) {
                     LOG.debug("Cape {} : meta {}x{} incoherente avec la planche {}x{}, on suit l'image", capeId, frameW, frameH, size[0], size[1]);
                     frameW = 0;
                     frameH = 0;
                  }

                  Platform.game().loadCapeFrames(capeId, png, frameW, frameH, (id, handles) -> {
                     FRAMES.put(id, handles);
                     LOG.debug("Cape {} prete : {} frame(s)", id, handles.length);
                  });
               }
            }
         );
      }
   }

   /** Forgets players that are no longer around; they are fetched again if they come back. */
   public static void retainOnly(Set<UUID> keep) {
      EQUIPPED.keySet().removeIf(u -> !keep.contains(u));
      QUERIED.keySet().removeIf(u -> !keep.contains(u));
      ANIMATED.keySet().removeIf(u -> !keep.contains(u));
   }

   public static void clear() {
      EQUIPPED.clear();
      QUERIED.clear();
      ANIMATED.clear();
   }
}
