package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import java.util.ArrayList;
import java.util.List;

public final class ArmorElement extends HudElement {
   private static final int ICON = 16;
   private static final int GAP = 2;
   private static final int PAD = 3;
   private static final int BAR_H = 2;

   public ArmorElement() {
      super("armor", "Armor");
   }

   private boolean horizontal() {
      return this.optCycle("layout", 0) == 0;
   }

   private boolean showHeld() {
      return this.opt("held", true);
   }

   private boolean background() {
      return this.opt("background", true);
   }

   private int durabilityMode() {
      return this.optCycle("durability", 1);
   }

   private List<HudData.Armor> pieces(HudData d) {
      List<HudData.Armor> out = new ArrayList<>(5);
      if (d == null) {
         return out;
      } else {
         List<HudData.Armor> a = d.armor();
         if (a != null) {
            out.addAll(a);
         }

         if (this.showHeld()) {
            HudData.Armor held = d.heldItem();
            if (held != null) {
               out.add(held);
            }
         }

         return out;
      }
   }

   private String durabilityText(HudData.Armor it) {
      if (it.maxDamage() <= 0) {
         return "";
      } else {
         return switch (this.durabilityMode()) {
            case 2 -> Math.round(it.durability() * 100.0F) + "%";
            case 3 -> String.valueOf(it.maxDamage() - it.damage());
            default -> "";
         };
      }
   }

   private static int durabilityColor(float ratio) {
      float h = Math.max(0.0F, ratio) / 3.0F;
      int b = 0;
      float f = h * 6.0F;
      int r;
      int g;
      if (f < 1.0F) {
         r = 255;
         g = Math.round(255.0F * f);
      } else if (f < 2.0F) {
         r = Math.round(255.0F * (2.0F - f));
         g = 255;
      } else {
         r = 0;
         g = 255;
      }

      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   private int textColumn(Canvas c, List<HudData.Armor> l) {
      int w = 0;

      for (HudData.Armor it : l) {
         w = Math.max(w, c.textWidth(this.durabilityText(it)));
      }

      return w;
   }

   private int rowHeight(Canvas c) {
      if (!this.horizontal()) {
         return Math.max(16, c.lineHeight());
      } else {
         return switch (this.durabilityMode()) {
            case 0 -> 16;
            case 1 -> 19;
            default -> 17 + c.lineHeight();
         };
      }
   }

   @Override
   public int width(Canvas c, HudData d) {
      List<HudData.Armor> l = this.pieces(d);
      if (l.isEmpty()) {
         return 0;
      } else {
         int inner;
         if (this.horizontal()) {
            int cell = 16;
            if (this.durabilityMode() >= 2) {
               cell = Math.max(16, this.textColumn(c, l));
            }

            inner = l.size() * cell + (l.size() - 1) * 2;
         } else {
            int txt = this.durabilityMode() == 1 ? 26 : this.textColumn(c, l);
            inner = 16 + (txt > 0 ? 2 + txt : 0);
         }

         return inner + (this.background() ? 6 : 0);
      }
   }

   @Override
   public int height(Canvas c) {
      return this.rowHeight(c) + (this.background() ? 6 : 0);
   }

   @Override
   public int height(Canvas c, HudData d) {
      List<HudData.Armor> l = this.pieces(d);
      if (l.isEmpty()) {
         return 0;
      } else {
         int inner = this.horizontal() ? this.rowHeight(c) : l.size() * this.rowHeight(c) + (l.size() - 1) * 2;
         return inner + (this.background() ? 6 : 0);
      }
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      List<HudData.Armor> l = this.pieces(d);
      if (!l.isEmpty()) {
         int w = this.width(c, d);
         int h = this.height(c, d);
         if (this.background()) {
            c.card(x, y, w, h, 1996488704, 587202559, 1, 4.0F);
         }

         int ox = x + (this.background() ? 3 : 0);
         int oy = y + (this.background() ? 3 : 0);
         if (this.horizontal()) {
            this.drawRow(c, l, ox, oy);
         } else {
            this.drawColumn(c, l, ox, oy);
         }
      }
   }

   private void drawRow(Canvas c, List<HudData.Armor> l, int x, int y) {
      int cell = this.durabilityMode() >= 2 ? Math.max(16, this.textColumn(c, l)) : 16;

      for (HudData.Armor it : l) {
         int cx = x + (cell - 16) / 2;
         if (!this.icon(c, it, cx, y)) {
            c.text(court(it.name()), x, y + (16 - c.lineHeight()) / 2, -1, true);
         }

         switch (this.durabilityMode()) {
            case 1:
               this.bar(c, it, cx, y + 16 + 1, 16);
               break;
            case 2:
            case 3:
               String s = this.durabilityText(it);
               if (!s.isEmpty()) {
                  c.centeredText(s, x + cell / 2, y + 16 + 1, durabilityColor(it.durability()), true);
               }
         }

         x += cell + 2;
      }
   }

   private void drawColumn(Canvas c, List<HudData.Armor> l, int x, int y) {
      int row = this.rowHeight(c);
      int txt = this.durabilityMode() == 1 ? 26 : this.textColumn(c, l);

      for (HudData.Armor it : l) {
         int iy = y + (row - 16) / 2;
         if (!this.icon(c, it, x, iy)) {
            c.text(court(it.name()), x, y + (row - c.lineHeight()) / 2, -1, true);
         }

         if (txt > 0) {
            int tx = x + 16 + 2;
            if (this.durabilityMode() == 1) {
               this.bar(c, it, tx, y + (row - 2) / 2, txt);
            } else {
               String s = this.durabilityText(it);
               if (!s.isEmpty()) {
                  c.text(s, tx, y + (row - c.lineHeight()) / 2, durabilityColor(it.durability()), true);
               }
            }
         }

         y += row + 2;
      }
   }

   private boolean icon(Canvas c, HudData.Armor it, int x, int y) {
      return it.stack() != null && c.itemStack(it.stack(), x, y);
   }

   private void bar(Canvas c, HudData.Armor it, int x, int y, int w) {
      if (it.maxDamage() > 0) {
         c.fill(x, y, x + w, y + 2, -16777216);
         int fill = Math.max(0, Math.round(w * it.durability()));
         if (fill > 0) {
            c.fill(x, y, x + fill, y + 2, durabilityColor(it.durability()));
         }
      }
   }

   private static String court(String nom) {
      if (nom == null) {
         return "";
      } else {
         int sp = nom.lastIndexOf(32);
         return sp > 0 && sp < nom.length() - 1 ? nom.substring(sp + 1) : nom;
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"[] [] [] []  []"};
   }
}
