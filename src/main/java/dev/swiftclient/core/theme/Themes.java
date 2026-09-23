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

      try {
         label94: {
            Object var13;
            try (InputStream in = Themes.class.getResourceAsStream("/swiftclient/themes.json")) {
               if (in != null) {
                  JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                  JsonArray arr = root.getAsJsonArray("themes");
                  if (arr == null) {
                     return out;
                  }

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
                  break label94;
               }

               var13 = out;
            }

            return (List<Theme>)var13;
         }
      } catch (Exception var12) {
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
         } catch (NumberFormatException var5) {
            return def;
         }
      }
   }
}
