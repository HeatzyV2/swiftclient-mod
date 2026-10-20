package dev.swiftclient.core.ui.widget;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.ui.SwiftIntro;
import dev.swiftclient.core.ui.SwiftSounds;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Title screen button, in the chamfered Swift shape of the module tiles. PRIMARY is the big accent call to
 * action, ROW a menu entry with its icon, SMALL and DANGER the compact buttons of the bottom row. Buttons slide
 * in from the left with the Swift intro and lean right on hover.
 */
public final class SwiftMenuButton implements CoreWidget {
   public enum Style {
      PRIMARY,
      ROW,
      SMALL,
      DANGER
   }

   private static final int BODY = 0xE611141B;
   private static final int FRAME = 0xFF222838;
   private static final int ICON = 0xFF98A3B8;
   private static final int LABEL = 0xFFC9D3E6;
   private static final int RED = 0xFFE05A5A;
   private final Supplier<String> label;
   private final String icon;
   private final Runnable click;
   private final Style style;
   private final int introIndex;
   private boolean wasHovered;
   private float hover;

   public SwiftMenuButton(Supplier<String> label, String icon, Runnable click, Style style, int introIndex) {
      this.label = label;
      this.icon = icon;
      this.click = click;
      this.style = style;
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
      this.hover += ((hovered && active ? 1.0F : 0.0F) - this.hover) * 0.25F;
      int dx = x - Math.round((1.0F - a) * 36.0F);
      String text = this.label.get();
      switch (this.style) {
         case PRIMARY -> this.primary(c, dx, y, w, h, text, a);
         case ROW -> this.row(c, dx + Math.round(3.0F * this.hover), y, w, h, text, active, a);
         default -> this.small(c, dx, y, w, h, text, active, a);
      }
   }

   private void primary(Canvas c, int x, int y, int w, int h, String text, float a) {
      int body = this.hover > 0.5F ? 0xFF4F8FF8 : Px.ACCENT;
      Px.slot(c, x, y, w, h, 9, alpha(body, a), alpha(this.hover > 0.5F ? 0xFFBFDBFE : 0xFF6EA2F9, a));
      int is = 12;
      c.icon(this.icon, x + 12, y + (h - is) / 2, is, alpha(0xFFFFFFFF, a));
      String caps = text.toUpperCase(Locale.ROOT);
      c.text(caps, x + 32, y + (h - c.lineHeight()) / 2 + 1, alpha(0xFFFFFFFF, a), false);
      int sx = x + w - 44 + Math.round(6.0F * this.hover);
      Px.dashes(c, sx, y + h / 2, alpha(0xFFFFFFFF, a));
   }

   private void row(Canvas c, int x, int y, int w, int h, String text, boolean active, float a) {
      int frame = this.hover > 0.5F ? Px.ACCENT : FRAME;
      int body = this.hover > 0.5F ? 0xF0151C2C : BODY;
      Px.slot(c, x, y, w, h, 6, alpha(body, a), alpha(frame, a));
      int col = !active ? 0xFF5A6070 : (this.hover > 0.5F ? 0xFFFFFFFF : LABEL);
      int iconCol = !active ? 0xFF4A5060 : (this.hover > 0.5F ? Px.ACCENT : ICON);
      int is = 11;
      c.icon(this.icon, x + 10, y + (h - is) / 2, is, alpha(iconCol, a));
      c.text(text.toUpperCase(Locale.ROOT), x + 28, y + (h - c.lineHeight()) / 2 + 1, alpha(col, a), false);
      if (this.hover > 0.05F) {
         int cs = 9;
         c.icon("chevron_right", x + w - 16 + Math.round(3.0F * this.hover), y + (h - cs) / 2, cs, alpha(Px.ACCENT, a * this.hover));
      }
   }

   private void small(Canvas c, int x, int y, int w, int h, String text, boolean active, float a) {
      boolean danger = this.style == Style.DANGER;
      int lit = danger ? RED : Px.ACCENT;
      boolean on = this.hover > 0.5F;
      Px.slot(c, x, y, w, h, 4, alpha(on ? 0xF0151C2C : BODY, a), alpha(on ? lit : FRAME, a));
      int is = 9;
      int tw = c.textWidth(text);
      int total = is + 5 + tw;
      int sx = x + (w - total) / 2;
      c.icon(this.icon, sx, y + (h - is) / 2, is, alpha(on ? lit : ICON, a));
      c.text(text, sx + is + 5, y + (h - c.lineHeight()) / 2 + 1, alpha(!active ? 0xFF5A6070 : (on ? 0xFFFFFFFF : LABEL), a), false);
   }

   private static int alpha(int argb, float a) {
      return Math.round(((argb >>> 24) & 255) * a) << 24 | argb & 0xFFFFFF;
   }
}
