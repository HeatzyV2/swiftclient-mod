package dev.swiftclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.swiftclient.modules.FireModule;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fire module: hides, lowers or fades the first-person fire overlay. */
@Mixin({ScreenEffectRenderer.class})
public abstract class ScreenEffectRendererMixin {
   @Inject(
      method = {"submitFire"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void swiftclient$fireStart(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
      if (FireModule.hidden()) {
         ci.cancel();
      } else {
         poseStack.pushPose();
         poseStack.translate(0.0F, -FireModule.offset(), 0.0F);
      }
   }

   @Inject(
      method = {"submitFire"},
      at = {@At("RETURN")}
   )
   private static void swiftclient$fireEnd(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
      poseStack.popPose();
   }

   @ModifyConstant(
      method = {"buildFireQuad"},
      constant = {@Constant(intValue = -436207617)}
   )
   private static int swiftclient$fireOpacity(int color) {
      return FireModule.tint(color);
   }
}
