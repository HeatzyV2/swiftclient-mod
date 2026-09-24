package dev.swiftclient.core.pet;

import com.google.gson.JsonObject;
import dev.swiftclient.core.cosmetics.CosmeticHttp;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DynamicPets {
   private static volatile DynamicPets.Sink sink;
   private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "swiftclient-dynpets");
      t.setDaemon(true);
      return t;
   });
   private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
   private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

   private DynamicPets() {
   }

   public static void setSink(DynamicPets.Sink s) {
      sink = s;
   }

   public static void ensure(String petId) {
      if (petId != null && !petId.isEmpty()) {
         if (!PetRegistry.exists(petId)) {
            String key = petId.toLowerCase(Locale.ROOT);
            if (!FAILED.contains(key) && LOADING.add(key)) {
               IO.execute(() -> {
                  try {
                     JsonObject b = CosmeticHttp.petModel(petId);
                     if (b == null || !b.has("geo") || b.get("geo").isJsonNull()) {
                        FAILED.add(key);
                        return;
                     }

                     String geo = b.get("geo").toString();
                     String anim = b.has("animation") && !b.get("animation").isJsonNull() ? b.get("animation").toString() : null;
                     byte[] tex = b.has("texture") && !b.get("texture").isJsonNull() ? Base64.getDecoder().decode(b.get("texture").getAsString()) : null;
                     String loop = b.has("loopAnim") ? b.get("loopAnim").getAsString() : "idle";
                     boolean playerSkin = b.has("playerSkin") && b.get("playerSkin").getAsBoolean();
                     DynamicPets.Sink s = sink;
                     if (s != null) {
                        s.inject(key, geo, anim, playerSkin ? null : tex);
                        PetRegistry.register(new PetRegistry.PetDef(key, loop, playerSkin));
                        System.out.println("[SwiftClient/Pet] modèle servi chargé : " + petId + " (loop=" + loop + ")");
                        return;
                     }

                     FAILED.add(key);
                     System.out.println("[SwiftClient/Pet] pas de Sink geckolib (version)");
                  } catch (Exception var12) {
                     FAILED.add(key);
                     System.out.println("[SwiftClient/Pet] échec chargement modèle servi " + petId + " : " + var12.getMessage());
                     return;
                  } finally {
                     LOADING.remove(key);
                  }
               });
            }
         }
      }
   }

   public interface Sink {
      void inject(String var1, String var2, String var3, byte[] var4);
   }
}
