package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.HudStats;

public final class FpsElement extends HudElement {
   public FpsElement() {
      super("fps", "FPS");
   }

   private int value() {
      return this.opt("smooth", true) ? HudStats.fpsSmoothed() : HudStats.fps();
   }

   private String text() {
      int v = this.value();
      return this.opt("label", true) ? v + " FPS" : String.valueOf(v);
   }

   private int color() {
      if (!this.opt("colored", true)) {
         return -1;
      } else {
         int v = this.value();
         return v >= 60 ? -9707413 : (v >= 30 ? -865972 : -38037);
      }
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
      String t = this.text();
      int w = chipW(c, new String[]{t});
      int h = chipH(c, 1);
      c.card(x, y, w, h, 1996488704, 587202559, 1, 4.0F);
      c.text(t, x + 6, y + 4, this.color(), true);
   }

   @Override
   public boolean drawsWithoutData() {
      return true;
   }

   @Override
   public String[] previewLines() {
      return new String[]{"144 FPS"};
   }
}
