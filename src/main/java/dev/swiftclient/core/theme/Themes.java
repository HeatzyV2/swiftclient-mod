package dev.swiftclient.core.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Themes {
   private static List<Theme> cache;

   private Themes() {
   }

   public static List<Theme> all() {
      if (cache == null) {
         cache = load();
      }

      return cache;
   }

   public static Theme byId(String id) {
      for (Theme t : all()) {
         if (t.id().equals(id)) {
            return t;
         }
      }

      return null;
   }

   public static Theme byIdOrFirst(String id) {
      Theme t = byId(id);
      if (t != null) {
         return t;
      } else {
         List<Theme> all = all();
         return all.isEmpty() ? null : all.get(0);
      }
   }

   private static List<Theme> load() {
      List<Theme> out = new ArrayList<>();

      try (InputStream in = Themes.class.getResourceAsStream("/swiftclient/themes.json")) {
         if (in != null) {
            JsonArray arr = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("themes");
            if (arr != null) {
               for (JsonElement el : arr) {
                  if (el.isJsonObject()) {
                     JsonObject o = el.getAsJsonObject();
                     String id = str(o, "id");
                     if (!id.isEmpty()) {
                        int top = color(o, "top", -15723496);
                        int bottom = color(o, "bottom", top);
                        out.add(new Theme(id, str(o, "name").isEmpty() ? id : str(o, "name"), str(o, "panorama"), top, bottom));
                     }
                  }
               }
            }
         }
      } catch (Exception ignored) {
         // Broken bundled file: no entries rather than a crash.
      }

      return out;
   }

   private static String str(JsonObject o, String k) {
      return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "";
   }

   private static int color(JsonObject o, String k, int def) {
      String s = str(o, k);
      if (s.isEmpty()) {
         return def;
      } else {
         try {
            return (int)Long.parseLong(s.replaceFirst("(?i)^0x", ""), 16);
         } catch (NumberFormatException ignored) {
            return def;
         }
      }
   }
}
