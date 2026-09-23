package dev.swiftclient.core.hud;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;

public final class Crosshair {
   private static final int CROSS = 0;
   private static final int DOT = 1;
   private static final int CIRCLE = 2;
   private static final int T_SHAPE = 3;
   private static final int CUSTOM = 4;

   private Crosshair() {
   }

   public static void draw(Canvas c, int cx, int cy) {
      Module m = ModuleManager.byId("crosshair");
      if (m != null) {
         int style = idx(m, "style", 0);
         int col = color(m, "color", -1);
         int len = val(m, "size", 4);
         int thick = Math.max(1, val(m, "thick", 1));
         int gap = val(m, "gap", 2);
         boolean dot = bool(m, "dot", false);
         boolean outline = bool(m, "outline", true);
         if (style == 4) {
            drawPixels(c, cx, cy, col, outline, Math.max(1, val(m, "pixsize", 2)));
         } else {
            if (outline) {
               shape(c, cx, cy, style, -1073741824, thick + 2, gap, len + 1, dot);
            }

            shape(c, cx, cy, style, col, thick, gap, len, dot);
         }
      }
   }

   private static void drawPixels(Canvas c, int cx, int cy, int col, boolean outline, int px) {
      int n = 16;
      int ox = cx - n * px / 2;
      int oy = cy - n * px / 2;
      if (outline) {
         for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
               if (CrosshairPixels.get(x, y)) {
                  c.fill(ox + x * px - 1, oy + y * px - 1, ox + x * px + px + 1, oy + y * px + px + 1, -1073741824);
               }
            }
         }
      }

      for (int y = 0; y < n; y++) {
         for (int xx = 0; xx < n; xx++) {
            if (CrosshairPixels.get(xx, y)) {
               c.fill(ox + xx * px, oy + y * px, ox + xx * px + px, oy + y * px + px, col);
            }
         }
      }
   }

   private static void shape(Canvas c, int cx, int cy, int style, int col, int t, int g, int L, boolean dot) {
      int x0 = cx - t / 2;
      int y0 = cy - t / 2;
      switch (style) {
         case 1:
            c.roundRect(cx - L / 2, cy - L / 2, L, L, L / 2.0F, col);
            break;
         case 2:
            c.card(cx - L, cy - L, 2 * L, 2 * L, 0, col, t, L);
            if (dot) {
               c.fill(x0, y0, x0 + t, y0 + t, col);
            }
            break;
         case 3:
            bar(c, x0, cy + g, t, L, col);
            bar(c, cx - g - L, y0, L, t, col);
            bar(c, cx + g, y0, L, t, col);
            if (dot) {
               c.fill(x0, y0, x0 + t, y0 + t, col);
            }
            break;
         default:
            bar(c, x0, cy - g - L, t, L, col);
            bar(c, x0, cy + g, t, L, col);
            bar(c, cx - g - L, y0, L, t, col);
            bar(c, cx + g, y0, L, t, col);
            if (dot) {
               c.fill(x0, y0, x0 + t, y0 + t, col);
            }
      }
   }

   private static void bar(Canvas c, int x, int y, int w, int h, int col) {
      c.fill(x, y, x + w, y + h, col);
   }

   private static int idx(Module m, String id, int def) {
      ModuleSetting s = m.setting(id);
      return s == null ? def : s.cycleIndex();
   }

   private static int color(Module m, String id, int def) {
      ModuleSetting s = m.setting(id);
      return s == null ? def : s.colorValue();
   }

   private static int val(Module m, String id, int def) {
      ModuleSetting s = m.setting(id);
      return s == null ? def : (int)Math.round(s.value());
   }

   private static boolean bool(Module m, String id, boolean def) {
      ModuleSetting s = m.setting(id);
      return s == null ? def : s.boolValue();
   }
}
