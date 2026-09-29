package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.HudStats;
import dev.swiftclient.core.platform.Tr;

/** Time spent in the current world or server since you joined it. */
public final class PlaytimeElement extends HudElement {
   public PlaytimeElement() {
      super("playtime", "Playtime");
   }

   @Override
   public String icon() {
      return "clock";
   }

   static String hms(long ms) {
      long s = Math.max(0L, ms / 1000L);
      long h = s / 3600L;
      long m = s / 60L % 60L;
      return h > 0L ? String.format("%d:%02d:%02d", h, m, s % 60L) : String.format("%d:%02d", m, s % 60L);
   }

   private String text() {
      String t = hms(HudStats.sessionMillis());
      return this.opt("label", true) ? Tr.of("swift.hud.playtime", t) : t;
   }

   @Override
   public int width(Canvas c, HudData d) {
      return chipW(c, this.text());
   }

   @Override
   public int height(Canvas c) {
      return chipH(c, 1);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      chip(c, x, y, this.text());
   }

   @Override
   public String[] previewLines() {
      return new String[]{"Playtime 1:24:07"};
   }
}
