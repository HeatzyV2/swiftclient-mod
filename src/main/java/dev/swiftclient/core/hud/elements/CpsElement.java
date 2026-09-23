package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.HudStats;

public final class CpsElement extends HudElement {
   public CpsElement() {
      super("cps", "CPS");
   }

   private String text() {
      int mode = this.optCycle("button", 0);
      boolean label = this.opt("label", true);

      String s = switch (mode) {
         case 1 -> String.valueOf(HudStats.rightCps());
         case 2 -> HudStats.leftCps() + " | " + HudStats.rightCps();
         default -> String.valueOf(HudStats.leftCps());
      };
      return label ? s + " CPS" : s;
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
      return new String[]{"12 CPS"};
   }
}
