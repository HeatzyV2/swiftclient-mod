package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import java.util.List;

/** The resource packs in use, the one on top first. */
public final class PackDisplayElement extends HudElement {
   public PackDisplayElement() {
      super("packs", "Pack Display");
   }

   @Override
   public String icon() {
      return "palette";
   }

   private String[] lines(HudData d) {
      List<String> packs = d.resourcePacks();
      int max = (int)Math.round(this.optValue("max", 3.0));
      int n = Math.min(max, packs.size());
      String[] out = new String[n];
      for (int i = 0; i < n; i++) {
         out[i] = packs.get(i);
      }

      return out;
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
   public int height(Canvas c, HudData d) {
      return chipH(c, Math.max(1, this.lines(d).length));
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
      return new String[]{"Bloomy 16x", "Default"};
   }
}
