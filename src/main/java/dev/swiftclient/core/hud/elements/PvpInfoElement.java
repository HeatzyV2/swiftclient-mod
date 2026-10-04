package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.platform.Tr;
import java.util.Locale;

/** Card about the player or mob you are fighting: head, name, health, armour, distance and your combo. */
public final class PvpInfoElement extends HudElement {
   private static final int W = 132;
   private static final int H = 38;

   public PvpInfoElement() {
      super("pvpinfo", "PvP Info");
   }

   @Override
   public String icon() {
      return "sword";
   }

   @Override
   public int width(Canvas c, HudData d) {
      return d.combat().target() == null ? 0 : W;
   }

   @Override
   public int height(Canvas c) {
      return H;
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      HudData.Target t = d.combat().target();
      if (t != null) {
         c.card(x, y, W, H, 0xAA0B0D12, 587202559, 1, 6.0F);
         int tx = x + 6;
         if (t.player() && t.uuid() != null) {
            c.playerHead(t.uuid(), x + 5, y + 5, 28);
            tx = x + 38;
         }

         int maxName = x + W - 6 - tx;
         String name = t.name();
         while (name.length() > 1 && c.textWidth(name) > maxName) {
            name = name.substring(0, name.length() - 1);
         }

         c.text(name, tx, y + 5, -1, true);
         float ratio = t.maxHealth() <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, t.health() / t.maxHealth()));
         int bw = x + W - 6 - tx;
         int col = ratio > 0.6F ? -9707413 : (ratio > 0.3F ? -865972 : -38037);
         dev.swiftclient.core.ui.Px.segBar(c, tx, y + 16, bw, 5, ratio, col, 10);
         String hp = String.format(Locale.ROOT, "%.1f", t.health() / 2.0F) + " ❤";
         String extra = this.opt("distance", true) ? String.format(Locale.ROOT, "%.1fm", t.distance()) : "";
         if (this.opt("armor", true) && t.armor() > 0) {
            extra = (extra.isEmpty() ? "" : extra + "  ") + t.armor() + " " + Tr.of("swift.hud.armor_points");
         }

         c.text(hp, tx, y + 24, col, true);
         if (!extra.isEmpty()) {
            c.text(extra, x + W - 6 - c.textWidth(extra), y + 24, -6644317, true);
         }
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"Notch", "9.5 ❤   3.1m"};
   }
}
