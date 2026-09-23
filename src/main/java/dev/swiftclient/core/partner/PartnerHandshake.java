package dev.swiftclient.core.partner;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.swiftclient.core.cosmetics.CosmeticHttp;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public final class PartnerHandshake {
   public static final String CHANNEL_MODERN = "condor:v1";
   public static final String CHANNEL_LEGACY = "CDR|V1";
   private static final Pattern NONCE = Pattern.compile("[a-f0-9]{32,128}", 2);
   private static final Pattern SERVER = Pattern.compile("[a-z0-9_-]{1,32}", 2);
   private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "swiftclient-condor");
      t.setDaemon(true);
      return t;
   });
   private static volatile String lastNonce = "";

   private PartnerHandshake() {
   }

   public static void onPayload(byte[] data) {
      if (data != null && data.length != 0 && data.length <= 8192) {
         String nonce;
         String server;
         try {
            JsonObject msg = new JsonParser().parse(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!"hello".equals(str(msg, "m"))) {
               return;
            }

            nonce = str(msg, "nonce");
            server = str(msg, "server");
         } catch (Exception var4) {
            return;
         }

         if (!nonce.isEmpty() && NONCE.matcher(nonce).matches()) {
            if (!server.isEmpty() && SERVER.matcher(server).matches()) {
               if (!nonce.equals(lastNonce)) {
                  lastNonce = nonce;
                  IO.execute(() -> {
                     JsonObject body = new JsonObject();
                     body.addProperty("nonce", nonce);
                     body.addProperty("server", server);
                     CosmeticHttp.postAuthed("/api/partner/attest", body);
                  });
               }
            }
         }
      }
   }

   public static byte[] capabilities(String clientVersion, String mcVersion) {
      JsonObject msg = new JsonObject();
      msg.addProperty("v", 1);
      msg.addProperty("m", "capabilities");
      msg.addProperty("client", clientVersion == null ? "" : clientVersion);
      msg.addProperty("mc", mcVersion == null ? "" : mcVersion);
      JsonArray modules = new JsonArray();
      modules.add(new JsonPrimitive("identity"));
      msg.add("modules", modules);
      return msg.toString().getBytes(StandardCharsets.UTF_8);
   }

   public static void reset() {
      lastNonce = "";
   }

   private static String str(JsonObject o, String key) {
      return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
   }
}
