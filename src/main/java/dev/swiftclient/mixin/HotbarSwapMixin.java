package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.swiftclient.visuals.HotbarSwapModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hotbar Swap: extra motion while the held item is being raised after a switch. */
@Mixin({ItemInHandRenderer.class})
public abstract class HotbarSwapMixin {
   @Inject(
      method = {"submitArmWithItem", "renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = At.Shift.AFTER,
         ordinal = 0
      )}
   )
   private void swiftclient$swap(
      CallbackInfo ci,
      @Local(argsOnly = true) InteractionHand hand,
      @Local(argsOnly = true) ItemStack stack,
      // Every version: (..., float partialTick, float xRot, hand, float attack, stack, float inverseArmHeight, ...)
      @Local(argsOnly = true, ordinal = 3) float inverseArmHeight,
      @Local(argsOnly = true) PoseStack poseStack
   ) {
      if (HotbarSwapModule.active != null && !stack.isEmpty() && Minecraft.getInstance().player != null) {
         HumanoidArm main = Minecraft.getInstance().player.getMainArm();
         HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? main : main.getOpposite();
         HotbarSwapModule.apply(poseStack, inverseArmHeight, arm == HumanoidArm.RIGHT ? 1 : -1);
      }
   }
}
