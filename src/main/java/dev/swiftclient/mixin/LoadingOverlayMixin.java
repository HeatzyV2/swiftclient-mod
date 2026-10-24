package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//? if >=1.21.6 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
//?} else
/*import java.util.function.Function;*/
import dev.swiftclient.ui.SwiftLoadingScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Swift loading screen over resource loads: vanilla keeps its fades and progress, only what it draws changes
 * (background colour, no Mojang logo and bar, the Swift scene on top).
 */
@Mixin({LoadingOverlay.class})
public abstract class LoadingOverlayMixin {
   @Shadow
   private float currentProgress;
   @Shadow
   private long fadeOutStart;
   @Shadow
   private long fadeInStart;
   @Shadow
   @Final
   private boolean fadeIn;

   @Inject(method = {"registerTextures"}, at = {@At("TAIL")})
   private static void swiftclient$registerTextures(TextureManager textures, CallbackInfo ci) {
      SwiftLoadingScreen.registerTextures(textures);
   }

   //? if >=26.2 {
   @ModifyArg(
      method = {"extractRenderState"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"),
      index = 4
   )
   private int swiftclient$background(int argb) {
      return argb & 0xFF000000 | SwiftLoadingScreen.BACKGROUND;
   }

   @ModifyArg(
      method = {"extractRenderState"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;setVector4fFromARGB32(Lorg/joml/Vector4f;I)Lorg/joml/Vector4f;"),
      index = 1
   )
   private int swiftclient$clearColor(int argb) {
      return 0xFF000000 | SwiftLoadingScreen.BACKGROUND;
   }
   //?} else {
   /*/^* Before 26.2 the brand colour is read once per use: fills and the clear colour. ^/
   @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
      method = {"extractRenderState"},
      at = @At(value = "INVOKE", target = "Ljava/util/function/IntSupplier;getAsInt()I")
   )
   private int swiftclient$background(int argb) {
      return 0xFF000000 | SwiftLoadingScreen.BACKGROUND;
   }
   *///?}

   @WrapOperation(
      method = {"extractRenderState"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"
      )
   )
   private void swiftclient$noMojangLogo(
      GuiGraphicsExtractor graphics, /*? if >=1.21.6 {*/RenderPipeline/*?} else {*//*Function<Identifier, ?>*//*?}*/ pipeline, Identifier texture, int x, int y, float u, float v, int w, int h, int srcW, int srcH,
      int texW, int texH, int color, Operation<Void> original
   ) {
   }

   @Inject(method = {"extractProgressBar", "drawProgressBar"}, at = {@At("HEAD")}, cancellable = true)
   private void swiftclient$noBar(CallbackInfo ci) {
      ci.cancel();
   }

   @Inject(method = {"extractRenderState"}, at = {@At("TAIL")})
   private void swiftclient$swiftScene(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, CallbackInfo ci) {
      long now = Util.getMillis();
      float fadeOut = this.fadeOutStart > -1L ? (now - this.fadeOutStart) / 1000.0F : -1.0F;
      float alpha;
      if (fadeOut >= 1.0F) {
         alpha = 1.0F - Math.min(1.0F, fadeOut - 1.0F);
      } else if (this.fadeIn) {
         alpha = this.fadeInStart > -1L ? Math.min(1.0F, (now - this.fadeInStart) / 500.0F) : 0.0F;
      } else {
         alpha = 1.0F;
      }

      SwiftLoadingScreen.forget();
      SwiftLoadingScreen.overlay(graphics, alpha, this.currentProgress, SwiftLoadingScreen.loadingLabel());
   }
}
