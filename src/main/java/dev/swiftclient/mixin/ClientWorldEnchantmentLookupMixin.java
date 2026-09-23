package dev.swiftclient.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientLevel.class})
public abstract class ClientWorldEnchantmentLookupMixin {
   private static Scoreboard LIGHTCLIENT_FAKE_SCOREBOARD;

   @Inject(
      method = {"getScoreboard"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void lightclient$nullSafeScoreboard(CallbackInfoReturnable<Scoreboard> cir) {
      ClientLevel self = (ClientLevel)(Object)this;
      if (((ClientWorldAccessor)self).lightclient$getNetworkHandler() == null) {
         if (LIGHTCLIENT_FAKE_SCOREBOARD == null) {
            LIGHTCLIENT_FAKE_SCOREBOARD = new Scoreboard();
         }

         cir.setReturnValue(LIGHTCLIENT_FAKE_SCOREBOARD);
      }
   }
}
