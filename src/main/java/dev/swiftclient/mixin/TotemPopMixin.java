package dev.swiftclient.mixin;

import dev.swiftclient.visuals.RelicParticlesModule;
import dev.swiftclient.visuals.TotemSpiritModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Totem pops (entity event 35), on the game thread: Totem Spirit and Relic Particles. */
@Mixin({ClientPacketListener.class})
public abstract class TotemPopMixin {
   @Shadow
   private ClientLevel level;

   @Inject(
      method = {"handleEntityEvent"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
         shift = At.Shift.AFTER
      )}
   )
   private void swiftclient$totemPop(ClientboundEntityEventPacket packet, CallbackInfo ci) {
      if (packet.getEventId() != EntityEvent.PROTECTED_FROM_DEATH || this.level == null) {
         return;
      }

      Entity entity = packet.getEntity(this.level);
      if (entity instanceof LivingEntity living && !living.isRemoved()) {
         TotemSpiritModule.onTotemPop(living);
         RelicParticlesModule.onTotemPop(living);
      }
   }
}
