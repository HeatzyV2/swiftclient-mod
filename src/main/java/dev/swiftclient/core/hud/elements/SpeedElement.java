package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.HudStats;
import java.util.Locale;

public final class SpeedElement extends HudElement {
   public SpeedElement() {
      super("speed", "Speed");
   }

   private String text() {
      double v = this.opt("vertical", false) ? HudStats.speedTotal() : HudStats.speedHorizontal();
      boolean kmh = this.optCycle("unit", 0) == 1;
      if (kmh) {
         v *= 3.6;
      }

      int dec = (int)Math.round(this.optValue("decimals", 2.0));
      String num = String.format(Locale.ROOT, "%." + dec + "f", v);
      return this.opt("label", true) ? num + (kmh ? " km/h" : " m/s") : num;
   }

   @Override
   public int width(Canvas c, HudData d) {
      return chipW(c, new String[]{this.text()});
   }

   @Override
   public int height(Canvas c) {
      return chipH(c, 1);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      chip(c, x, y, new String[]{this.text()});
   }

   @Override
   public boolean drawsWithoutData() {
      return true;
   }

   @Override
   public String[] previewLines() {
      return new String[]{"5.61 m/s"};
   }
}
