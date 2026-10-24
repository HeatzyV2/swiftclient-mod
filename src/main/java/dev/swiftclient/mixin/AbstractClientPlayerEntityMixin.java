package dev.swiftclient.mixin;

//? if skins
import dev.swiftclient.modules.SkinChangeModule;
import dev.swiftclient.skin.OfflineSkin;
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
   private void swiftclient$safePlayerListEntry(CallbackInfoReturnable<PlayerInfo> cir) {
      if (Minecraft.getInstance().getConnection() == null) {
         cir.setReturnValue(null);
      }
   }

   @Inject(
      method = {"getSkin"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$realSkinTextures(CallbackInfoReturnable<PlayerSkin> cir) {
      if (Minecraft.getInstance().getConnection() == null) {
         Object cached = SkinRenderer.getCachedSkinTexturesObj();
         if (cached != null) {
            cir.setReturnValue((PlayerSkin)cached);
         }
      }
   }

   //? if skins {
   /** Skin Change: the skin chosen in the module, for you and/or the other players. */
   @Inject(
      method = {"getSkin"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void swiftclient$skinChange(CallbackInfoReturnable<PlayerSkin> cir) {
      AbstractClientPlayer self = (AbstractClientPlayer)(Object)this;
      PlayerSkin original = cir.getReturnValue();
      PlayerSkin base = OfflineSkin.configured() && OfflineSkin.isSelf(self.getUUID()) ? OfflineSkin.apply(original) : original;
      PlayerSkin swapped = SkinChangeModule.swap(self, base);
      if (swapped != original) {
         cir.setReturnValue(swapped);
      }
   }
   //?}
}
