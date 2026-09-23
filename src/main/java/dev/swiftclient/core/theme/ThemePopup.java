package dev.swiftclient.core.theme;

import dev.swiftclient.core.gfx.Canvas;
import java.util.List;

public class ThemePopup {
   private static final int PANEL_W = 128;
   private static final int ROW_H = 22;
   private static final int PAD = 4;
   private static final int MARGIN = 12;
   private static final int TOP = 52;
   private static final long ANIM = 190L;
   private static final int BG = -267251182;
   private static final int LINE = -14013904;
   private static final int ROW_HOVER = -14737626;
   private static final int ROW_SEL = -14013894;
   private static final int TEXT = -1;
   private static final int TEXT_DIM = -5592406;
   private static final int ACCENT = -12877066;
   private boolean targetOpen;
   private long animStart;
   private float animFrom;

   private static long now() {
      return System.currentTimeMillis();
   }

   public boolean isOpen() {
      return this.targetOpen;
   }

   public void toggle() {
      this.animFrom = this.progress();
      this.targetOpen = !this.targetOpen;
      this.animStart = now();
   }

   private void startClose() {
      if (this.targetOpen) {
         this.animFrom = this.progress();
         this.targetOpen = false;
         this.animStart = now();
      }
   }

   private float progress() {
      float target = this.targetOpen ? 1.0F : 0.0F;
      long dt = now() - this.animStart;
      if (dt >= 190L) {
         return target;
      } else {
         float t = (float)dt / 190.0F;
         t = 1.0F - (1.0F - t) * (1.0F - t);
         return this.animFrom + (target - this.animFrom) * t;
      }
   }

   private int panelX(int screenW) {
      return screenW - 128 - 12;
   }

   private int fullH() {
      return 8 + Themes.all().size() * 22;
   }

   public void draw(Canvas c, int screenW, int mouseX, int mouseY) {
      float p = this.progress();
      if (!(p <= 0.01F)) {
         List<Theme> themes = Themes.all();
         int x = this.panelX(screenW);
         int full = this.fullH();
         int h = Math.max(1, Math.round(full * p));
         c.buttonBackground(x, TOP, 128, h, false);
         c.pushScissor(x, TOP, 128, h);
         String cur = ThemeManager.currentId();
         int ry = TOP + 4;

         for (Theme t : themes) {
            boolean over = this.targetOpen && mouseX >= x && mouseX <= x + 128 && mouseY >= ry && mouseY <= ry + 22;
            boolean sel = t.id().equals(cur);
            if (sel || over) {
               c.roundRect(x + 2, ry, 124, 22, 2.0F, sel ? -14013894 : -14737626);
            }

            if (sel) {
               c.fill(x + 2, ry + 3, x + 4, ry + 19, -12877066);
            }

            int sw = 14;
            int sx = x + 8;
            int sy = ry + (22 - sw) / 2;
            if (t.top() == t.bottom()) {
               c.fill(sx, sy, sx + sw, sy + sw, t.top());
            } else {
               c.gradientV(sx, sy, sw, sw, t.top(), t.bottom());
            }

            c.text(t.name(), sx + sw + 6, ry + (22 - c.lineHeight()) / 2 + 1, sel ? -1 : -5592406, false);
            ry += 22;
         }

         c.popScissor();
      }
   }

   public boolean clickAt(int screenW, double mouseX, double mouseY) {
      if (!this.targetOpen) {
         return false;
      } else {
         int x = this.panelX(screenW);
         int full = this.fullH();
         if (!(mouseX < x) && !(mouseX > x + 128) && !(mouseY < TOP) && !(mouseY > TOP + full)) {
            int ry = TOP + 4;

            for (Theme t : Themes.all()) {
               if (mouseY >= ry && mouseY <= ry + 22) {
                  ThemeManager.select(t.id());
                  this.startClose();
                  return true;
               }

               ry += 22;
            }

            return true;
         } else {
            this.startClose();
            return false;
         }
      }
   }
}
