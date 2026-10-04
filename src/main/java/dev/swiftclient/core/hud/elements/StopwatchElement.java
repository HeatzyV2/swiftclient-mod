package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.Stopwatch;

public final class StopwatchElement extends HudElement {
   public StopwatchElement() {
      super("stopwatch", "Stopwatch");
   }

   @Override
   public String icon() {
      return "timer";
   }

   private String text() {
      long ms = Stopwatch.elapsed();
      long s = ms / 1000L;
      String t = this.opt("millis", true) ? String.format("%d:%02d.%02d", s / 60L, s % 60L, ms % 1000L / 10L) : String.format("%d:%02d", s / 60L, s % 60L);
      return (Stopwatch.running() ? "> " : "|| ") + t;
   }

   @Override
   public int width(Canvas c, HudData d) {
      return chipW(c, "|| 00:00.00");
   }

   @Override
   public int height(Canvas c) {
      return chipH(c, 1);
   }

   @Override
   public boolean drawsWithoutData() {
      return true;
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      plate(c, x, y, this.width(c, d), chipH(c, 1));
      c.text(this.text(), x + 9, y + 4, Stopwatch.running() ? -9707413 : -1, true);
   }

   @Override
   public String[] previewLines() {
      return new String[]{"> 1:07.42"};
   }
}
