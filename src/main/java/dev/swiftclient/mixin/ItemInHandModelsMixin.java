package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.swiftclient.visuals.HandModelModule;
import dev.swiftclient.visuals.WeaponInspectModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
//? if >=26.3 {
/*import net.minecraft.client.renderer.item.ItemStackRenderState;
*///?} else
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First-person CS Knife (knife models and inspect) and 3D Hand (your arm holding the item, stretched up to the
 * shoulder): they draw through the 1.21.9+ entity rendering (feature "submit").
 */
@Mixin({ItemInHandRenderer.class})
public abstract class ItemInHandModelsMixin {
   private static int swiftclient$modelSide(InteractionHand hand) {
      HumanoidArm main = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getMainArm() : HumanoidArm.RIGHT;
      HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? main : main.getOpposite();
      return arm == HumanoidArm.RIGHT ? 1 : -1;
   }

   @Inject(
      method = {"submitHandsWithItems", "renderHandsWithItems"},
      at = {@At("HEAD")}
   )
   private void swiftclient$inspectHands(CallbackInfo ci, @Local(argsOnly = true) PoseStack poseStack) {
      if (WeaponInspectModule.active != null) {
         WeaponInspectModule.handsBegin(poseStack);
      }
   }

   //? if >=26.3 {
   /*/^* 26.3 resolves the held item earlier: its render state is submitted here. ^/
   @WrapOperation(
      method = {"submitArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
      )}
   )
   private void swiftclient$inspectItem(
      ItemStackRenderState state,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      int overlay,
      int outline,
      Operation<Void> original,
      @Local(argsOnly = true) InteractionHand hand,
      @Local(argsOnly = true) ItemStack stack
   ) {
      boolean mainHand = hand == InteractionHand.MAIN_HAND;
      ItemDisplayContext context = swiftclient$modelSide(hand) > 0 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
      var holder = Minecraft.getInstance().player;
      boolean nudged = HandModelModule.nudge(poseStack, stack, context, mainHand);
      if (WeaponInspectModule.active != null && holder != null) {
         if (!WeaponInspectModule.renderItem(poseStack, holder, stack, context, collector, light, mainHand)) {
            original.call(state, poseStack, collector, light, overlay, outline);
         }
      } else {
         if (HandModelModule.active != null && holder != null) {
            HandModelModule.renderArm(poseStack, holder, stack, context, collector, light, mainHand);
         }

         original.call(state, poseStack, collector, light, overlay, outline);
      }

      if (nudged) {
         poseStack.popPose();
      }
   }
   *///?} else {
   @WrapOperation(
      method = {"submitArmWithItem", "renderArmWithItem"},
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
   //?}

   @WrapOperation(
      method = {"renderPlayerArm", "renderMapHand", "renderPlayerHand"},
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
      method = {"renderPlayerArm", "renderMapHand", "renderPlayerHand"},
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
