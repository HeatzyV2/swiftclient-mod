package dev.swiftclient.mixin;

import dev.swiftclient.core.mods.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Screen.class})
public abstract class ScreenMixin {
   @Shadow
   public abstract boolean isInGameUi();

   @Shadow
   protected abstract void extractPanorama(GuiGraphicsExtractor var1, float var2);

   @Inject(
      method = {"extractBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void lightclient$panoramaEverywhere(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!ModuleManager.active("vanillaui")) {
         if (!this.isInGameUi()) {
            this.extractPanorama(ctx, delta);
            ci.cancel();
         } else if (ModuleManager.active("gui_blur")) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"panoramaShouldSpin"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void lightclient$alwaysSpin(CallbackInfoReturnable<Boolean> cir) {
      if (!ModuleManager.active("vanillaui")) {
         if (!this.isInGameUi()) {
            cir.setReturnValue(true);
         }
      }
   }
}
