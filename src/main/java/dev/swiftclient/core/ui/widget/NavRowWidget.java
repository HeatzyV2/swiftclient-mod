package dev.swiftclient.core.ui.widget;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.ui.SwiftIntro;
import dev.swiftclient.core.ui.SwiftSounds;
import java.util.function.Supplier;

/** Text navigation row — speed-bar CTA + cascade intro. */
public final class NavRowWidget implements CoreWidget {
   private static final int IDLE = -4473925;
   private static final int HOVER = -1;
   private static final int ACCENT = -12877066;
   private final Supplier<String> label;
   private final Runnable click;
   private final boolean primary;
   private final int introIndex;
   private boolean wasHovered;

   public NavRowWidget(Supplier<String> label, Runnable click) {
      this(label, click, false, -1);
   }

   public NavRowWidget(Supplier<String> label, Runnable click, boolean primary) {
      this(label, click, primary, primary ? 0 : -1);
   }

   public NavRowWidget(Supplier<String> label, Runnable click, boolean primary, int introIndex) {
      this.label = label;
      this.click = click;
      this.primary = primary;
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

      int slide = this.introIndex < 0 ? 0 : Math.round((1.0F - a) * 28.0F);
      int dx = x + slide;
      String text = this.label.get();

      if (this.primary) {
         this.drawSpeedBar(c, dx, y, w, h, hovered, a);
         c.centeredText(text, dx + w / 2, y + (h - 8) / 2, mulAlpha(-1, a), false);
         return;
      }

      if (hovered && a > 0.85F) {
         c.fill(dx, y + 3, dx + 2, y + h - 3, mulAlpha(ACCENT, a));
      }

      int col = !active ? -10066330 : (hovered ? HOVER : IDLE);
      int tw = c.textWidth(text);
      c.text(text, dx + (w - tw) / 2, y + (h - c.lineHeight()) / 2 + 1, mulAlpha(col, a), false);
   }

   /** Solo CTA — blue speed bar (not LightClient pill). */
   private void drawSpeedBar(Canvas c, int x, int y, int w, int h, boolean hovered, float a) {
      int bg = hovered ? ACCENT : -1606712586;
      int yy = y + 2;
      int hh = h - 4;
      // Body
      c.fill(x + 6, yy, x + w, yy + hh, mulAlpha(bg, a));
      // Leading edge tick (speed mark)
      c.fill(x, yy, x + 5, yy + hh, mulAlpha(-1, a));
      c.fill(x + 5, yy, x + 7, yy + hh, mulAlpha(ACCENT, a));
      // Trailing speed lines
      int tip = hovered ? -1 : ACCENT;
      for (int i = 0; i < 3; i++) {
         int lx = x + w - 8 - i * 7;
         int ly = yy + 3 + i;
         c.fill(lx, ly, lx + 4 - i, yy + hh - 3 - i, mulAlpha(tip, a * (0.7F - i * 0.15F)));
      }
   }

   private static int mulAlpha(int argb, float a) {
      int alpha = Math.round(((argb >>> 24) & 255) * a);
      return argb & 16777215 | alpha << 24;
   }
}
