package dev.swiftclient.mixin;

import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.modules.GuiBlurModule;
import dev.swiftclient.renderer.blur.ScreenBlur;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public class GameRendererBlurMixin {
   @Inject(
      method = {"renderLevel"},
      at = {@At("TAIL")}
   )
   private void swiftclient$guiBlur(DeltaTracker deltaTracker, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      GuiBlurModule m = ModuleManager.get(GuiBlurModule.class);
      if (mc.gui.screen() != null && m.isEnabled()) {
         float blur = (float)(m.strength.value() / 100.0 * 28.0);
         int dimA = (int)Math.round(m.darkness.value() / 100.0 * 180.0);
         int tint = Math.max(0, Math.min(255, dimA)) << 24;
         ScreenBlur.draw(blur, tint);
      }
   }
}
