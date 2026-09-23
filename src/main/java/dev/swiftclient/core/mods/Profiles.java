package dev.swiftclient.core.mods;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.platform.Platform;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class Profiles {
   private static final String FICHIER = "swiftclient-profiles.json";
   public static final String DEFAUT = "Default";
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Map<String, Map<String, Map<String, String>>> PROFILS = new LinkedHashMap<>();
   private static String actif;
   private static boolean charge;

   private Profiles() {
   }

   public static synchronized List<String> noms() {
      charger();
      return new ArrayList<>(PROFILS.keySet());
   }

   public static synchronized String actif() {
      charger();
      return actif;
   }

   public static synchronized int modulesActifs(String nom) {
      charger();
      Map<String, Map<String, String>> p = PROFILS.get(nom);
      if (p == null) {
         return 0;
      } else {
         int n = 0;

         for (Entry<String, Map<String, String>> en : p.entrySet()) {
            if (!"hud".equals(en.getKey()) && "1".equals(en.getValue().get("enabled"))) {
               n++;
            }
         }

         return n;
      }
   }

   public static synchronized boolean creer(String nom) {
      charger();
      nom = nom == null ? "" : nom.trim();
      if (!nom.isEmpty() && !PROFILS.containsKey(nom)) {
         memoriser();
         PROFILS.put(nom, capturer());
         actif = nom;
         ecrire();
         return true;
      } else {
         return false;
      }
   }

   public static synchronized void activer(String nom) {
      charger();
      if (nom != null && !nom.equals(actif) && PROFILS.containsKey(nom)) {
         memoriser();
         actif = nom;
         appliquer(PROFILS.get(nom));
         ecrire();
      }
   }

   public static synchronized boolean supprimer(String nom) {
      charger();
      if (nom != null && !nom.equals(actif) && PROFILS.size() > 1) {
         boolean ok = PROFILS.remove(nom) != null;
         if (ok) {
            ecrire();
         }

         return ok;
      } else {
         return false;
      }
   }

   public static synchronized void memoriser() {
      charger();
      if (actif != null) {
         PROFILS.put(actif, capturer());
      }
   }

   private static Map<String, Map<String, String>> capturer() {
      Map<String, Map<String, String>> out = new LinkedHashMap<>();

      for (Module m : ModuleManager.modules()) {
         out.put(m.id, m.exporter());
      }

      try {
         out.put("hud", HudManager.exporter());
      } catch (Throwable var3) {
      }

      return out;
   }

   private static void appliquer(Map<String, Map<String, String>> p) {
      if (p != null) {
         for (Module m : ModuleManager.modules()) {
            m.importer(p.get(m.id));
         }

         try {
            HudManager.importer(p.get("hud"));
         } catch (Throwable var3) {
         }
      }
   }

   private static Path fichier() {
      return Platform.game().configDir().resolve("swiftclient-profiles.json");
   }

   private static void charger() {
      if (!charge) {
         charge = true;

         try {
            Path f = fichier();
            if (Files.isRegularFile(f)) {
               JsonObject racine = new JsonParser().parse(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
               JsonObject profils = racine.has("profiles") ? racine.getAsJsonObject("profiles") : new JsonObject();

               for (Entry<String, JsonElement> p : profils.entrySet()) {
                  Map<String, Map<String, String>> sections = new LinkedHashMap<>();

                  for (Entry<String, JsonElement> sec : p.getValue().getAsJsonObject().entrySet()) {
                     Map<String, String> cles = new LinkedHashMap<>();

                     for (Entry<String, JsonElement> kv : sec.getValue().getAsJsonObject().entrySet()) {
                        cles.put(kv.getKey(), kv.getValue().isJsonNull() ? "" : kv.getValue().getAsString());
                     }

                     sections.put(sec.getKey(), cles);
                  }

                  PROFILS.put(p.getKey(), sections);
               }

               if (racine.has("active")) {
                  actif = racine.get("active").getAsString();
               }
            }
         } catch (Exception var12) {
            System.out.println("[SwiftClient] profils illisibles, repli sur Default : " + var12);
            PROFILS.clear();

            try {
               Files.copy(fichier(), fichier().resolveSibling("swiftclient-profiles.json.bak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception var11) {
            }
         }

         if (PROFILS.isEmpty()) {
            PROFILS.put("Default", capturer());
            actif = "Default";
            ecrire();
         }

         if (actif == null || !PROFILS.containsKey(actif)) {
            actif = PROFILS.keySet().iterator().next();
         }
      }
   }

   private static void ecrire() {
      try {
         JsonObject racine = new JsonObject();
         racine.addProperty("active", actif);
         racine.add("profiles", GSON.toJsonTree(PROFILS));
         Path f = fichier();
         Files.createDirectories(f.getParent());
         Path tmp = f.resolveSibling("swiftclient-profiles.json.tmp");
         Files.writeString(tmp, GSON.toJson(racine), StandardCharsets.UTF_8);

         try {
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException var4) {
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (Exception var5) {
         System.out.println("[SwiftClient] profils non enregistres : " + var5);
      }
   }
}
