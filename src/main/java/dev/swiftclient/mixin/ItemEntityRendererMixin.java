package dev.swiftclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.modules.ItemPhysicsModule;
import dev.swiftclient.modules.Items2dModule;
import dev.swiftclient.modules.UhcOverlayModule;
import dev.swiftclient.render.DroppedItemState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dropped items: Item Physics (they fall flat and stay still), 2D Items (they always face you) and UHC
 * Overlay (golden apples, heads, gold... drawn bigger). Vanilla rendering is kept when all three are off.
 */
@Mixin({ItemEntityRenderer.class})
public abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity, ItemEntityRenderState> {
   @Shadow
   @Final
   private RandomSource random;

   protected ItemEntityRendererMixin(EntityRendererProvider.Context context) {
      super(context);
   }

   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V"},
      at = {@At("TAIL")}
   )
   private void swiftclient$extractExtras(ItemEntity entity, ItemEntityRenderState state, float partialTicks, CallbackInfo ci) {
      DroppedItemState extra = (DroppedItemState)state;
      extra.swiftclient$setOnGround(entity.onGround());
      extra.swiftclient$setScale(UhcOverlayModule.scaleFor(entity.getItem()));
   }

   @Inject(
      method = {"submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$customItem(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
      if (!ModuleManager.isLoaded() || state.item.isEmpty()) {
         return;
      }

      boolean physics = ModuleManager.get(ItemPhysicsModule.class).isEnabled();
      boolean flat2d = ModuleManager.get(Items2dModule.class).isEnabled();
      DroppedItemState extra = (DroppedItemState)state;
      float scale = extra.swiftclient$scale();
      if (!physics && !flat2d && scale == 1.0F) {
         return;
      }

      poseStack.pushPose();
      AABB box = state.item.getModelBoundingBox();
      float minOffsetY = -((float)box.minY) + 0.0625F;
      boolean flatSprite = box.getZsize() <= 0.0625;
      if (flat2d) {
         float bob = physics ? 0.0F : Mth.sin(state.ageInTicks / 10.0F + state.bobOffset) * 0.1F + 0.1F;
         poseStack.translate(0.0F, bob + minOffsetY * scale, 0.0F);
         poseStack.mulPose(camera.orientation);
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         poseStack.scale(scale, scale, flatSprite ? scale : scale * 0.1F);
      } else if (physics) {
         poseStack.mulPose(Axis.YP.rotation(state.bobOffset));
         if (extra.swiftclient$onGround()) {
            if (flatSprite) {
               poseStack.translate(0.0F, 0.035F * scale, 0.0F);
               poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            } else {
               poseStack.translate(0.0F, (float)-box.minY * scale, 0.0F);
            }
         } else {
            // Tumbling while it falls
            poseStack.translate(0.0F, minOffsetY * scale, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(state.ageInTicks * 18.0F % 360.0F));
         }

         poseStack.scale(scale, scale, scale);
      } else {
         float bob = Mth.sin(state.ageInTicks / 10.0F + state.bobOffset) * 0.1F + 0.1F;
         poseStack.translate(0.0F, bob + minOffsetY * scale, 0.0F);
         poseStack.mulPose(Axis.YP.rotation(ItemEntity.getSpin(state.ageInTicks, state.bobOffset)));
         poseStack.scale(scale, scale, scale);
      }

      ItemEntityRenderer.submitMultipleFromCount(poseStack, collector, state.lightCoords, state, this.random, box);
      poseStack.popPose();
      super.submit(state, poseStack, collector, camera);
      ci.cancel();
   }
}
