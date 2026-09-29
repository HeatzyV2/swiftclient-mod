package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.platform.Tr;

public final class ServerElement extends HudElement {
   public ServerElement() {
      super("server", "Server Address");
   }

   @Override
   public String icon() {
      return "globe";
   }

   private String text(HudData d) {
      String a = d.serverAddress();
      if (a == null || a.isEmpty()) {
         return this.opt("singleplayer", true) ? Tr.of("swift.hud.singleplayer") : "";
      } else {
         return a;
      }
   }

   @Override
   public int width(Canvas c, HudData d) {
      String t = this.text(d);
      return t.isEmpty() ? 0 : chipW(c, t) + (this.opt("icon", true) ? 12 : 0);
   }

   @Override
   public int height(Canvas c) {
      return chipH(c, 1);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      String t = this.text(d);
      if (!t.isEmpty()) {
         boolean icon = this.opt("icon", true);
         c.card(x, y, this.width(c, d), chipH(c, 1), 1996488704, 587202559, 1, 4.0F);
         if (icon) {
            c.icon("globe", x + 5, y + 3, 9, -12877066);
         }

         c.text(t, x + 6 + (icon ? 12 : 0), y + 4, -1, true);
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"play.swiftclient.fr"};
   }
}
