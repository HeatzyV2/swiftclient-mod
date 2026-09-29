package dev.swiftclient.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.swiftclient.core.platform.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Waypoints, saved per server (or per singleplayer world) in swiftclient.json under
 * {@code waypoints.<server>}. Each one belongs to a dimension and is only shown there.
 */
public final class Waypoints {
   private static final int[] PALETTE = {0xFF3B82F6, 0xFFFF6B6B, 0xFF6BD36B, 0xFFF2C94C, 0xFFB47CFF, 0xFF4DD0E1, 0xFFFF9F43};
   private static String loadedFor;
   private static List<Waypoint> list = new ArrayList<>();

   private Waypoints() {
   }

   public record Waypoint(String name, int x, int y, int z, String dimension, int color) {
   }

   /** "mc.hypixel.net" or "sp:<world name>", empty out of a world. */
   private static String context() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return "";
      } else if (mc.getSingleplayerServer() != null) {
         return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
      } else {
         ServerData sd = mc.getCurrentServer();
         return sd == null ? "unknown" : sd.ip.toLowerCase(Locale.ROOT);
      }
   }

   private static String key(String ctx) {
      return "waypoints." + ctx.replaceAll("[^a-zA-Z0-9_.:-]", "_");
   }

   private static String dimension() {
      Minecraft mc = Minecraft.getInstance();
      return mc.level == null ? "" : mc.level.dimension().identifier().toString();
   }

   private static List<Waypoint> all() {
      String ctx = context();
      if (!ctx.equals(loadedFor)) {
         loadedFor = ctx;
         list = ctx.isEmpty() ? new ArrayList<>() : read(Platform.game().getConfig(key(ctx), "[]"));
      }

      return list;
   }

   private static List<Waypoint> read(String raw) {
      List<Waypoint> out = new ArrayList<>();
      try {
         JsonElement root = JsonParser.parseString(raw);
         if (root.isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray()) {
               JsonObject o = e.getAsJsonObject();
               out.add(new Waypoint(
                  o.get("name").getAsString(), o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt(), o.get("dim").getAsString(), o.get("color").getAsInt()
               ));
            }
         }
      } catch (RuntimeException ignored) {
         // A damaged entry must not break the game: start from what could be read.
      }

      return out;
   }

   private static void save() {
      if (loadedFor != null && !loadedFor.isEmpty()) {
         JsonArray arr = new JsonArray();
         for (Waypoint w : list) {
            JsonObject o = new JsonObject();
            o.addProperty("name", w.name());
            o.addProperty("x", w.x());
            o.addProperty("y", w.y());
            o.addProperty("z", w.z());
            o.addProperty("dim", w.dimension());
            o.addProperty("color", w.color());
            arr.add(o);
         }

         Platform.game().setConfig(key(loadedFor), arr.toString());
      }
   }

   /** Waypoints of the current dimension. */
   public static List<Waypoint> visibleHere() {
      String dim = dimension();
      List<Waypoint> out = new ArrayList<>();
      for (Waypoint w : all()) {
         if (w.dimension().equals(dim)) {
            out.add(w);
         }
      }

      return out;
   }

   /** Adds a waypoint at your feet, named {@code name} or "Waypoint N" when blank. Returns it, or null out of a world. */
   public static Waypoint addHere(String name) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return null;
      }

      List<Waypoint> l = all();
      String n = name == null || name.isBlank() ? "Waypoint " + (l.size() + 1) : name.trim();
      Waypoint w = new Waypoint(n, mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ(), dimension(), PALETTE[l.size() % PALETTE.length]);
      l.add(w);
      save();
      return w;
   }

   /** Removes the waypoint closest to you in this dimension. Returns it, or null when there is none. */
   public static Waypoint removeNearest() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return null;
      }

      Waypoint best = null;
      double bestD = Double.MAX_VALUE;
      for (Waypoint w : visibleHere()) {
         double d = mc.player.distanceToSqr(w.x() + 0.5, w.y(), w.z() + 0.5);
         if (d < bestD) {
            bestD = d;
            best = w;
         }
      }

      if (best != null) {
         all().remove(best);
         save();
      }

      return best;
   }

   /** Removes every waypoint of this server / world. */
   public static int clearHere() {
      List<Waypoint> l = all();
      int n = l.size();
      l.clear();
      save();
      return n;
   }

   /** Called on disconnect: the next world loads its own list. */
   public static void forget() {
      loadedFor = null;
      list = new ArrayList<>();
   }
}
