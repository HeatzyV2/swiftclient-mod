package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.swiftclient.visuals.CustomGlintModule;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Custom Glint: pattern size and speed of the enchantment glint. */
@Mixin({TextureTransform.class})
public abstract class GlintTextureTransformMixin {
   @ModifyVariable(
      method = {"setupGlintTexturing"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private static float swiftclient$glintPatternScale(float scale) {
      CustomGlintModule module = CustomGlintModule.active;
      return module == null ? scale : scale * module.uvScaleMultiplier();
   }

   @ModifyExpressionValue(
      method = {"setupGlintTexturing"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Util;getMillis()J"
      )}
   )
   private static long swiftclient$glintClock(long millis) {
      CustomGlintModule module = CustomGlintModule.active;
      return module == null ? millis : module.glintMillis(millis);
   }
}
