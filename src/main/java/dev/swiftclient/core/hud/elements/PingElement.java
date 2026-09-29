package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;

public final class PingElement extends HudElement {
   public PingElement() {
      super("ping", "Ping");
   }

   @Override
   public String icon() {
      return "signal";
   }

   private String text(HudData d) {
      int p = d.ping();
      String v = p < 0 ? "--" : String.valueOf(p);
      return this.opt("label", true) ? v + " ms" : v;
   }

   static int color(int ping) {
      if (ping < 0) {
         return -6644317;
      } else {
         return ping < 80 ? -9707413 : (ping < 160 ? -865972 : -38037);
      }
   }

   @Override
   public int width(Canvas c, HudData d) {
      return d.ping() >= 0 || this.opt("singleplayer", false) ? chipW(c, this.text(d)) : 0;
   }

   @Override
   public int height(Canvas c) {
      return chipH(c, 1);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      if (this.width(c, d) > 0) {
         String t = this.text(d);
         c.card(x, y, chipW(c, t), chipH(c, 1), 1996488704, 587202559, 1, 4.0F);
         c.text(t, x + 6, y + 4, this.opt("colored", true) ? color(d.ping()) : -1, true);
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"42 ms"};
   }
}
