package dev.swiftclient.core.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Maps the flat keys used by the code ({@code mod.zoom.fov}, {@code hud_pos_fps}, {@code theme.current}...)
 * to the structured, typed v2 document and back:
 *
 * <pre>
 * {
 *   "schemaVersion": 2,
 *   "modules": { "zoom": { "enabled": true, "settings": { "mode": "hold", "fov": 30.0 } } },
 *   "hud": { "scale": 2.0, "elements": { "fps": { "pos": [0.02, 0.03], "anchor": [0, 0], "scale": 1.0 } } },
 *   "general": { "theme.current": "bloomy" },
 *   "profiles": { "active": "Default", "list": { "Default": { "modules": {...}, "hud": {...} } } }
 * }
 * </pre>
 *
 * Values: "1"/"0" become booleans, decimal numbers become numbers, everything else stays a string;
 * reading gives back the exact string the code expects.
 */
public final class ConfigCodec {
   public static final int SCHEMA_VERSION = 2;
   public static final String MODULES = "modules";
   public static final String HUD = "hud";
   public static final String GENERAL = "general";
   public static final String PROFILES = "profiles";
   private static final Pattern DECIMAL = Pattern.compile("-?\\d+\\.\\d+(E-?\\d+)?");

   private ConfigCodec() {
   }

   public static JsonObject newRoot() {
      JsonObject root = new JsonObject();
      root.addProperty("schemaVersion", SCHEMA_VERSION);
      root.add(MODULES, new JsonObject());
      root.add(HUD, new JsonObject());
      root.add(GENERAL, new JsonObject());
      return root;
   }

   // --- Values ---

   static JsonElement toJson(String v) {
      if ("1".equals(v)) {
         return new JsonPrimitive(true);
      } else if ("0".equals(v)) {
         return new JsonPrimitive(false);
      } else {
         return DECIMAL.matcher(v).matches() ? new JsonPrimitive(new BigDecimal(v)) : new JsonPrimitive(v);
      }
   }

   static String fromJson(JsonElement e) {
      if (e == null || e.isJsonNull()) {
         return null;
      } else if (e.isJsonPrimitive()) {
         JsonPrimitive p = e.getAsJsonPrimitive();
         if (p.isBoolean()) {
            return p.getAsBoolean() ? "1" : "0";
         } else {
            return p.isNumber() ? Double.toString(p.getAsDouble()) : p.getAsString();
         }
      } else {
         return e.toString();
      }
   }

   private static JsonPrimitive floatJson(String s) {
      return new JsonPrimitive(new BigDecimal(Float.toString(Float.parseFloat(s.trim()))));
   }

   private static String floatString(JsonElement e) {
      return Float.toString(e.getAsFloat());
   }

   // --- Flat key <-> location ---

   private static JsonObject obj(JsonObject parent, String name) {
      JsonElement e = parent.get(name);
      if (e == null || !e.isJsonObject()) {
         JsonObject o = new JsonObject();
         parent.add(name, o);
         return o;
      } else {
         return e.getAsJsonObject();
      }
   }

   private static JsonObject objOrNull(JsonObject parent, String name) {
      JsonElement e = parent == null ? null : parent.get(name);
      return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
   }

   /** "mod.zoom.fov" -> ["zoom", "fov"]; null for keys that are not module keys. */
   private static String[] moduleKey(String key) {
      if (!key.startsWith("mod.")) {
         return null;
      } else {
         int dot = key.indexOf('.', 4);
         return dot <= 4 || dot == key.length() - 1 ? null : new String[]{key.substring(4, dot), key.substring(dot + 1)};
      }
   }

   /**
    * Stores {@code value} for flat {@code key} in {@code root}.
    *
    * @throws IllegalArgumentException if a HUD value is malformed (the migrator reports and skips it)
    */
   public static void put(JsonObject root, String key, String value) {
      String[] mk = moduleKey(key);
      if (mk != null) {
         JsonObject m = obj(obj(root, MODULES), mk[0]);
         if ("enabled".equals(mk[1])) {
            m.add("enabled", new JsonPrimitive("1".equals(value) || "true".equalsIgnoreCase(value)));
         } else {
            obj(m, "settings").add(mk[1], toJson(value));
         }
      } else if ("hud_scale_global".equals(key)) {
         obj(root, HUD).add("scale", floatJson(value));
      } else if (key.startsWith("hud_pos_") || key.startsWith("hud_anchor_") || key.startsWith("hud_scale_")) {
         String field = key.startsWith("hud_pos_") ? "pos" : (key.startsWith("hud_anchor_") ? "anchor" : "scale");
         String id = key.substring(key.indexOf('_', 4) + 1);
         JsonObject el = obj(obj(obj(root, HUD), "elements"), id);
         if ("scale".equals(field)) {
            el.add("scale", floatJson(value));
         } else {
            String[] parts = value.split(",");
            if (parts.length != 2) {
               throw new IllegalArgumentException("deux valeurs attendues : " + value);
            }

            JsonArray arr = new JsonArray();
            for (String p : parts) {
               arr.add("pos".equals(field) ? floatJson(p) : new JsonPrimitive(Integer.parseInt(p.trim())));
            }

            el.add(field, arr);
         }
      } else {
         obj(root, GENERAL).add(key, toJson(value));
      }
   }

   public static String get(JsonObject root, String key) {
      try {
         String[] mk = moduleKey(key);
         if (mk != null) {
            JsonObject m = objOrNull(objOrNull(root, MODULES), mk[0]);
            return "enabled".equals(mk[1]) ? (m != null && m.has("enabled") ? fromJson(m.get("enabled")) : null) : fromJson(child(objOrNull(m, "settings"), mk[1]));
         } else if ("hud_scale_global".equals(key)) {
            JsonElement s = child(objOrNull(root, HUD), "scale");
            return s == null ? null : floatString(s);
         } else if (key.startsWith("hud_pos_") || key.startsWith("hud_anchor_") || key.startsWith("hud_scale_")) {
            String field = key.startsWith("hud_pos_") ? "pos" : (key.startsWith("hud_anchor_") ? "anchor" : "scale");
            String id = key.substring(key.indexOf('_', 4) + 1);
            JsonElement v = child(objOrNull(objOrNull(objOrNull(root, HUD), "elements"), id), field);
            if (v == null) {
               return null;
            } else if ("scale".equals(field)) {
               return floatString(v);
            } else {
               JsonArray a = v.getAsJsonArray();
               return "pos".equals(field) ? floatString(a.get(0)) + "," + floatString(a.get(1)) : a.get(0).getAsInt() + "," + a.get(1).getAsInt();
            }
         } else {
            return fromJson(child(objOrNull(root, GENERAL), key));
         }
      } catch (RuntimeException e) {
         return null;
      }
   }

   private static JsonElement child(JsonObject o, String name) {
      return o == null ? null : o.get(name);
   }

   public static void remove(JsonObject root, String key) {
      String[] mk = moduleKey(key);
      if (mk != null) {
         JsonObject m = objOrNull(objOrNull(root, MODULES), mk[0]);
         if (m != null) {
            if ("enabled".equals(mk[1])) {
               m.remove("enabled");
            } else {
               JsonObject s = objOrNull(m, "settings");
               if (s != null) {
                  s.remove(mk[1]);
               }
            }
         }
      } else if ("hud_scale_global".equals(key)) {
         JsonObject h = objOrNull(root, HUD);
         if (h != null) {
            h.remove("scale");
         }
      } else if (key.startsWith("hud_pos_") || key.startsWith("hud_anchor_") || key.startsWith("hud_scale_")) {
         JsonObject el = objOrNull(objOrNull(objOrNull(root, HUD), "elements"), key.substring(key.indexOf('_', 4) + 1));
         if (el != null) {
            el.remove(key.startsWith("hud_pos_") ? "pos" : (key.startsWith("hud_anchor_") ? "anchor" : "scale"));
         }
      } else {
         JsonObject g = objOrNull(root, GENERAL);
         if (g != null) {
            g.remove(key);
         }
      }
   }

   /** Every flat key/value held by {@code root} (modules, hud, general), in document order. */
   public static Map<String, String> flatten(JsonObject root) {
      Map<String, String> out = new LinkedHashMap<>();
      JsonObject modules = objOrNull(root, MODULES);
      if (modules != null) {
         for (Entry<String, JsonElement> m : modules.entrySet()) {
            if (m.getValue().isJsonObject()) {
               JsonObject mo = m.getValue().getAsJsonObject();
               if (mo.has("enabled")) {
                  out.put("mod." + m.getKey() + ".enabled", fromJson(mo.get("enabled")));
               }

               JsonObject s = objOrNull(mo, "settings");
               if (s != null) {
                  for (Entry<String, JsonElement> e : s.entrySet()) {
                     out.put("mod." + m.getKey() + "." + e.getKey(), fromJson(e.getValue()));
                  }
               }
            }
         }
      }

      JsonObject hud = objOrNull(root, HUD);
      if (hud != null) {
         if (hud.has("scale")) {
            out.put("hud_scale_global", get(root, "hud_scale_global"));
         }

         JsonObject els = objOrNull(hud, "elements");
         if (els != null) {
            for (String id : els.keySet()) {
               for (String prefix : new String[]{"hud_pos_", "hud_anchor_", "hud_scale_"}) {
                  String v = get(root, prefix + id);
                  if (v != null) {
                     out.put(prefix + id, v);
                  }
               }
            }
         }
      }

      JsonObject general = objOrNull(root, GENERAL);
      if (general != null) {
         for (Entry<String, JsonElement> e : general.entrySet()) {
            out.put(e.getKey(), fromJson(e.getValue()));
         }
      }

      return out;
   }

   // --- Profiles: Profiles keeps {section: {key: value}} maps, the file holds typed documents ---

   /** {moduleId: {enabled, setting...}, "hud": {hud_pos_x...}} -> {"modules": ..., "hud": ...} */
   public static JsonObject profileToJson(Map<String, Map<String, String>> profile) {
      JsonObject doc = new JsonObject();

      for (Entry<String, Map<String, String>> section : profile.entrySet()) {
         for (Entry<String, String> kv : section.getValue().entrySet()) {
            if (kv.getValue() != null) {
               String key = "hud".equals(section.getKey()) ? kv.getKey() : "mod." + section.getKey() + "." + kv.getKey();
               put(doc, key, kv.getValue());
            }
         }
      }

      return doc;
   }

   public static Map<String, Map<String, String>> profileFromJson(JsonObject doc) {
      Map<String, Map<String, String>> out = new LinkedHashMap<>();

      for (Entry<String, String> kv : flatten(doc).entrySet()) {
         String[] mk = moduleKey(kv.getKey());
         if (mk != null) {
            out.computeIfAbsent(mk[0], k -> new LinkedHashMap<>()).put(mk[1], kv.getValue());
         } else if (kv.getKey().startsWith("hud_")) {
            out.computeIfAbsent("hud", k -> new LinkedHashMap<>()).put(kv.getKey(), kv.getValue());
         }
      }

      return out;
   }
}
