package dev.swiftclient.mixin;

import dev.swiftclient.core.cosmetics.CosmeticState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({WingsLayer.class})
public class WingsLayerMixin {
   @Inject(
      method = {"getPlayerElytraTexture"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void swiftclient$capeSurElytra(HumanoidRenderState state, CallbackInfoReturnable<Identifier> cir) {
      if (state instanceof AvatarRenderState avatar) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            if (level.getEntity(avatar.id) instanceof AbstractClientPlayer player) {
               if (!player.isInvisible()) {
                  String capeId = CosmeticState.capeFor(player.getUUID(), player.getGameProfile().name());
                  if (capeId != null) {
                     if (!CosmeticState.capeHasElytra(capeId)) {
                        cir.setReturnValue(null);
                     } else {
                        if (CosmeticState.frameFor(player.getUUID(), capeId) instanceof Identifier frame) {
                           cir.setReturnValue(frame);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
