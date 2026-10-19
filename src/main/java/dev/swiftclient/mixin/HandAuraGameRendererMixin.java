package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.swiftclient.visuals.HandAuraModule;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Hand Aura: the first-person hands are drawn into their own target, then composed with the glow. */
@Mixin({GameRenderer.class})
public abstract class HandAuraGameRendererMixin {
   @WrapOperation(
      method = {"renderItemInHand"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V"
      )}
   )
   private void swiftclient$handAura(FeatureRenderDispatcher features, SubmitNodeStorage storage, Operation<Void> original) {
      if (HandAuraModule.active != null && HandAuraModule.beginCapture()) {
         boolean drawn = false;
         try {
            original.call(features, storage);
            drawn = true;
         } finally {
            HandAuraModule.endCapture(drawn);
         }
      } else {
         original.call(features, storage);
      }
   }
}
