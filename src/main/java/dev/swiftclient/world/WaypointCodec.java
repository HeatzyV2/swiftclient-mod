package dev.swiftclient.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Waypoints as a short text code to paste to a friend: {@code SWIFTWP1.} + base64url(gzip(json)). */
public final class WaypointCodec {
   public static final String PREFIX = "SWIFTWP1.";
   public static final int MAX_WAYPOINTS = 200;
   private static final int MAX_BYTES = 128 * 1024;

   private WaypointCodec() {
   }

   public static String encode(List<Waypoints.Waypoint> list) {
      JsonArray arr = new JsonArray();
      for (Waypoints.Waypoint w : list.subList(0, Math.min(list.size(), MAX_WAYPOINTS))) {
         JsonObject o = new JsonObject();
         o.addProperty("n", w.name());
         o.addProperty("x", w.x());
         o.addProperty("y", w.y());
         o.addProperty("z", w.z());
         o.addProperty("d", w.dimension());
         o.addProperty("c", w.color());
         arr.add(o);
      }

      try {
         ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         try (GZIPOutputStream gz = new GZIPOutputStream(bytes)) {
            gz.write(arr.toString().getBytes(StandardCharsets.UTF_8));
         }

         return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
      } catch (IOException e) {
         throw new IllegalStateException(e);
      }
   }

   /** @throws IllegalArgumentException if {@code code} is not a readable waypoint code */
   public static List<Waypoints.Waypoint> decode(String code) {
      String c = code == null ? "" : code.trim();
      if (!c.startsWith(PREFIX)) {
         throw new IllegalArgumentException("not a waypoint code");
      }

      try {
         byte[] gz = Base64.getUrlDecoder().decode(c.substring(PREFIX.length()));
         byte[] json;
         try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(gz))) {
            json = in.readNBytes(MAX_BYTES + 1);
         }

         if (json.length > MAX_BYTES) {
            throw new IllegalArgumentException("code too big");
         }

         List<Waypoints.Waypoint> out = new ArrayList<>();
         for (JsonElement e : JsonParser.parseString(new String(json, StandardCharsets.UTF_8)).getAsJsonArray()) {
            if (out.size() >= MAX_WAYPOINTS) {
               break;
            }

            JsonObject o = e.getAsJsonObject();
            String name = o.get("n").getAsString().trim();
            if (name.isEmpty() || name.length() > 48) {
               continue;
            }

            out.add(new Waypoints.Waypoint(name, o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt(), o.get("d").getAsString(), o.get("c").getAsInt()));
         }

         return out;
      } catch (IllegalArgumentException e) {
         throw e;
      } catch (Exception e) {
         throw new IllegalArgumentException("unreadable waypoint code", e);
      }
   }
}
