package dev.swiftclient.mixin;

import dev.swiftclient.ui.RegistryCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryAccess.Frozen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ClientLevel.class})
public abstract class ClientWorldNullSafeMixin {
   @Redirect(
      method = {"<init>"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;registryAccess()Lnet/minecraft/core/RegistryAccess$Frozen;"
      ),
      require = 0
   )
   private static Frozen lightclient$nullSafeRegistries(ClientPacketListener self) {
      if (self != null) {
         return self.registryAccess();
      } else {
         Frozen cached = RegistryCache.get();
         return cached != null ? cached : RegistryAccess.EMPTY;
      }
   }
}
