package dev.swiftclient.ui;

import dev.swiftclient.core.ui.SwiftIntro;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class LogoButton extends AbstractWidget {
   private static final Identifier LOGO_TEX = Identifier.fromNamespaceAndPath("swiftclient", "textures/gui/logo.png");
   private float hoverProgress = 0.0F;

   public LogoButton(int x, int y, int size) {
      super(x, y, size, size, Component.literal("Swift Client"));
   }

   public void onClick(MouseButtonEvent click, boolean doubled) {
   }

   public void playDownSound(SoundManager sounds) {
   }

   protected void extractWidgetRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
      float a = SwiftIntro.logoAlpha();
      if (a < 0.02F) {
         return;
      }

      boolean hovered = this.isHovered() || this.isFocused();
      float target = hovered ? 1.0F : 0.0F;
      this.hoverProgress = this.hoverProgress + (target - this.hoverProgress) * 0.18F;
      if (Math.abs(target - this.hoverProgress) < 0.001F) {
         this.hoverProgress = target;
      }

      float introScale = 0.72F + 0.28F * SwiftIntro.logoProgress();
      float scale = introScale * (1.0F + 0.1F * this.hoverProgress);
      int ox = SwiftIntro.logoOffsetX();
      int cx = this.getX() + this.width / 2 + ox;
      int cy = this.getY() + this.height / 2;
      int drawX = this.getX() + ox;
      int drawY = this.getY();

      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy);
      ctx.pose().scale(scale, scale);
      ctx.pose().translate(-cx, -cy);
      int tint = 16777215 | Math.round(255 * a) << 24;
      ctx.blit(
         RenderPipelines.GUI_TEXTURED,
         LOGO_TEX,
         drawX,
         drawY,
         0.0F,
         0.0F,
         this.width,
         this.height,
         this.width,
         this.height,
         this.width,
         this.height,
         tint
      );
      ctx.pose().popMatrix();
   }

   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }
}
