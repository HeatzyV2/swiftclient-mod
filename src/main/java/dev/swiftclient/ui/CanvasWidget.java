package dev.swiftclient.ui;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.ui.widget.CoreWidget;
import dev.swiftclient.platform.CanvasImpl;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class CanvasWidget extends AbstractWidget {
   private final CoreWidget widget;

   public CanvasWidget(int x, int y, int width, int height, Component narration, CoreWidget widget) {
      super(x, y, width, height, narration);
      this.widget = widget;
      this.active = widget.enabled();
      String tip = widget.tooltip();
      if (tip != null) {
         this.setTooltip(Tooltip.create(Component.literal(tip)));
      }
   }

   public void onClick(MouseButtonEvent click, boolean doubled) {
      if (this.visible && (this.active || this.widget.clickWhenDisabled())) {
         this.widget.onClick();
      }
   }

   public void playDownSound(net.minecraft.client.sounds.SoundManager sounds) {
      // SwiftSounds fired from CoreWidget.onClick — mute vanilla UI click
   }

   protected void extractWidgetRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
      Canvas c = new CanvasImpl(ctx);
      this.widget.draw(c, this.getX(), this.getY(), this.width, this.height, this.isHovered() || this.isFocused(), this.active, mouseX, mouseY, delta);
   }

   protected void updateWidgetNarration(NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }
}
