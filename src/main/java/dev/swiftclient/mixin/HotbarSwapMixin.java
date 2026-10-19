package dev.swiftclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.swiftclient.visuals.HotbarSwapModule;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
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
      method = {"submitArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = At.Shift.AFTER
      )}
   )
   private void swiftclient$swap(
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
      if (HotbarSwapModule.active != null && !stack.isEmpty()) {
         HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
         HotbarSwapModule.apply(poseStack, inverseArmHeight, arm == HumanoidArm.RIGHT ? 1 : -1);
      }
   }
}
