package dev.swiftclient.core.mods;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.swiftclient.core.config.ConfigCodec;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.platform.Platform;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;

public final class Profiles {
   private static final Logger LOG = Log.get("Profiles");
   public static final String DEFAUT = "Default";
   private static final Map<String, Map<String, Map<String, String>>> PROFILS = new LinkedHashMap<>();
   private static String actif;
   private static boolean charge;

   private Profiles() {
   }

   /** Test support: forget everything loaded. */
   static synchronized void resetForTests() {
      PROFILS.clear();
      actif = null;
      charge = false;
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
            Module m = ModuleManager.byId(en.getKey());
            if (m != null && "1".equals(en.getValue().get("enabled"))) {
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
      } catch (Throwable ignored) {
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
         } catch (Throwable ignored) {
         }
      }
   }

   /** Profiles live in the "profiles" section of swiftclient.json (see ConfigStore / ConfigCodec). */
   private static void charger() {
      if (!charge) {
         charge = true;

         try {
            JsonObject section = Platform.game().config().profiles();
            if (section != null) {
               JsonElement list = section.get("list");
               if (list != null && list.isJsonObject()) {
                  for (Entry<String, JsonElement> p : list.getAsJsonObject().entrySet()) {
                     if (p.getValue().isJsonObject()) {
                        PROFILS.put(p.getKey(), ConfigCodec.profileFromJson(p.getValue().getAsJsonObject()));
                     } else {
                        LOG.warn("Profil '{}' illisible, ignore", p.getKey());
                     }
                  }
               }

               if (section.has("active") && section.get("active").isJsonPrimitive()) {
                  actif = section.get("active").getAsString();
               }
            }
         } catch (Exception e) {
            LOG.error("Profils illisibles, repli sur Default", e);
            PROFILS.clear();
         }

         boolean normalises = normaliser();
         if (PROFILS.isEmpty()) {
            PROFILS.put("Default", capturer());
            actif = "Default";
            ecrire();
         }

         if (actif == null || !PROFILS.containsKey(actif)) {
            actif = PROFILS.keySet().iterator().next();
         }

         if (normalises) {
            ecrire();
         }
      }
   }

   /**
    * Profiles saved by older builds hold cycle settings as positions ("1.0"). Rewrites them as option keys
    * using the module definitions. Returns true if anything changed.
    */
   private static boolean normaliser() {
      boolean changed = false;

      for (Map<String, Map<String, String>> profil : PROFILS.values()) {
         for (Entry<String, Map<String, String>> section : profil.entrySet()) {
            Module m = "hud".equals(section.getKey()) ? null : ModuleManager.byId(section.getKey());
            if (m != null) {
               for (Entry<String, String> kv : section.getValue().entrySet()) {
                  ModuleSetting setting = m.setting(kv.getKey());
                  if (setting != null && setting.type == ModuleSetting.Type.CYCLE && kv.getValue() != null) {
                     String[] keys = setting.optionKeys();

                     try {
                        int index = (int)Double.parseDouble(kv.getValue());
                        if (index >= 0 && index < keys.length) {
                           kv.setValue(keys[index]);
                           changed = true;
                        }
                     } catch (NumberFormatException ignored) {
                        // Already a key.
                     }
                  }
               }
            }
         }
      }

      return changed;
   }

   private static JsonObject section() {
      JsonObject list = new JsonObject();

      for (Entry<String, Map<String, Map<String, String>>> p : PROFILS.entrySet()) {
         list.add(p.getKey(), ConfigCodec.profileToJson(p.getValue()));
      }

      JsonObject section = new JsonObject();
      section.addProperty("active", actif);
      section.add("list", list);
      return section;
   }

   private static void ecrire() {
      try {
         Platform.game().config().setProfiles(section());
      } catch (Exception e) {
         LOG.error("Profils non enregistres", e);
      }
   }

   // --- Sharing: a profile as a short text code ---

   private static final String CODE_PREFIX = "SWIFT1.";
   /** Refuse codes that would inflate to something absurd. */
   private static final int CODE_MAX_BYTES = 256 * 1024;

   /** Code that recreates profile {@code nom} anywhere: "SWIFT1." + base64url(gzip(json)). */
   public static synchronized String codePartage(String nom) {
      charger();
      if (nom.equals(actif)) {
         memoriser();
      }

      Map<String, Map<String, String>> p = PROFILS.get(nom);
      if (p == null) {
         throw new IllegalArgumentException("Profil inconnu : " + nom);
      }

      JsonObject doc = new JsonObject();
      doc.addProperty("v", ConfigCodec.SCHEMA_VERSION);
      doc.addProperty("name", nom);
      doc.add("profile", ConfigCodec.profileToJson(p));

      try {
         ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         try (GZIPOutputStream gz = new GZIPOutputStream(bytes)) {
            gz.write(doc.toString().getBytes(StandardCharsets.UTF_8));
         }

         return CODE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
      } catch (IOException e) {
         throw new IllegalStateException(e);
      }
   }

   /**
    * Adds the profile carried by {@code code} (not activated). Returns its name, made unique if needed.
    *
    * @throws IllegalArgumentException with a message for the player if the code is not valid
    */
   public static synchronized String importerCode(String code) {
      charger();
      String c = code == null ? "" : code.trim();
      if (!c.startsWith(CODE_PREFIX)) {
         throw new IllegalArgumentException("Ce n'est pas un code de profil Swift Client");
      }

      JsonObject doc;
      try {
         byte[] gz = Base64.getUrlDecoder().decode(c.substring(CODE_PREFIX.length()));
         byte[] json;
         try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(gz))) {
            json = in.readNBytes(CODE_MAX_BYTES + 1);
         }

         if (json.length > CODE_MAX_BYTES) {
            throw new IllegalArgumentException("Code de profil trop volumineux");
         }

         doc = JsonParser.parseString(new String(json, StandardCharsets.UTF_8)).getAsJsonObject();
      } catch (IllegalArgumentException e) {
         throw e.getMessage() != null && e.getMessage().startsWith("Code") ? e : new IllegalArgumentException("Code de profil invalide ou incomplet");
      } catch (Exception e) {
         throw new IllegalArgumentException("Code de profil invalide ou incomplet");
      }

      if (!doc.has("profile") || !doc.get("profile").isJsonObject()) {
         throw new IllegalArgumentException("Code de profil invalide ou incomplet");
      } else if (doc.has("v") && doc.get("v").getAsInt() > ConfigCodec.SCHEMA_VERSION) {
         throw new IllegalArgumentException("Ce profil vient d'une version plus recente de Swift Client");
      }

      String base = doc.has("name") && doc.get("name").isJsonPrimitive() ? doc.get("name").getAsString().trim() : "";
      if (base.isEmpty() || base.length() > 32) {
         base = "Profil importe";
      }

      String nom = base;
      for (int i = 2; PROFILS.containsKey(nom); i++) {
         nom = base + " (" + i + ")";
      }

      PROFILS.put(nom, ConfigCodec.profileFromJson(doc.getAsJsonObject("profile")));
      normaliser();
      ecrire();
      return nom;
   }
}
