package dev.swiftclient.core.ui.widget;

import dev.swiftclient.core.gfx.Canvas;
import java.util.function.Supplier;

public final class LabelButtonWidget implements CoreWidget {
   private final Supplier<String> label;
   private final Runnable click;

   public LabelButtonWidget(Supplier<String> label, Runnable click) {
      this.label = label;
      this.click = click;
   }

   @Override
   public void onClick() {
      if (this.click != null) {
         this.click.run();
      }
   }

   @Override
   public void draw(Canvas c, int x, int y, int w, int h, boolean hovered, boolean active, int mouseX, int mouseY, float delta) {
      c.buttonBackground(x, y, w, h, hovered);
      int textColor = active ? -1 : -5592406;
      c.centeredText(this.label.get(), x + w / 2, y + (h - 8) / 2, textColor, true);
   }
}
