package dev.swiftclient.mixin;

import dev.swiftclient.ui.SkinRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayer.class})
public class AbstractClientPlayerEntityMixin {
   @Inject(
      method = {"getPlayerInfo"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void lightclient$safePlayerListEntry(CallbackInfoReturnable<PlayerInfo> cir) {
      if (Minecraft.getInstance().getConnection() == null) {
         cir.setReturnValue(null);
      }
   }

   @Inject(
      method = {"getSkin"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void lightclient$realSkinTextures(CallbackInfoReturnable<PlayerSkin> cir) {
      if (Minecraft.getInstance().getConnection() == null) {
         Object cached = SkinRenderer.getCachedSkinTexturesObj();
         if (cached != null) {
            cir.setReturnValue((PlayerSkin)cached);
         }
      }
   }
}
