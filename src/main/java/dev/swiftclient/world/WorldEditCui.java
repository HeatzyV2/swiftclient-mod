package dev.swiftclient.world;

import dev.swiftclient.core.log.Log;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * WorldEdit CUI protocol (channel {@code worldedit:cui}): the server sends the current selection as text
 * events ("s|cuboid", "p|0|x|y|z|volume", "p2|...", "mm|min|max") once the client announced itself with
 * "v|4". Only the selection shape is kept; WorldEditCuiModule draws it.
 */
public final class WorldEditCui {
   public static final int PROTOCOL = 4;
   private static volatile boolean enabled;
   private static volatile String shape = "";
   private static final BlockPos[] CUBOID = new BlockPos[2];
   private static final List<int[]> POLY = new ArrayList<>();
   private static int polyMin;
   private static int polyMax;

   private WorldEditCui() {
   }

   public record CuiPayload(String text) implements CustomPacketPayload {
      public static final CustomPacketPayload.Type<CuiPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("worldedit", "cui"));
      public static final StreamCodec<ByteBuf, CuiPayload> CODEC = StreamCodec.of(
         (buf, p) -> buf.writeBytes(p.text().getBytes(StandardCharsets.UTF_8)),
         buf -> {
            byte[] b = new byte[buf.readableBytes()];
            buf.readBytes(b);
            return new CuiPayload(new String(b, StandardCharsets.UTF_8));
         }
      );

      @Override
      public CustomPacketPayload.Type<CuiPayload> type() {
         return TYPE;
      }
   }

   /** Registers the channel both ways. Called once at startup. */
   public static void register() {
      try {
         PayloadTypeRegistry.clientboundPlay().register(CuiPayload.TYPE, CuiPayload.CODEC);
         PayloadTypeRegistry.serverboundPlay().register(CuiPayload.TYPE, CuiPayload.CODEC);
         ClientPlayNetworking.registerGlobalReceiver(CuiPayload.TYPE, (payload, context) -> context.client().execute(() -> handle(payload.text())));
      } catch (RuntimeException e) {
         // Another mod (WorldEdit CUI itself) already owns the channel: let it do the job.
         Log.get("WECUI").info("Canal worldedit:cui deja enregistre, WorldEdit CUI de Swift desactive");
      }
   }

   public static void setEnabled(boolean on) {
      enabled = on;
      if (on) {
         handshake();
      } else {
         clear();
      }
   }

   /** Tells the server we understand CUI events; it then resends the current selection. */
   public static void handshake() {
      if (enabled) {
         try {
            if (ClientPlayNetworking.canSend(CuiPayload.TYPE)) {
               ClientPlayNetworking.send(new CuiPayload("v|" + PROTOCOL));
            }
         } catch (RuntimeException ignored) {
         }
      }
   }

   public static synchronized void clear() {
      shape = "";
      CUBOID[0] = null;
      CUBOID[1] = null;
      POLY.clear();
   }

   static synchronized void handle(String msg) {
      if (!enabled || msg == null) {
         return;
      }

      String[] p = msg.split("\\|");
      try {
         switch (p[0]) {
            case "s" -> {
               clear();
               shape = p.length > 1 ? p[1] : "";
            }
            case "p" -> {
               int i = Integer.parseInt(p[1]);
               if (i >= 0 && i < 2) {
                  CUBOID[i] = new BlockPos(Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]));
               }
            }
            case "p2" -> {
               int i = Integer.parseInt(p[1]);
               int[] pt = {Integer.parseInt(p[2]), Integer.parseInt(p[3])};
               while (POLY.size() <= i) {
                  POLY.add(pt);
               }

               POLY.set(i, pt);
            }
            case "mm" -> {
               polyMin = Integer.parseInt(p[1]);
               polyMax = Integer.parseInt(p[2]);
            }
            default -> {
            }
         }
      } catch (RuntimeException ignored) {
         // Unknown or malformed event: keep the last good selection.
      }
   }

   public static synchronized String shape() {
      return shape;
   }

   public static synchronized BlockPos[] cuboid() {
      return new BlockPos[]{CUBOID[0], CUBOID[1]};
   }

   public static synchronized List<int[]> polygon() {
      return List.copyOf(POLY);
   }

   public static synchronized int[] polygonHeight() {
      return new int[]{polyMin, polyMax};
   }
}
