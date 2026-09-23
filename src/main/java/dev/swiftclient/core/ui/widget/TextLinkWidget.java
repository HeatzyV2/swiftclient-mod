package dev.swiftclient.core.ui.widget;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.ui.SwiftIntro;
import dev.swiftclient.core.ui.SwiftSounds;
import java.util.function.Supplier;

/** Subtle text action — Quit / secondary links. */
public final class TextLinkWidget implements CoreWidget {
   private final Supplier<String> label;
   private final Runnable click;
   private final int introIndex;
   private boolean wasHovered;

   public TextLinkWidget(Supplier<String> label, Runnable click) {
      this(label, click, -1);
   }

   public TextLinkWidget(Supplier<String> label, Runnable click, int introIndex) {
      this.label = label;
      this.click = click;
      this.introIndex = introIndex;
   }

   @Override
   public void onClick() {
      if (this.introIndex >= 0 && !SwiftIntro.rowReady(this.introIndex)) {
         return;
      }
      SwiftSounds.click();
      if (this.click != null) {
         this.click.run();
      }
   }

   @Override
   public void draw(Canvas c, int x, int y, int w, int h, boolean hovered, boolean active, int mouseX, int mouseY, float delta) {
      float a = this.introIndex < 0 ? 1.0F : SwiftIntro.rowAlpha(this.introIndex);
      if (a < 0.02F) {
         return;
      }
      if (hovered && !this.wasHovered && a > 0.9F) {
         SwiftSounds.hover();
      }
      this.wasHovered = hovered;

      String text = this.label.get();
      int col = !active ? -10066330 : (hovered ? -12877066 : -3355444);
      int alpha = Math.round(255 * a) << 24;
      col = col & 16777215 | alpha;
      c.centeredText(text, x + w / 2, y + (h - 8) / 2, col, false);
      if (hovered && active) {
         int tw = c.textWidth(text);
         int ux = x + (w - tw) / 2;
         int uy = y + (h - 8) / 2 + 10;
         c.fill(ux, uy, ux + tw, uy + 1, -12877066 & 16777215 | alpha);
      }
   }
}
