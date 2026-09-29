package dev.swiftclient.mixin;

import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.modules.BossBarModule;
import dev.swiftclient.core.mods.modules.TitlesModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Boss Bar (vanilla bars hidden, Swift element drawn instead) and Titles (size, height, hide). */
@Mixin({Hud.class})
public abstract class HudOverlaysMixin {
   @Shadow
   private int titleTime;
   @Shadow
   private @Nullable Component title;
   @Shadow
   private @Nullable Component subtitle;
   @Shadow
   private int titleFadeInTime;
   @Shadow
   private int titleStayTime;
   @Shadow
   private int titleFadeOutTime;

   @Shadow
   public abstract Font getFont();

   @Inject(
      method = {"extractBossOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$swiftBossBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (ModuleManager.isLoaded() && ModuleManager.get(BossBarModule.class).isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"extractTitle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$titles(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (!ModuleManager.isLoaded()) {
         return;
      }

      TitlesModule m = ModuleManager.get(TitlesModule.class);
      if (!m.isEnabled()) {
         return;
      }

      ci.cancel();
      if (this.title == null || this.titleTime <= 0) {
         return;
      }

      float t = this.titleTime - deltaTracker.getGameTimeDeltaPartialTick(false);
      int alpha = 255;
      if (this.titleTime > this.titleFadeOutTime + this.titleStayTime) {
         alpha = (int)((this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime - t) * 255.0F / this.titleFadeInTime);
      }

      if (this.titleTime <= this.titleFadeOutTime) {
         alpha = (int)(t * 255.0F / this.titleFadeOutTime);
      }

      alpha = Mth.clamp(alpha, 0, 255);
      if (alpha <= 0) {
         return;
      }

      Font font = this.getFont();
      float k = (float)(m.scale.value() / 100.0);
      int color = ARGB.white(alpha);
      graphics.nextStratum();
      graphics.pose().pushMatrix();
      graphics.pose().translate(graphics.guiWidth() / 2.0F, graphics.guiHeight() / 2.0F - (float)(m.offset.value() / 100.0) * graphics.guiHeight());
      if (!m.hideTitles.boolValue()) {
         graphics.pose().pushMatrix();
         graphics.pose().scale(4.0F * k, 4.0F * k);
         int w = font.width(this.title);
         graphics.textWithBackdrop(font, this.title, -w / 2, -10, w, color);
         graphics.pose().popMatrix();
      }

      if (this.subtitle != null && !m.hideSubtitles.boolValue()) {
         graphics.pose().pushMatrix();
         graphics.pose().scale(2.0F * k, 2.0F * k);
         int w = font.width(this.subtitle);
         graphics.textWithBackdrop(font, this.subtitle, -w / 2, 5, w, color);
         graphics.pose().popMatrix();
      }

      graphics.pose().popMatrix();
   }
}
