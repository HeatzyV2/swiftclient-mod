package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.swiftclient.modules.NameTagsModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * NameTags: size and background opacity of the tags above entities. The tag is laid out in
 * SubmitNodeCollection.submitNameTag (26.2+), NameTagFeatureRenderer.Storage.add (1.21.9 - 26.1) or
 * EntityRenderer.renderNameTag (before).
 */
//? if >=26.2 {
@Mixin({net.minecraft.client.renderer.SubmitNodeCollection.class})
//?} else if >=1.21.9 {
/*@Mixin({net.minecraft.client.renderer.feature.NameTagFeatureRenderer.Storage.class})
*///?} else
/*@Mixin({net.minecraft.client.renderer.entity.EntityRenderer.class})*/
public abstract class NameTagStyleMixin {
   @ModifyConstant(
      method = {"submitNameTag", "add", "renderNameTag"},
      constant = {@Constant(floatValue = 0.025F), @Constant(floatValue = -0.025F)}
   )
   private float swiftclient$tagScale(float original) {
      return original * NameTagsModule.scale();
   }

   //? if >=26.1 {
   @WrapOperation(
      method = {"submitNameTag", "add"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/state/OptionsRenderState;getBackgroundOpacity(F)F"
      )
   )
   private float swiftclient$tagBackground(net.minecraft.client.renderer.state.OptionsRenderState options, float def, Operation<Float> original) {
      float custom = NameTagsModule.background();
      return custom >= 0.0F ? custom : original.call(options, def);
   }
   //?} else {
   /*@WrapOperation(
      method = {"add", "renderNameTag"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Options;getBackgroundOpacity(F)F"
      )
   )
   private float swiftclient$tagBackground(net.minecraft.client.Options options, float def, Operation<Float> original) {
      float custom = NameTagsModule.background();
      return custom >= 0.0F ? custom : original.call(options, def);
   }
   *///?}
}
