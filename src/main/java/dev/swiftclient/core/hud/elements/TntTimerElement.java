package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;

public final class TntTimerElement extends HudElement {
   public TntTimerElement() {
      super("tnt", "TNT timer");
   }

   @Override
   public String icon() {
      return "bomb";
   }

   private String text(HudData d) {
      return "TNT  " + String.format("%.1f", d.tntFuseTicks() / 20.0F) + "s";
   }

   @Override
   public int width(Canvas c, HudData d) {
      return d.tntFuseTicks() < 0 ? 0 : chipW(c, new String[]{this.text(d)});
   }

   @Override
   public int height(Canvas c) {
      return chipH(c, 1);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      if (d.tntFuseTicks() >= 0) {
         chip(c, x, y, new String[]{this.text(d)});
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"TNT  2.4s"};
   }
}
