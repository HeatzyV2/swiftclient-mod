package dev.swiftclient.mixin;

import dev.swiftclient.compat.GuiPose;
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
   private Component title;
   @Shadow
   private Component subtitle;
   @Shadow
   private int titleFadeInTime;
   @Shadow
   private int titleStayTime;
   @Shadow
   private int titleFadeOutTime;

   @Shadow
   public abstract Font getFont();

   //? if >=1.21.6 {
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
   //?} else {
   /*/^* Before 1.21.6 the boss bars are drawn by a lambda of the HUD layers. ^/
   @com.llamalad7.mixinextras.injector.v2.WrapWithCondition(
      method = {"*"},
      at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;)V")}
   )
   private boolean swiftclient$swiftBossBar(net.minecraft.client.gui.components.BossHealthOverlay overlay, GuiGraphicsExtractor graphics) {
      return !(ModuleManager.isLoaded() && ModuleManager.get(BossBarModule.class).isEnabled());
   }
   *///?}

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
      //? if >=1.21.6
      graphics.nextStratum();
      GuiPose.of(graphics).pushMatrix();
      GuiPose.of(graphics).translate(graphics.guiWidth() / 2.0F, graphics.guiHeight() / 2.0F - (float)(m.offset.value() / 100.0) * graphics.guiHeight());
      if (!m.hideTitles.boolValue()) {
         GuiPose.of(graphics).pushMatrix();
         GuiPose.of(graphics).scale(4.0F * k, 4.0F * k);
         int w = font.width(this.title);
         graphics.textWithBackdrop(font, this.title, -w / 2, -10, w, color);
         GuiPose.of(graphics).popMatrix();
      }

      if (this.subtitle != null && !m.hideSubtitles.boolValue()) {
         GuiPose.of(graphics).pushMatrix();
         GuiPose.of(graphics).scale(2.0F * k, 2.0F * k);
         int w = font.width(this.subtitle);
         graphics.textWithBackdrop(font, this.subtitle, -w / 2, 5, w, color);
         GuiPose.of(graphics).popMatrix();
      }

      GuiPose.of(graphics).popMatrix();
   }
}
