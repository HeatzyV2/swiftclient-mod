package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;

public final class DayCounterElement extends HudElement {
   public DayCounterElement() {
      super("days", "Day counter");
   }

   @Override
   public String icon() {
      return "sunrise";
   }

   private String text(HudData d) {
      return "Day " + (d.worldTime() / 24000L + 1L);
   }

   @Override
   public int width(Canvas c, HudData d) {
      return chipW(c, new String[]{this.text(d)});
   }

   @Override
   public int height(Canvas c) {
      return chipH(c, 1);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      chip(c, x, y, new String[]{this.text(d)});
   }

   @Override
   public String[] previewLines() {
      return new String[]{"Day 128"};
   }
}
