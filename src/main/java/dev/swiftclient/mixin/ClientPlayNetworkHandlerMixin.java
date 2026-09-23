package dev.swiftclient.mixin;

import dev.swiftclient.ui.RegistryCache;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPacketListener.class})
public abstract class ClientPlayNetworkHandlerMixin {
   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void lightclient$cacheRegistries(CallbackInfo ci) {
      try {
         ClientPacketListener self = (ClientPacketListener)(Object)this;
         RegistryCache.set(self.registryAccess(), self);
      } catch (Throwable var3) {
      }
   }
}
