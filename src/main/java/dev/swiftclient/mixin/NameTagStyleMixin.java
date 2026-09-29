package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.swiftclient.modules.NameTagsModule;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.state.OptionsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** NameTags: size and background opacity of the tags above entities. */
@Mixin({SubmitNodeCollection.class})
public abstract class NameTagStyleMixin {
   @ModifyConstant(
      method = {"submitNameTag"},
      constant = {@Constant(floatValue = 0.025F), @Constant(floatValue = -0.025F)}
   )
   private float swiftclient$tagScale(float original) {
      return original * NameTagsModule.scale();
   }

   @WrapOperation(
      method = {"submitNameTag"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/state/OptionsRenderState;getBackgroundOpacity(F)F"
      )
   )
   private float swiftclient$tagBackground(OptionsRenderState options, float def, Operation<Float> original) {
      float custom = NameTagsModule.background();
      return custom >= 0.0F ? custom : original.call(options, def);
   }
}
