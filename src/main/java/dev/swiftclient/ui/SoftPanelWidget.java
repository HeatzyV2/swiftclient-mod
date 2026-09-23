package dev.swiftclient.ui;

import dev.swiftclient.platform.CanvasImpl;
import dev.swiftclient.renderer.RoundedButtons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Non-interactive soft glass panel drawn behind title menu controls. */
public final class SoftPanelWidget extends AbstractWidget {
   public SoftPanelWidget(int x, int y, int w, int h) {
      super(x, y, w, h, Component.empty());
      this.active = false;
   }

   public void onClick(MouseButtonEvent click, boolean doubled) {
   }

   protected void extractWidgetRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
      new CanvasImpl(ctx);
      RoundedButtons.drawSoftPanel(this.getX(), this.getY(), this.width, this.height);
   }

   protected boolean isValidClickButton(MouseButtonEvent click) {
      return false;
   }

   protected void updateWidgetNarration(NarrationElementOutput output) {
   }
}
