package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateElement extends HudElement {
   private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);

   public DateElement() {
      super("date", "Date");
   }

   @Override
   public String icon() {
      return "calendar";
   }

   private String text() {
      return LocalDate.now().format(F);
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
