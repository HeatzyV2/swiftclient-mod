package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import java.util.ArrayList;
import java.util.List;

/** How many arrows, pearls, golden apples, potions, totems and blocks you carry. */
public final class ItemCounterElement extends HudElement {
   private static final int ROW = 18;

   public ItemCounterElement() {
      super("itemcounter", "Item Counter");
   }

   @Override
   public String icon() {
      return "stack";
   }

   private List<HudData.ItemCount> shown(HudData d) {
      List<HudData.ItemCount> out = new ArrayList<>();
      boolean hideEmpty = this.opt("hide_empty", true);
      for (HudData.ItemCount ic : d.itemCounts()) {
         if (this.opt(ic.key(), true) && (!hideEmpty || ic.count() > 0)) {
            out.add(ic);
         }
      }

      return out;
   }

   private boolean horizontal() {
      return this.optCycle("layout", 1) == 0;
   }

   @Override
   public int width(Canvas c, HudData d) {
      List<HudData.ItemCount> l = this.shown(d);
      if (l.isEmpty()) {
         return 0;
      } else if (this.horizontal()) {
         int w = 4;
         for (HudData.ItemCount ic : l) {
            w += 18 + c.textWidth(String.valueOf(ic.count())) + 6;
         }

         return w;
      } else {
         int tw = 0;
         for (HudData.ItemCount ic : l) {
            tw = Math.max(tw, c.textWidth(String.valueOf(ic.count())));
         }

         return 4 + 18 + tw + 6;
      }
   }

   @Override
   public int height(Canvas c) {
      return ROW + 2;
   }

   @Override
   public int height(Canvas c, HudData d) {
      int n = Math.max(1, this.shown(d).size());
      return this.horizontal() ? ROW + 2 : n * ROW + 2;
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      List<HudData.ItemCount> l = this.shown(d);
      if (!l.isEmpty()) {
         if (this.opt("background", true)) {
            c.card(x, y, this.width(c, d), this.height(c, d), 1996488704, 587202559, 1, 4.0F);
         }

         int cx = x + 3;
         int cy = y + 2;

         for (HudData.ItemCount ic : l) {
            c.itemStack(ic.stack(), cx, cy);
            String n = String.valueOf(ic.count());
            c.text(n, cx + 19, cy + 5, ic.count() == 0 ? -38037 : -1, true);
            if (this.horizontal()) {
               cx += 18 + c.textWidth(n) + 6;
            } else {
               cy += ROW;
            }
         }
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"Arrows 64", "Pearls 16", "Golden apples 8"};
   }
}
