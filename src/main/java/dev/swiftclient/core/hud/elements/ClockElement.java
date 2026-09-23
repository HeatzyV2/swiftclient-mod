package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class ClockElement extends HudElement {
   private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("HH:mm");

   public ClockElement() {
      super("clock", "Clock");
   }

   @Override
   public String icon() {
      return "clock";
   }

   private String text() {
      return LocalTime.now().format(F);
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
   public String[] previewLines() {
      return new String[]{this.text()};
   }
}
