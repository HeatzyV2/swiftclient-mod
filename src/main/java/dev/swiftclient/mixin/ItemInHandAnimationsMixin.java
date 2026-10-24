package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.swiftclient.visuals.HandsModule;
import dev.swiftclient.visuals.SwingAnimationModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First-person hands: Swing Animation (swing motion) and Hand Pose (position / rotation / scale). The knife
 * models and the 3D hand are in ItemInHandModelsMixin. Arguments are taken with {@link Local} by type so the
 * handlers fit every version's signature; the method names list the old ("render...") and new ("submit...")
 * spellings.
 */
@Mixin({ItemInHandRenderer.class})
public abstract class ItemInHandAnimationsMixin {
   private static int swiftclient$side(InteractionHand hand) {
      HumanoidArm main = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getMainArm() : HumanoidArm.RIGHT;
      HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? main : main.getOpposite();
      return arm == HumanoidArm.RIGHT ? 1 : -1;
   }

   @Inject(
      method = {"submitArmWithItem", "renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = At.Shift.AFTER,
         ordinal = 0
      )}
   )
   private void swiftclient$handPose(CallbackInfo ci, @Local(argsOnly = true) InteractionHand hand, @Local(argsOnly = true) PoseStack poseStack) {
      if (HandsModule.live != null) {
         HandsModule.shape(poseStack, hand == InteractionHand.MAIN_HAND, swiftclient$side(hand));
      }
   }

   //? if >=1.21.11 {
   @WrapOperation(
      method = {"submitArmWithItem", "renderArmWithItem"},
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
   //?} else {
   /*@WrapOperation(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm(FFLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"
      )}
   )
   private void swiftclient$swingStyle(
      ItemInHandRenderer renderer, float attack, float equip, PoseStack poseStack, int side, HumanoidArm arm, Operation<Void> original,
      @Local(argsOnly = true) InteractionHand hand
   ) {
      SwingAnimationModule module = SwingAnimationModule.active;
      if (module != null && module.covers(hand == InteractionHand.MAIN_HAND)) {
         module.pose(poseStack, attack, side);
      } else {
         original.call(renderer, attack, equip, poseStack, side, arm);
      }
   }
   *///?}
}
