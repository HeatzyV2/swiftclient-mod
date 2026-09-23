package dev.swiftclient.mixin;

import dev.swiftclient.ui.WorldCache;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientLevel.class})
public abstract class ClientWorldCacheMixin {
   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void lightclient$cache(CallbackInfo ci) {
      WorldCache.set((ClientLevel)(Object)this);
   }
}
