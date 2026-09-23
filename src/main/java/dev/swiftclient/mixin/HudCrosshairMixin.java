package dev.swiftclient.mixin;

import dev.swiftclient.core.hud.Crosshair;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.platform.CanvasImpl;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Hud.class})
public class HudCrosshairMixin {
   @Inject(
      method = {"extractCrosshair"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void lightclient$customCrosshair(GuiGraphicsExtractor ctx, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (ModuleManager.active("crosshair")) {
         Minecraft mc = Minecraft.getInstance();
         int cx = mc.getWindow().getGuiScaledWidth() / 2;
         int cy = mc.getWindow().getGuiScaledHeight() / 2;
         Crosshair.draw(new CanvasImpl(ctx), cx, cy);
         ci.cancel();
      }
   }
}
