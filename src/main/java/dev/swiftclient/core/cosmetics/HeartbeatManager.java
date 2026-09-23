package dev.swiftclient.core.cosmetics;

import dev.swiftclient.core.badges.BadgeState;
import dev.swiftclient.core.platform.Platform;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public final class HeartbeatManager {
   private static final long INTERVAL_MS = 15000L;
   private static final long RETRY_MS = 6000L;
   private static volatile boolean started = false;
   private static volatile BooleanSupplier inGame = () -> false;
   private static boolean wasInGame = false;
   private static boolean lastOk = false;
   private static long lastAttempt = 0L;
   private static String lastUuid = null;

   private HeartbeatManager() {
   }

   public static synchronized void start(BooleanSupplier inGameCheck) {
      inGame = inGameCheck;
      if (!started) {
         started = true;
         ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "swiftclient-heartbeat");
            t.setDaemon(true);
            return t;
         });
         exec.scheduleWithFixedDelay(HeartbeatManager::tick, 2L, 3L, TimeUnit.SECONDS);
      }
   }

   private static void tick() {
      try {
         boolean now = inGame.getAsBoolean();
         if (!now) {
            wasInGame = false;
            return;
         }

         String uuid = Platform.game().getUuid();
         boolean entered = !wasInGame || uuid != null && !uuid.equals(lastUuid);
         long elapsed = System.currentTimeMillis() - lastAttempt;
         if (!entered && elapsed < (lastOk ? 15000L : 6000L)) {
            return;
         }

         wasInGame = true;
         lastUuid = uuid;
         lastAttempt = System.currentTimeMillis();
         boolean ok = CosmeticHttp.heartbeat();
         if (ok && (entered || !lastOk)) {
            invalidateSelf(uuid);
         }

         lastOk = ok;
      } catch (Throwable var6) {
      }
   }

   private static void invalidateSelf(String uuid) {
      try {
         if (uuid == null || uuid.isBlank()) {
            return;
         }

         String u = uuid.replace("-", "");
         if (u.length() != 32) {
            return;
         }

         BadgeState.invalidate(
            UUID.fromString(u.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"))
         );
      } catch (Throwable var2) {
      }
   }
}
