package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.swiftclient.modules.FireModule;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fire module: hides, lowers or fades the first-person fire overlay (submitFire since 26.2, renderFire before). */
@Mixin({ScreenEffectRenderer.class})
public abstract class ScreenEffectRendererMixin {
   /** The vanilla fire colour: white at 90% opacity. */
   private static final int SWIFTCLIENT$FIRE = -436207617;

   @Inject(
      method = {"submitFire", "renderFire"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void swiftclient$fireStart(CallbackInfo ci, @Local(argsOnly = true) PoseStack poseStack) {
      if (FireModule.hidden()) {
         ci.cancel();
      } else {
         poseStack.pushPose();
         poseStack.translate(0.0F, -FireModule.offset(), 0.0F);
      }
   }

   @Inject(
      method = {"submitFire", "renderFire"},
      at = {@At("RETURN")}
   )
   private static void swiftclient$fireEnd(CallbackInfo ci, @Local(argsOnly = true) PoseStack poseStack) {
      poseStack.popPose();
   }

   //? if >=26.2 {
   @ModifyConstant(
      method = {"buildFireQuad"},
      constant = {@Constant(intValue = SWIFTCLIENT$FIRE)}
   )
   private static int swiftclient$fireOpacity(int color) {
      return FireModule.tint(color);
   }
   //?} else {
   /*@ModifyConstant(
      method = {"renderFire"},
      constant = {@Constant(floatValue = 0.9F)}
   )
   private static float swiftclient$fireOpacity(float alpha) {
      return (FireModule.tint(SWIFTCLIENT$FIRE) >>> 24) / 255.0F;
   }
   *///?}
}
