package dev.swiftclient.core.partner;

import com.google.gson.JsonObject;
import dev.swiftclient.core.cosmetics.CosmeticHttp;
import dev.swiftclient.core.platform.Platform;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PartnerTracking {
   private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "swiftclient-partner-tracking");
      t.setDaemon(true);
      return t;
   });
   private static final long GRACE_MS = 2000L;
   private static volatile String serveurCourant = null;

   private PartnerTracking() {
   }

   public static String serveurCourant() {
      return serveurCourant;
   }

   public static void onJoinedServer(String address) {
      if (address != null && !address.isBlank()) {
         String addr = address.trim();
         String uuid = Platform.game().getUuid();
         IO.execute(() -> {
            if (!PartnerServers.isPartnerAddress(addr)) {
               if (PartnerServers.pret()) {
                  serveurCourant = null;
                  return;
               }

               try {
                  Thread.sleep(2000L);
               } catch (InterruptedException var3) {
                  return;
               }

               if (!PartnerServers.isPartnerAddress(addr)) {
                  serveurCourant = null;
                  return;
               }
            }

            serveurCourant = addr;
            JsonObject b = new JsonObject();
            b.addProperty("uuid", uuid);
            b.addProperty("address", addr);
            CosmeticHttp.postAuthed("/api/partners/join", b);
         });
      } else {
         serveurCourant = null;
      }
   }
}
