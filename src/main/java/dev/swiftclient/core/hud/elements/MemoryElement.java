package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.HudStats;

public final class MemoryElement extends HudElement {
   public MemoryElement() {
      super("memory", "Memory");
   }

   private String text() {
      long used = HudStats.usedMemoryMb();
      StringBuilder sb = new StringBuilder();
      if (this.opt("percent", true)) {
         sb.append(HudStats.memoryPercent()).append("%  ");
      }

      sb.append(used);
      if (this.opt("max", true)) {
         sb.append('/').append(HudStats.maxMemoryMb());
      }

      sb.append(" MB");
      return sb.toString();
   }

   private int color() {
      int p = HudStats.memoryPercent();
      return p < 60 ? -9707413 : (p < 85 ? -865972 : -38037);
   }

   @Override
   public int width(Canvas c, HudData d) {
      return chipW(c, new String[]{this.text()});
   }

   @Override
   public int height(Canvas c) {
      return chipH(c, 1) + (this.opt("bar", true) ? 5 : 0);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      String t = this.text();
      boolean bar = this.opt("bar", true);
      int w = chipW(c, new String[]{t});
      int h = chipH(c, 1) + (bar ? 5 : 0);
      c.card(x, y, w, h, 1996488704, 587202559, 1, 4.0F);
      c.text(t, x + 6, y + 4, this.opt("colored", true) ? this.color() : -1, true);
      if (bar) {
         int bw = w - 12;
         int by = y + h - 6;
         c.fill(x + 6, by, x + 6 + bw, by + 2, 1442840575);
         int fill = Math.max(1, Math.round(bw * HudStats.memoryPercent() / 100.0F));
         c.fill(x + 6, by, x + 6 + fill, by + 2, this.color());
      }
   }

   @Override
   public boolean drawsWithoutData() {
      return true;
   }

   @Override
   public String[] previewLines() {
      return new String[]{"42%  1720/4096 MB"};
   }
}
