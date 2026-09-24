package dev.swiftclient.core.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Converts the pre-v2 files ({@code swiftclient.properties} + {@code swiftclient-profiles.json}) into the v2
 * document. Pure: no IO, so it is tested on its own. Every key that is dropped is reported in
 * {@code warnings}; nothing here throws on bad input.
 */
public final class ConfigMigration {
   /** Modules that existed in older builds without any behaviour; their values mean nothing. */
   static final Set<String> REMOVED_MODULES = Set.of(
      "shulker_preview", "quick_swap", "dynamic_lights", "food_status", "death_point", "hit_sounds", "old_animations",
      "particle_multiplier", "auto_reconnect", "streamer_mode", "chat_tools", "macro_keybinds"
   );
   /** Single keys that are no longer used. */
   static final Set<String> REMOVED_KEYS = Set.of(
      "partners.cache", // list cached from a previous backend; the v2 cache has its own key
      "mod.togglesprint.backwards" // vanilla cannot sprint backwards, the setting never did anything
   );

   private ConfigMigration() {
   }

   /**
    * @param properties  every key of swiftclient.properties (may be empty)
    * @param profiles    root of swiftclient-profiles.json, or null if absent / unreadable
    * @param warnings    receives one line per ignored key or profile entry
    */
   public static JsonObject fromLegacy(Map<String, String> properties, JsonObject profiles, List<String> warnings) {
      JsonObject root = ConfigCodec.newRoot();

      // Sorted so the document is stable whatever order Properties returned.
      for (Entry<String, String> e : new TreeMap<>(properties).entrySet()) {
         String why = obsolete(e.getKey());
         if (why != null) {
            warnings.add(e.getKey() + " : " + why);
         } else if (e.getValue() == null) {
            warnings.add(e.getKey() + " : valeur vide");
         } else {
            try {
               ConfigCodec.put(root, e.getKey(), e.getValue());
            } catch (RuntimeException ex) {
               warnings.add(e.getKey() + " : valeur illisible '" + e.getValue() + "' (" + ex.getMessage() + ")");
            }
         }
      }

      if (profiles != null) {
         root.add(ConfigCodec.PROFILES, migrateProfiles(profiles, warnings));
      }

      return root;
   }

   /** Null if the key is still meaningful, otherwise why it is dropped. */
   static String obsolete(String key) {
      if (REMOVED_KEYS.contains(key)) {
         return "cle obsolete ignoree";
      } else {
         if (key.startsWith("mod.")) {
            int dot = key.indexOf('.', 4);
            if (dot > 4 && REMOVED_MODULES.contains(key.substring(4, dot))) {
               return "module supprime (" + key.substring(4, dot) + "), ignore";
            }
         }

         return null;
      }
   }

   /** Legacy {"active", "profiles": {name: {section: {k: v}}}} -> v2 {"active", "list": {name: document}}. */
   static JsonObject migrateProfiles(JsonObject legacy, List<String> warnings) {
      JsonObject out = new JsonObject();
      JsonObject list = new JsonObject();
      JsonElement profiles = legacy.get("profiles");
      if (profiles != null && profiles.isJsonObject()) {
         for (Entry<String, JsonElement> p : profiles.getAsJsonObject().entrySet()) {
            if (!p.getValue().isJsonObject()) {
               warnings.add("profil '" + p.getKey() + "' : format illisible, ignore");
            } else {
               JsonObject doc = new JsonObject();

               for (Entry<String, JsonElement> sec : p.getValue().getAsJsonObject().entrySet()) {
                  if (!sec.getValue().isJsonObject()) {
                     warnings.add("profil '" + p.getKey() + "', section '" + sec.getKey() + "' : format illisible, ignoree");
                  } else if (REMOVED_MODULES.contains(sec.getKey())) {
                     warnings.add("profil '" + p.getKey() + "' : module supprime (" + sec.getKey() + "), ignore");
                  } else {
                     for (Entry<String, JsonElement> e : sec.getValue().getAsJsonObject().entrySet()) {
                        String flat = "hud".equals(sec.getKey()) ? e.getKey() : "mod." + sec.getKey() + "." + e.getKey();
                        if (REMOVED_KEYS.contains(flat)) {
                           warnings.add("profil '" + p.getKey() + "' : " + flat + " obsolete, ignoree");
                        } else if (!e.getValue().isJsonPrimitive()) {
                           warnings.add("profil '" + p.getKey() + "' : " + flat + " illisible, ignoree");
                        } else {
                           try {
                              ConfigCodec.put(doc, flat, e.getValue().getAsString());
                           } catch (RuntimeException ex) {
                              warnings.add("profil '" + p.getKey() + "' : " + flat + " illisible (" + ex.getMessage() + "), ignoree");
                           }
                        }
                     }
                  }
               }

               list.add(p.getKey(), doc);
            }
         }
      } else if (profiles != null) {
         warnings.add("profils : format illisible, ignores");
      }

      JsonElement active = legacy.get("active");
      if (active != null && active.isJsonPrimitive() && list.has(active.getAsString())) {
         out.addProperty("active", active.getAsString());
      } else if (!list.keySet().isEmpty()) {
         out.addProperty("active", list.keySet().iterator().next());
      }

      out.add("list", list);
      return out;
   }
}
