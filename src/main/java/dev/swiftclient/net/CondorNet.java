package dev.swiftclient.net;

import dev.swiftclient.core.partner.PartnerHandshake;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public final class CondorNet {
   private CondorNet() {
   }

   private static String version(String modId) {
      return FabricLoader.getInstance().getModContainer(modId).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
   }

   public static void init() {
      PayloadTypeRegistry.clientboundPlay().register(CondorNet.Payload.TYPE, CondorNet.Payload.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(CondorNet.Payload.TYPE, CondorNet.Payload.CODEC);
      ClientPlayNetworking.registerGlobalReceiver(CondorNet.Payload.TYPE, (payload, context) -> PartnerHandshake.onPayload(payload.data()));
      ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> {
         PartnerHandshake.reset();
         if (ClientPlayNetworking.canSend(CondorNet.Payload.TYPE)) {
            ClientPlayNetworking.send(new CondorNet.Payload(PartnerHandshake.capabilities(version("swiftclient"), version("minecraft"))));
         }
      });
   }

   public record Payload(byte[] data) implements CustomPacketPayload {
      public static final Type<CondorNet.Payload> TYPE = new Type(Identifier.fromNamespaceAndPath("condor", "v1"));
      public static final StreamCodec<FriendlyByteBuf, CondorNet.Payload> CODEC = StreamCodec.of((buf, value) -> buf.writeBytes(value.data()), buf -> {
         byte[] bytes = new byte[buf.readableBytes()];
         buf.readBytes(bytes);
         return new CondorNet.Payload(bytes);
      });

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
