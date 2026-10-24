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
   //? if >=26.3 {
   /*@WrapOperation(method = "renderItemInHand", at = @At(value = "INVOKE",
      target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/renderpearl/api/commands/RenderPass;"))
   private com.mojang.renderpearl.api.commands.RenderPass swiftclient$captureHand(
      com.mojang.renderpearl.api.commands.CommandEncoder encoder, java.util.function.Supplier<String> label,
      com.mojang.renderpearl.api.textures.GpuTextureView color, java.util.Optional<org.joml.Vector4f> clear,
      com.mojang.renderpearl.api.textures.GpuTextureView depth, java.util.OptionalDouble clearDepth,
      Operation<com.mojang.renderpearl.api.commands.RenderPass> original) {
      if (HandAuraModule.active != null && HandAuraModule.beginCapture()) {
         var target = HandAuraModule.captureTarget();
         return original.call(encoder, label, target.getColorTextureView(), clear, target.getDepthTextureView(), clearDepth);
      }
      return original.call(encoder, label, color, clear, depth, clearDepth);
   }

   @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "renderItemInHand")
   private void swiftclient$composeHand(net.minecraft.client.renderer.state.level.CameraRenderState camera,
      net.minecraft.client.renderer.state.level.PlayerRenderState player,
      com.mojang.renderpearl.api.textures.GpuTextureView depth, Operation<Void> original) {
      boolean drawn = false;
      try { original.call(camera, player, depth); drawn = true; }
      finally { HandAuraModule.endCapture(drawn); }
   }
   *///?} else {
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
   //?}
}
