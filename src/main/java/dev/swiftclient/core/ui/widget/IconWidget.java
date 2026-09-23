package dev.swiftclient.core.ui.widget;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.renderer.RoundedButtons;

public final class IconWidget implements CoreWidget {
   private final String iconName;
   private final boolean disabled;
   private final String tip;
   private final Runnable click;

   public IconWidget(String iconName, boolean disabled, String tooltip, Runnable click) {
      this.iconName = iconName;
      this.disabled = disabled;
      this.tip = tooltip;
      this.click = click;
   }

   @Override
   public boolean enabled() {
      return !this.disabled;
   }

   @Override
   public String tooltip() {
      return this.tip;
   }

   @Override
   public void onClick() {
      if (!this.disabled && this.click != null) {
         this.click.run();
      }
   }

   @Override
   public void draw(Canvas c, int x, int y, int w, int h, boolean hovered, boolean active, int mouseX, int mouseY, float delta) {
      boolean hover = hovered && !this.disabled;
      int size = Math.min(w, h);
      int cx = x + (w - size) / 2;
      int cy = y + (h - size) / 2;
      RoundedButtons.drawCircle(cx, cy, size, hover, !this.disabled);
      int iconSize = (int)(size * 0.48F);
      int ix = cx + (size - iconSize) / 2;
      int iy = cy + (size - iconSize) / 2;
      int tint = this.disabled ? -10066330 : (hover ? -1 : -4473925);
      c.icon(this.iconName, ix, iy, iconSize, tint);
   }
}
