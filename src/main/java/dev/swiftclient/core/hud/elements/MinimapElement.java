package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;

/** Top-down map of the terrain around you, with other players and your waypoints. */
public final class MinimapElement extends HudElement {
   public MinimapElement() {
      super("minimap", "Minimap");
   }

   @Override
   public String icon() {
      return "map";
   }

   private int size() {
      return (int)Math.round(this.optValue("size", 90.0));
   }

   private int radius() {
      return (int)Math.round(this.optValue("radius", 48.0));
   }

   @Override
   public int width(Canvas c, HudData d) {
      return this.size() + 4;
   }

   @Override
   public int height(Canvas c) {
      return this.size() + 4 + (this.opt("coords", true) ? c.lineHeight() + 3 : 0);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      int s = this.size();
      boolean rotate = this.opt("rotate", true);
      c.card(x, y, s + 4, s + 4, 0xCC0B0D12, 587202559, 1, 5.0F);
      HudData.Minimap map = d.minimap(this.radius(), rotate);
      if (map != null) {
         int tex = map.size();
         c.textureRegion(map.texture(), x + 2, y + 2, s, s, 0, 0, tex, tex, tex, tex);
         float scale = (float)s / tex;
         int cx = x + 2 + s / 2;
         int cy = y + 2 + s / 2;
         c.pushScissor(x + 2, y + 2, s, s);

         for (HudData.MapMarker m : map.markers()) {
            if (m.waypoint() && !this.opt("waypoints", true) || !m.waypoint() && !this.opt("players", true)) {
               continue;
            }

            int mx = cx + Math.round(m.dx() * scale);
            int my = cy + Math.round(m.dy() * scale);
            mx = Math.max(x + 3, Math.min(x + s, mx));
            my = Math.max(y + 3, Math.min(y + s, my));
            int r = m.waypoint() ? 2 : 1;
            c.fill(mx - r - 1, my - r - 1, mx + r + 2, my + r + 2, 0xFF000000);
            c.fill(mx - r, my - r, mx + r + 1, my + r + 1, m.color());
         }

         c.popScissor();
         // You: a small arrow pointing up (rotating map) or a dot (north-up map)
         int acc = this.optColor("color", -1);
         if (rotate) {
            c.fill(cx, cy - 3, cx + 1, cy - 2, acc);
            c.fill(cx - 1, cy - 2, cx + 2, cy, acc);
            c.fill(cx - 2, cy, cx + 3, cy + 2, acc);
         } else {
            c.fill(cx - 1, cy - 1, cx + 2, cy + 2, acc);
         }

         if (!rotate) {
            c.centeredText("N", cx, y + 3, -38037, true);
         }
      }

      if (this.opt("coords", true)) {
         String t = (long)Math.floor(d.x()) + ", " + (long)Math.floor(d.y()) + ", " + (long)Math.floor(d.z());
         c.centeredText(t, x + (s + 4) / 2, y + s + 6, -1, true);
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"   Minimap   ", "", "", "", "", ""};
   }
}
