package dev.swiftclient.core.rpc;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class DiscordIpc {
   private static final int OP_HANDSHAKE = 0;
   private static final int OP_FRAME = 1;
   private final String clientId;
   private RandomAccessFile pipe;

   public DiscordIpc(String clientId) {
      this.clientId = clientId;
   }

   public boolean isConnected() {
      return this.pipe != null;
   }

   public synchronized boolean connect() {
      if (!isWindows()) {
         return false;
      } else {
         for (int i = 0; i < 10; i++) {
            try {
               this.pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
               this.handshake();
               return true;
            } catch (IOException ignored) {
               this.close();
            }
         }

         return false;
      }
   }

   private void handshake() throws IOException {
      JsonObject hs = new JsonObject();
      hs.addProperty("v", 1);
      hs.addProperty("client_id", this.clientId);
      this.write(0, hs.toString());
      this.readFrame();
   }

   public synchronized void setActivity(String details, String state, String largeImageKey, String largeImageText, Long startTimestamp) {
      if (this.pipe != null) {
         try {
            JsonObject activity = new JsonObject();
            if (details != null) {
               activity.addProperty("details", details);
            }

            if (state != null) {
               activity.addProperty("state", state);
            }

            if (startTimestamp != null) {
               JsonObject ts = new JsonObject();
               ts.addProperty("start", startTimestamp);
               activity.add("timestamps", ts);
            }

            if (largeImageKey != null) {
               JsonObject assets = new JsonObject();
               assets.addProperty("large_image", largeImageKey);
               if (largeImageText != null) {
                  assets.addProperty("large_text", largeImageText);
               }

               activity.add("assets", assets);
            }

            JsonObject args = new JsonObject();
            args.addProperty("pid", (int)ProcessHandle.current().pid());
            args.add("activity", activity);
            JsonObject cmd = new JsonObject();
            cmd.addProperty("cmd", "SET_ACTIVITY");
            cmd.add("args", args);
            cmd.addProperty("nonce", UUID.randomUUID().toString());
            this.write(1, cmd.toString());
            this.readFrame();
         } catch (IOException ignored) {
            this.close();
         }
      }
   }

   /** SET_ACTIVITY without an activity removes the presence from the Discord profile. */
   public synchronized void clearActivity() {
      if (this.pipe != null) {
         try {
            JsonObject args = new JsonObject();
            args.addProperty("pid", (int)ProcessHandle.current().pid());
            JsonObject cmd = new JsonObject();
            cmd.addProperty("cmd", "SET_ACTIVITY");
            cmd.add("args", args);
            cmd.addProperty("nonce", UUID.randomUUID().toString());
            this.write(1, cmd.toString());
            this.readFrame();
         } catch (IOException ignored) {
            this.close();
         }
      }
   }

   public synchronized void close() {
      if (this.pipe != null) {
         try {
            this.pipe.close();
         } catch (IOException ignored) {
         }

         this.pipe = null;
      }
   }

   private void write(int op, String json) throws IOException {
      byte[] data = json.getBytes(StandardCharsets.UTF_8);
      ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
      header.putInt(op);
      header.putInt(data.length);
      this.pipe.write(header.array());
      this.pipe.write(data);
   }

   private void readFrame() throws IOException {
      byte[] hdr = new byte[8];
      this.pipe.readFully(hdr);
      ByteBuffer hb = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN);
      hb.getInt();
      int len = hb.getInt();
      if (len > 0) {
         this.pipe.readFully(new byte[len]);
      }
   }

   private static boolean isWindows() {
      return System.getProperty("os.name", "").toLowerCase().contains("win");
   }
}
