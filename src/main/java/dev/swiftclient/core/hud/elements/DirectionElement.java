package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;

/** Compass strip: the cardinal points slide past a centre mark as you turn. */
public final class DirectionElement extends HudElement {
   private static final String[] POINTS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
   private static final int W = 150;
   private static final int H = 18;

   public DirectionElement() {
      super("direction", "Direction HUD");
   }

   @Override
   public String icon() {
      return "compass";
   }

   @Override
   public int width(Canvas c, HudData d) {
      return W;
   }

   @Override
   public int height(Canvas c) {
      return this.opt("degrees", true) ? H + c.lineHeight() + 2 : H;
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      float yaw = d == null ? 180.0F : d.yaw();
      yaw = (yaw % 360.0F + 360.0F) % 360.0F;
      float pxPerDeg = 1.4F;
      if (this.opt("background", true)) {
         plate(c, x, y, W, H);
      }

      c.pushScissor(x + 2, y, W - 4, H);
      int cx = x + W / 2;

      for (int deg = 0; deg < 360; deg += 15) {
         float diff = deg - yaw;
         if (diff > 180.0F) {
            diff -= 360.0F;
         } else if (diff < -180.0F) {
            diff += 360.0F;
         }

         int px = cx + Math.round(diff * pxPerDeg);
         if (px >= x - 10 && px <= x + W + 10) {
            if (deg % 45 == 0) {
               String p = POINTS[deg / 45];
               int col = p.equals("N") ? -38037 : (p.length() == 1 ? -1 : -6644317);
               c.centeredText(p, px, y + 3, col, true);
            } else {
               c.fill(px, y + 5, px + 1, y + 9, 0x66FFFFFF);
            }
         }
      }

      c.popScissor();
      c.fill(cx, y + H - 5, cx + 1, y + H - 1, this.optColor("color", -12877066));
      if (this.opt("degrees", true)) {
         c.centeredText(Math.round(yaw) % 360 + "°", cx, y + H + 2, -1, true);
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"  W    NW    N    NE    E  "};
   }
}
