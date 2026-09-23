package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;

public final class BiomeElement extends HudElement {
   public BiomeElement() {
      super("biome", "Biome");
   }

   private String text(HudData d) {
      String b = d == null ? "" : d.biome();
      if (b == null || b.isEmpty()) {
         b = "-";
      }

      return this.opt("label", true) ? "Biome  " + b : b;
   }

   @Override
   public int width(Canvas c, HudData d) {
      return chipW(c, new String[]{this.text(d)});
   }

   @Override
   public int height(Canvas c) {
      return chipH(c, 1);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      chip(c, x, y, new String[]{this.text(d)});
   }

   @Override
   public String[] previewLines() {
      return new String[]{"Biome  Dark Forest"};
   }
}
