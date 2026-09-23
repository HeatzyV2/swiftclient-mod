package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;

public final class CoordsElement extends HudElement {
   public CoordsElement() {
      super("coords", "Coordinates");
   }

   @Override
   public String icon() {
      return "pin";
   }

   private String text(HudData d) {
      String f = d.facing() != null && !d.facing().isEmpty() ? "  " + d.facing() : "";
      return "XYZ  " + (long)Math.floor(d.x()) + "  " + (long)Math.floor(d.y()) + "  " + (long)Math.floor(d.z()) + f;
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
      return new String[]{"XYZ  128  64  -512  North"};
   }
}
