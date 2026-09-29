package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.platform.Tr;
import java.util.Locale;

/** Distance of your last hit, from your eyes to the hitbox you struck. */
public final class ReachElement extends HudElement {
   private static final long SHOWN_MS = 3000L;

   public ReachElement() {
      super("reach", "Reach Display");
   }

   @Override
   public String icon() {
      return "reach";
   }

   private String text(HudData d) {
      HudData.Combat cb = d.combat();
      boolean fresh = cb.reach() >= 0.0 && System.currentTimeMillis() - cb.lastHitMs() < SHOWN_MS;
      String v = fresh ? String.format(Locale.ROOT, "%." + (int)this.optValue("decimals", 2.0) + "f", cb.reach()) : "--";
      return Tr.of("swift.hud.reach", v);
   }

   @Override
   public int width(Canvas c, HudData d) {
      return chipW(c, this.text(d));
   }

   @Override
   public int height(Canvas c) {
      return chipH(c, 1);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      chip(c, x, y, this.text(d));
   }

   @Override
   public String[] previewLines() {
      return new String[]{"Reach 2.87"};
   }
}
