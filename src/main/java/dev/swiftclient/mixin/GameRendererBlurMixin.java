package dev.swiftclient.mixin;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
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
   private void lightclient$guiBlur(DeltaTracker deltaTracker, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.gui.screen() != null && ModuleManager.active("gui_blur")) {
         Module m = ModuleManager.byId("gui_blur");
         ModuleSetting sStr = m.setting("strength");
         ModuleSetting sDim = m.setting("darkness");
         float blur = (float)((sStr == null ? 60.0 : sStr.value()) / 100.0 * 28.0);
         int dimA = (int)Math.round((sDim == null ? 25.0 : sDim.value()) / 100.0 * 180.0);
         int tint = Math.max(0, Math.min(255, dimA)) << 24;
         ScreenBlur.draw(blur, tint);
      }
   }
}
