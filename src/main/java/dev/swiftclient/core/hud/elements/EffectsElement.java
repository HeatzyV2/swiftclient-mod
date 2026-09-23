package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import java.util.List;

public final class EffectsElement extends HudElement {
   public EffectsElement() {
      super("effects", "Potion effects");
   }

   @Override
   public String icon() {
      return "flask";
   }

   private String[] lines(HudData d) {
      List<HudData.Effect> fx = d.effects();
      if (fx != null && !fx.isEmpty()) {
         String[] out = new String[fx.size()];

         for (int i = 0; i < fx.size(); i++) {
            HudData.Effect e = fx.get(i);
            String lvl = e.amplifier() > 0 ? " " + roman(e.amplifier() + 1) : "";
            out[i] = e.name() + lvl + "  " + mmss(e.durationTicks());
         }

         return out;
      } else {
         return new String[0];
      }
   }

   @Override
   public int width(Canvas c, HudData d) {
      String[] l = this.lines(d);
      return l.length == 0 ? 0 : chipW(c, l);
   }

   @Override
   public int height(Canvas c) {
      return chipH(c, 1);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      String[] l = this.lines(d);
      if (l.length > 0) {
         chip(c, x, y, l);
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"Speed II  1:20", "Night Vision  4:59"};
   }

   static String roman(int n) {
      String[] r = new String[]{"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
      return n >= 0 && n < r.length ? r[n] : Integer.toString(n);
   }

   static String mmss(int ticks) {
      int s = Math.max(0, ticks / 20);
      return s / 60 + ":" + String.format("%02d", s % 60);
   }
}
