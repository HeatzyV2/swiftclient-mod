package dev.swiftclient.core.partner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.swiftclient.core.cosmetics.CosmeticHttp;
import dev.swiftclient.core.platform.Platform;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PartnerServers {
   private static final String HIDDEN_KEY = "partners.hidden";
   /** Last list served by the backend. "partners.cache" (older builds) may hold another client's list: ignored. */
   private static final String CACHE_KEY = "partners.cache.v2";
   private static final String OBSOLETE_CACHE_KEY = "partners.cache";
   private static final long REFRESH_MS = 300000L;
   private static volatile List<PartnerServer> cache;
   private static volatile long lastFetch;
   private static volatile boolean fetching;
   private static volatile Set<String> adresses = new HashSet<>();
   private static Set<String> hidden;
   private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "swiftclient-partners");
      t.setDaemon(true);
      return t;
   });

   private PartnerServers() {
   }

   private static void ensureFresh() {
      long now = System.currentTimeMillis();
      if (!fetching) {
         if (cache == null || now - lastFetch >= 300000L) {
            fetching = true;
            IO.execute(() -> {
               try {
                  List<PartnerServer> got = fetchApi();
                  if (!got.isEmpty()) {
                     cache = got;
                     enregistre(got);
                  } else if (cache == null) {
                     cache = disque();
                  }

                  majAdresses(cache);
                  lastFetch = System.currentTimeMillis();
               } catch (Throwable var4) {
                  if (cache == null) {
                     cache = disque();
                  }
               } finally {
                  fetching = false;
               }
            });
         }
      }
   }

   private static List<PartnerServer> fetchApi() {
      List<PartnerServer> out = new ArrayList<>();

      for (JsonElement el : CosmeticHttp.ingamePartners()) {
         if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            String name = str(o, "name");
            String ip = str(o, "ip");
            if (!name.isEmpty() && !ip.isEmpty()) {
               int port = o.has("port") && o.get("port").isJsonPrimitive() ? o.get("port").getAsInt() : 25565;
               out.add(new PartnerServer(name, ip, port, ""));
            }
         }
      }

      return out;
   }

   private static List<PartnerServer> disque() {
      try {
         if (Platform.game().getConfig(OBSOLETE_CACHE_KEY, null) != null) {
            Platform.game().setConfig(OBSOLETE_CACHE_KEY, null);
         }

         String json = Platform.game().getConfig(CACHE_KEY, "");
         if (json != null && !json.isBlank()) {
            List<PartnerServer> out = new ArrayList<>();

            for (JsonElement el : new JsonParser().parse(json).getAsJsonArray()) {
               if (el.isJsonObject()) {
                  JsonObject o = el.getAsJsonObject();
                  String ip = str(o, "ip");
                  if (!ip.isEmpty()) {
                     int port = o.has("port") && o.get("port").isJsonPrimitive() ? o.get("port").getAsInt() : 25565;
                     out.add(new PartnerServer(str(o, "name"), ip, port, str(o, "description")));
                  }
               }
            }

            return out;
         } else {
            return List.of();
         }
      } catch (Throwable var7) {
         return List.of();
      }
   }

   private static void enregistre(List<PartnerServer> l) {
      try {
         JsonArray arr = new JsonArray();

         for (PartnerServer s : l) {
            JsonObject o = new JsonObject();
            o.addProperty("name", s.name());
            o.addProperty("ip", s.ip());
            o.addProperty("port", s.port());
            o.addProperty("description", s.description());
            arr.add(o);
         }

         Platform.game().setConfig(CACHE_KEY, arr.toString());
      } catch (Throwable var5) {
      }
   }

   public static void prechauffe() {
      ensureFresh();
   }

   private static void majAdresses(List<PartnerServer> slots) {
      Set<String> set = new HashSet<>();
      if (slots != null) {
         for (PartnerServer s : slots) {
            set.add(normalise(s.ip(), s.port()));
         }
      }

      for (JsonElement el : CosmeticHttp.partnerServers()) {
         if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            String ip = str(o, "ip");
            if (!ip.isEmpty()) {
               int port = o.has("port") && o.get("port").isJsonPrimitive() ? o.get("port").getAsInt() : 25565;
               set.add(normalise(ip, port));
            }
         }
      }

      if (!set.isEmpty()) {
         adresses = set;
      }
   }

   static String normalise(String adresse) {
      if (adresse == null) {
         return "";
      } else {
         String a = adresse.trim().toLowerCase(Locale.ROOT);
         if (a.isEmpty()) {
            return "";
         } else {
            int i = a.lastIndexOf(58);
            if (i > 0 && i < a.length() - 1) {
               String fin = a.substring(i + 1);
               if (fin.chars().allMatch(Character::isDigit)) {
                  try {
                     return normalise(a.substring(0, i), Integer.parseInt(fin));
                  } catch (NumberFormatException var5) {
                  }
               }
            }

            return normalise(a, 25565);
         }
      }
   }

   static String normalise(String hote, int port) {
      String h = hote == null ? "" : hote.trim().toLowerCase(Locale.ROOT);
      if (h.endsWith(".")) {
         h = h.substring(0, h.length() - 1);
      }

      return h + ":" + (port <= 0 ? 25565 : port);
   }

   public static boolean pret() {
      return lastFetch > 0L;
   }

   /** Backend list if any, else its last cached copy, else the list shipped in the jar. */
   private static List<PartnerServer> current() {
      List<PartnerServer> cur = cache;
      if (cur == null || cur.isEmpty()) {
         cur = disque();
      }

      if (cur.isEmpty()) {
         cur = bundled();
      }

      return cur;
   }

   private static volatile List<PartnerServer> bundled;

   private static List<PartnerServer> bundled() {
      List<PartnerServer> b = bundled;
      if (b == null) {
         b = load();
         bundled = b;
      }

      return b;
   }

   public static List<PartnerServer> all() {
      ensureFresh();
      List<PartnerServer> cur = current();

      Set<String> h = hidden();
      if (h.isEmpty()) {
         return cur;
      } else {
         List<PartnerServer> out = new ArrayList<>(cur.size());

         for (PartnerServer s : cur) {
            if (!h.contains(s.address().toLowerCase(Locale.ROOT))) {
               out.add(s);
            }
         }

         return out;
      }
   }

   public static boolean isPartnerAddress(String address) {
      if (address == null) {
         return false;
      } else {
         ensureFresh();
         String cle = normalise(address);
         if (cle.isEmpty()) {
            return false;
         } else if (adresses.contains(cle)) {
            return true;
         } else {
            List<PartnerServer> cur = current();

            for (PartnerServer s : cur) {
               if (normalise(s.ip(), s.port()).equals(cle)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public static boolean isPinnedAddress(String address) {
      if (address == null) {
         return false;
      } else {
         String cle = normalise(address);
         if (cle.isEmpty()) {
            return false;
         } else {
            for (PartnerServer s : all()) {
               if (normalise(s.ip(), s.port()).equals(cle)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public static void hide(String address) {
      if (address != null && !address.isBlank()) {
         Set<String> h = new HashSet<>(hidden());
         h.add(address.trim().toLowerCase(Locale.ROOT));
         hidden = h;

         try {
            Platform.game().setConfig("partners.hidden", String.join(",", h));
         } catch (Throwable var3) {
         }
      }
   }

   private static Set<String> hidden() {
      if (hidden != null) {
         return hidden;
      } else {
         Set<String> out = new HashSet<>();

         try {
            for (String a : Platform.game().getConfig("partners.hidden", "").split(",")) {
               String t = a.trim().toLowerCase(Locale.ROOT);
               if (!t.isEmpty()) {
                  out.add(t);
               }
            }
         } catch (Throwable var6) {
            return out;
         }

         hidden = out;
         return out;
      }
   }

   private static List<PartnerServer> load() {
      List<PartnerServer> out = new ArrayList<>();

      try {
         label102: {
            Object var13;
            try (InputStream in = PartnerServers.class.getResourceAsStream("/swiftclient/partners.json")) {
               if (in != null) {
                  JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                  JsonArray arr = root.getAsJsonArray("servers");
                  if (arr == null) {
                     return out;
                  }

                  for (JsonElement el : arr) {
                     if (el.isJsonObject()) {
                        JsonObject o = el.getAsJsonObject();
                        String name = str(o, "name");
                        String ip = str(o, "ip");
                        if (!name.isEmpty() && !ip.isEmpty()) {
                           int port = o.has("port") && o.get("port").isJsonPrimitive() ? o.get("port").getAsInt() : 25565;
                           out.add(new PartnerServer(name, ip, port, str(o, "description")));
                        }
                     }
                  }
                  break label102;
               }

               var13 = out;
            }

            return (List<PartnerServer>)var13;
         }
      } catch (Exception var12) {
      }

      return out;
   }

   private static String str(JsonObject o, String key) {
      return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
   }
}
