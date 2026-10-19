package dev.swiftclient.mixin;

import dev.swiftclient.visuals.PharaohHeaddressLayer;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pharaoh Headdress: an extra layer on every player model (drawn only while the module is on). */
@Mixin({AvatarRenderer.class})
public abstract class HeadCosmeticLayerMixin<T extends Avatar & ClientAvatarEntity> extends LivingEntityRenderer<T, AvatarRenderState, PlayerModel> {
   protected HeadCosmeticLayerMixin(EntityRendererProvider.Context context, PlayerModel model, float shadowRadius) {
      super(context, model, shadowRadius);
   }

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void swiftclient$addHeadCosmetics(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
      this.addLayer(new PharaohHeaddressLayer(this));
   }
}
