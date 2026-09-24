package dev.swiftclient.mixin;

import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.modules.ScoreboardModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Hud.class})
public class ScoreboardHudMixin {
   @Inject(
      method = {"extractScoreboardSidebar"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$ownScoreboard(GuiGraphicsExtractor ctx, DeltaTracker delta, CallbackInfo ci) {
      if (ModuleManager.get(ScoreboardModule.class).isEnabled()) {
         HudData d = HudManager.lastData();
         if (d != null && d.sidebar() != null) {
            ci.cancel();
         }
      }
   }
}
