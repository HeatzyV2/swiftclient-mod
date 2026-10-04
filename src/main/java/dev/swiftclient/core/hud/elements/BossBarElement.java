package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import java.util.List;

/** The server boss bars, restyled, movable and resizable (the vanilla ones are hidden while this is on). */
public final class BossBarElement extends HudElement {
   private static final int W = 182;
   private static final int BAR_H = 5;

   public BossBarElement() {
      super("bossbar", "Boss Bar");
   }

   @Override
   public String icon() {
      return "bossbar";
   }

   private List<HudData.BossBar> bars(HudData d) {
      List<HudData.BossBar> all = d.bossBars();
      int max = (int)Math.round(this.optValue("max", 3.0));
      return all.size() > max ? all.subList(0, max) : all;
   }

   private int rowH(Canvas c) {
      return (this.opt("title", true) ? c.lineHeight() + 2 : 0) + BAR_H + 4;
   }

   @Override
   public int width(Canvas c, HudData d) {
      return this.bars(d).isEmpty() ? 0 : W;
   }

   @Override
   public int height(Canvas c) {
      return this.rowH(c);
   }

   @Override
   public int height(Canvas c, HudData d) {
      return Math.max(1, this.bars(d).size()) * this.rowH(c);
   }

   /** Vanilla boss bar colours: pink, blue, red, green, yellow, purple, white. */
   static int barColor(int index) {
      return switch (index) {
         case 0 -> -1485889;
         case 1 -> -16734465;
         case 2 -> -1101505;
         case 3 -> -16713906;
         case 4 -> -2240;
         case 5 -> -7456513;
         default -> -1;
      };
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      boolean title = this.opt("title", true);
      int yy = y;

      for (HudData.BossBar b : this.bars(d)) {
         if (title) {
            int tw = c.richWidth(b.nameComponent());
            if (tw < 0) {
               tw = c.textWidth(b.name());
            }

            int tx = x + (W - tw) / 2;
            if (!c.richText(b.nameComponent(), tx, yy, -1, true)) {
               c.text(b.name(), tx, yy, -1, true);
            }

            yy += c.lineHeight() + 2;
         }

         int col = this.opt("own_color", false) ? this.optColor("color", -12877066) : barColor(b.color());
         dev.swiftclient.core.ui.Px.segBar(c, x, yy, W, BAR_H, Math.max(0.0F, Math.min(1.0F, b.progress())), col, 20);

         yy += BAR_H + 4;
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"Ender Dragon", "=============================="};
   }
}
