package dev.swiftclient.core.cosmetics;

import dev.swiftclient.core.platform.Platform;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OfflineNames {
   private static final Map<String, String> NAMES = new ConcurrentHashMap<>();
   private static volatile String selfCacheKey;
   private static volatile List<UUID> selfCache;

   private OfflineNames() {
   }

   public static boolean offline(UUID uuid) {
      return uuid != null && uuid.version() != 4;
   }

   public static String key(UUID uuid) {
      return uuid == null ? null : uuid.toString().replace("-", "").toLowerCase();
   }

   public static void remember(UUID uuid, String name) {
      if (offline(uuid) && name != null && !name.isBlank()) {
         if (NAMES.size() > 5000) {
            NAMES.clear();
         }

         NAMES.put(key(uuid), name);
      }
   }

   public static String of(String key) {
      return key == null ? null : NAMES.get(key);
   }

   public static UUID offlineUuidOf(String pseudo) {
      if (pseudo != null && !pseudo.isBlank()) {
         try {
            byte[] d = MessageDigest.getInstance("MD5").digest(("OfflinePlayer:" + pseudo).getBytes(StandardCharsets.UTF_8));
            d[6] = (byte)(d[6] & 15 | 48);
            d[8] = (byte)(d[8] & 63 | 128);
            long hi = 0L;
            long lo = 0L;

            for (int i = 0; i < 8; i++) {
               hi = hi << 8 | d[i] & 255;
            }

            for (int i = 8; i < 16; i++) {
               lo = lo << 8 | d[i] & 255;
            }

            return new UUID(hi, lo);
         } catch (Exception var7) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static UUID selfUuid() {
      String self = Platform.game().getUuid();
      if (self != null && self.length() == 32) {
         try {
            return new UUID(Long.parseUnsignedLong(self.substring(0, 16), 16), Long.parseUnsignedLong(self.substring(16), 16));
         } catch (Exception var2) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static List<UUID> selfUuids() {
      String cle = Platform.game().getUuid() + "/" + Platform.game().getUsername();
      List<UUID> cache = selfCache;
      if (cache != null && cle.equals(selfCacheKey)) {
         return cache;
      } else {
         List<UUID> out = new ArrayList<>(2);
         UUID mine = selfUuid();
         if (mine != null) {
            out.add(mine);
         }

         UUID off = offlineUuidOf(Platform.game().getUsername());
         if (off != null && !off.equals(mine)) {
            out.add(off);
         }

         selfCacheKey = cle;
         selfCache = out;
         return out;
      }
   }

   public static boolean isSelf(UUID uuid) {
      return uuid != null && selfUuids().contains(uuid);
   }
}
