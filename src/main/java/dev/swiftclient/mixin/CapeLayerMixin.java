package dev.swiftclient.mixin;

import dev.swiftclient.core.log.Log;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.swiftclient.core.cosmetics.CosmeticState;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
//?} else
/*import net.minecraft.client.renderer.MultiBufferSource;*/
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CapeLayer.class})
public abstract class CapeLayerMixin {
   @Shadow
   @Final
   private HumanoidModel<AvatarRenderState> model;
   @Unique
   private static final Set<String> swiftclient$logged = ConcurrentHashMap.newKeySet();
   @Unique
   private static final AtomicBoolean swiftclient$firstFire = new AtomicBoolean(false);
   @Unique
   private static final AtomicBoolean swiftclient$firstResolve = new AtomicBoolean(false);

   //? if >=1.21.9 {
   @Inject(
      method = {"submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$customCape(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float f1, float f2, CallbackInfo ci) {
   //?} else {
   /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V", at = @At("HEAD"), cancellable = true)
   private void swiftclient$customCape(PoseStack pose, MultiBufferSource collector, int light, AvatarRenderState state, float f1, float f2, CallbackInfo ci) {
   *///?}
      if (swiftclient$firstFire.compareAndSet(false, true)) {
         Log.get("Cape").debug("Rendu de cape intercepte");
      }

      if (!state.isInvisible && state.showCape) {
         if (state.chestEquipment == null || state.chestEquipment.isEmpty() || !state.chestEquipment.is(Items.ELYTRA)) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
               Entity e = level.getEntity(state.id);
               if (e instanceof AbstractClientPlayer player) {
                  if (!player.isInvisible()) {
                     String capeId = CosmeticState.capeFor(player.getUUID(), player.getGameProfile().name());
                     if (capeId != null) {
                        if (CosmeticState.frameFor(player.getUUID(), capeId) instanceof Identifier frame) {
                           if (swiftclient$logged.add(player.getUUID() + ":" + capeId)) {
                              Log.get("Cape").debug("Rendu de la cape {} pour {}", capeId, player.getName().getString());
                           }

                           pose.pushPose();
                           //? if >=1.21.9 {
                           collector.submitModel(
                              //? if >=26.3 {
                              /*this.model, state, pose, RenderTypes.entitySolid(frame), light, OverlayTexture.NO_OVERLAY, state.outlineColor
                              *///?} else
                              this.model, state, pose, RenderTypes.entitySolid(frame), light, OverlayTexture.NO_OVERLAY, state.outlineColor, null
                           );
                           //?} else {
                           /*this.model.setupAnim(state);
                           this.model.renderToBuffer(pose, collector.getBuffer(RenderTypes.entitySolid(frame)), light, OverlayTexture.NO_OVERLAY);
                           *///?}

                           pose.popPose();
                           ci.cancel();
                        }
                     }
                  }
               } else {
                  if (swiftclient$firstResolve.compareAndSet(false, true)) {
                     Log.get("Cape").warn("Resolution du joueur impossible pour le rendu de cape : state.id={} entity={}", state.id, e == null ? "null" : e.getClass().getSimpleName());
                  }
               }
            }
         }
      }
   }
}
