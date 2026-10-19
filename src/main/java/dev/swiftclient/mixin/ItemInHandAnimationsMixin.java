package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.swiftclient.visuals.HandModelModule;
import dev.swiftclient.visuals.HandsModule;
import dev.swiftclient.visuals.SwingAnimationModule;
import dev.swiftclient.visuals.WeaponInspectModule;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First-person hands: Swing Animation (swing motion), Hand Pose (position / rotation / scale), CS Knife
 * (knife models and inspect) and 3D Hand (your arm holding the item, stretched up to the shoulder).
 */
@Mixin({ItemInHandRenderer.class})
public abstract class ItemInHandAnimationsMixin {
   @Inject(
      method = {"submitHandsWithItems"},
      at = {@At("HEAD")}
   )
   private void swiftclient$inspectHands(float frameInterp, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int light, CallbackInfo ci) {
      if (WeaponInspectModule.active != null) {
         WeaponInspectModule.handsBegin(poseStack);
      }
   }

   @Inject(
      method = {"submitArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = At.Shift.AFTER
      )}
   )
   private void swiftclient$handPose(
      AbstractClientPlayer player,
      float frameInterp,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack stack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo ci
   ) {
      if (HandsModule.live != null) {
         boolean mainHand = hand == InteractionHand.MAIN_HAND;
         HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
         HandsModule.shape(poseStack, mainHand, arm == HumanoidArm.RIGHT ? 1 : -1);
      }
   }

   @WrapOperation(
      method = {"submitArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"
      )}
   )
   private void swiftclient$swingStyle(
      ItemInHandRenderer renderer, float attack, PoseStack poseStack, int side, HumanoidArm arm, Operation<Void> original, @Local(argsOnly = true) InteractionHand hand
   ) {
      SwingAnimationModule module = SwingAnimationModule.active;
      if (module != null && module.covers(hand == InteractionHand.MAIN_HAND)) {
         module.pose(poseStack, attack, side);
      } else {
         original.call(renderer, attack, poseStack, side, arm);
      }
   }

   @WrapOperation(
      method = {"submitArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
      )}
   )
   private void swiftclient$inspectItem(
      ItemInHandRenderer renderer,
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext context,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      Operation<Void> original,
      @Local(argsOnly = true) InteractionHand hand
   ) {
      boolean mainHand = hand == InteractionHand.MAIN_HAND;
      boolean nudged = HandModelModule.nudge(poseStack, stack, context, mainHand);
      if (WeaponInspectModule.active != null) {
         if (!WeaponInspectModule.renderItem(poseStack, entity, stack, context, collector, light, mainHand)) {
            original.call(renderer, entity, stack, context, poseStack, collector, light);
         }
      } else {
         if (HandModelModule.active != null) {
            HandModelModule.renderArm(poseStack, entity, stack, context, collector, light, mainHand);
         }

         original.call(renderer, entity, stack, context, poseStack, collector, light);
      }

      if (nudged) {
         poseStack.popPose();
      }
   }

   @WrapOperation(
      method = {"renderPlayerArm", "renderMapHand"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"
      )}
   )
   private void swiftclient$longRightArm(
      AvatarRenderer<?> renderer, PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
      original.call(renderer, pose, collector, light, skin, sleeve);
      if (HandModelModule.active != null) {
         pose.pushPose();
         HandModelModule.shoulder(pose, true, renderer);
         original.call(renderer, pose, collector, light, skin, sleeve);
         pose.popPose();
      }
   }

   @WrapOperation(
      method = {"renderPlayerArm", "renderMapHand"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"
      )}
   )
   private void swiftclient$longLeftArm(
      AvatarRenderer<?> renderer, PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
      original.call(renderer, pose, collector, light, skin, sleeve);
      if (HandModelModule.active != null) {
         pose.pushPose();
         HandModelModule.shoulder(pose, false, renderer);
         original.call(renderer, pose, collector, light, skin, sleeve);
         pose.popPose();
      }
   }
}
