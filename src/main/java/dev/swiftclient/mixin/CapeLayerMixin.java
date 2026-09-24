package dev.swiftclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.swiftclient.core.cosmetics.CosmeticState;
import dev.swiftclient.core.mods.CapeSim;
import dev.swiftclient.core.mods.CapeSimManager;
import dev.swiftclient.core.mods.RealisticCapeState;
import dev.swiftclient.render.WaveMesh;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
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

   @Inject(
      method = {"submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$customCape(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float f1, float f2, CallbackInfo ci) {
      if (swiftclient$firstFire.compareAndSet(false, true)) {
         System.out.println("[SwiftClient/Cape] mixin CapeLayer ACTIF (le rendu de cape est intercepté)");
      }

      if (!state.isInvisible) {
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
                              System.out.println("[SwiftClient/Cape] rendu de la cape " + capeId + " pour " + player.getName().getString());
                           }

                           pose.pushPose();
                           CapeSim sim = RealisticCapeState.active ? CapeSimManager.get(player.getUUID()) : null;
                           if (sim != null) {
                              ModelPart buste = ((PlayerModel)((CapeLayer)(Object)this).getParentModel()).body;
                              float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
                              collector.submitCustomGeometry(pose, RenderTypes.entitySolid(frame), (p, vc) -> WaveMesh.emit(p, vc, sim, partial, light, buste));
                           } else {
                              collector.submitModel(
                                 this.model, state, pose, RenderTypes.entitySolid(frame), light, OverlayTexture.NO_OVERLAY, state.outlineColor, null
                              );
                           }

                           pose.popPose();
                           ci.cancel();
                        }
                     }
                  }
               } else {
                  if (swiftclient$firstResolve.compareAndSet(false, true)) {
                     System.out
                        .println(
                           "[SwiftClient/Cape] résolution joueur ÉCHOUÉE : state.id="
                              + state.id
                              + " → entity="
                              + (e == null ? "null" : e.getClass().getSimpleName())
                              + " (l'UUID via entity id ne marche pas ici)"
                        );
                  }
               }
            }
         }
      }
   }
}
