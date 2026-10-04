package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.platform.Tr;

/** Hits landed in a row without being hit back. */
public final class ComboElement extends HudElement {
   public ComboElement() {
      super("combo", "Combo Counter");
   }

   @Override
   public String icon() {
      return "combo";
   }

   private String text(HudData d) {
      int n = d.combat().combo();
      return n == 0 ? Tr.of("swift.hud.no_combo") : Tr.of(n > 1 ? "swift.hud.combo" : "swift.hud.combo_one", n);
   }

   @Override
   public int width(Canvas c, HudData d) {
      return d.combat().combo() == 0 && this.opt("hide_zero", false) ? 0 : chipW(c, this.text(d));
   }

   @Override
   public int height(Canvas c) {
      return chipH(c, 1);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      if (this.width(c, d) > 0) {
         String t = this.text(d);
         int n = d.combat().combo();
         plate(c, x, y, chipW(c, t), chipH(c, 1));
         c.text(t, x + 9, y + 4, n >= 3 ? this.optColor("color", -12877066) : -1, true);
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"4 Combo"};
   }
}
