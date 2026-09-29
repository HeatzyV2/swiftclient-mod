package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import java.util.ArrayList;
import java.util.List;

/** "+12 Cobblestone": a feed of the items entering (and optionally leaving) your inventory. */
public final class ItemTrackerElement extends HudElement {
   private static final int ROW = 17;

   public ItemTrackerElement() {
      super("itemtracker", "Item Tracker");
   }

   @Override
   public String icon() {
      return "pickup";
   }

   private List<HudData.Pickup> shown(HudData d) {
      long life = Math.round(this.optValue("duration", 4.0) * 1000.0);
      boolean losses = this.opt("losses", false);
      long now = System.currentTimeMillis();
      List<HudData.Pickup> out = new ArrayList<>();
      for (HudData.Pickup p : d.pickups()) {
         if (now - p.timeMs() < life && (losses || p.delta() > 0)) {
            out.add(p);
            if (out.size() >= 6) {
               break;
            }
         }
      }

      return out;
   }

   private static String label(HudData.Pickup p) {
      return (p.delta() > 0 ? "+" : "") + p.delta() + " " + p.name();
   }

   @Override
   public int width(Canvas c, HudData d) {
      int w = 0;
      for (HudData.Pickup p : this.shown(d)) {
         w = Math.max(w, c.textWidth(label(p)));
      }

      return w == 0 ? 0 : w + 26;
   }

   @Override
   public int height(Canvas c) {
      return ROW;
   }

   @Override
   public int height(Canvas c, HudData d) {
      return Math.max(1, this.shown(d).size()) * ROW;
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      long life = Math.round(this.optValue("duration", 4.0) * 1000.0);
      long now = System.currentTimeMillis();
      int yy = y;

      for (HudData.Pickup p : this.shown(d)) {
         float left = 1.0F - (now - p.timeMs()) / (float)life;
         int a = Math.max(24, Math.min(255, Math.round(left * 4.0F * 255.0F)));
         c.itemStack(p.stack(), x + 2, yy);
         int col = p.delta() > 0 ? 0x00FFFFFF : 0x00FF6B6B;
         c.text(label(p), x + 22, yy + 4, a << 24 | col, true);
         yy += ROW;
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"+32 Cobblestone", "+1 Diamond"};
   }
}
