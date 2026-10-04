package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import java.util.List;

/** Items on cooldown (ender pearl, chorus fruit, shield, wind charge...) with a draining bar under each. */
public final class CooldownsElement extends HudElement {
   private static final int CELL = 20;

   public CooldownsElement() {
      super("cooldowns", "Cooldowns");
   }

   @Override
   public String icon() {
      return "hourglass";
   }

   @Override
   public int width(Canvas c, HudData d) {
      int n = d.cooldowns().size();
      return n == 0 ? 0 : n * CELL + 4;
   }

   @Override
   public int height(Canvas c) {
      return CELL + 6;
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      List<HudData.Cooldown> list = d.cooldowns();
      if (!list.isEmpty()) {
         if (this.opt("background", true)) {
            plate(c, x, y, this.width(c, d), CELL + 6);
         }

         int cx = x + 4;
         int accent = this.optColor("color", -12877066);

         for (HudData.Cooldown cd : list) {
            c.itemStack(cd.stack(), cx, y + 3);
            int bw = 16;
            c.fill(cx, y + CELL + 1, cx + bw, y + CELL + 3, 0x55000000);
            c.fill(cx, y + CELL + 1, cx + Math.round(bw * cd.fraction()), y + CELL + 3, accent);
            cx += CELL;
         }
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"Pearl  ====  Shield  =="};
   }
}
