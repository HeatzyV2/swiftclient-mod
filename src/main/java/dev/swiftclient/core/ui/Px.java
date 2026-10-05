package dev.swiftclient.core.ui;

import dev.swiftclient.core.gfx.Canvas;

/**
 * Swift's interface language, shared by every screen and HUD element: smooth rounded panels with a soft
 * shadow, pill switches and bars, lamps, and the speed streak. Everything is anti-aliased and keeps the
 * transparency it is given: nothing is drawn under a translucent fill.
 */
public final class Px {
   public static final int INK = 0xFF000000;
   public static final int ACCENT = 0xFF3B82F6;
   public static final int ACCENT_HI = 0xFF6FA8FF;
   public static final int ACCENT_LO = 0xFF1B4FC4;
   public static final int SIDE = 0xFF0B1A44;
   public static final int BLOCK = 0xFF141925;
   public static final int BLOCK_HI = 0xFF1D2535;
   public static final int PANEL = 0xF2090C12;
   public static final int TEXT = 0xFFFFFFFF;
   public static final int DIM = 0xFF9AA6BA;
   public static final int FAINT = 0xFF5F6B80;
   public static final int OK = 0xFF3ECF8E;
   public static final int DANGER = 0xFFF2555A;

   private Px() {
   }

   /** A soft shadow under a rounded rectangle: a few translucent copies, growing and fading outward. */
   public static void shadow(Canvas c, int x, int y, int w, int h, float radius, int alpha) {
      for (int i = 1; i <= 3; i++) {
         int a = Math.max(1, alpha / (i + 1));
         c.roundRect(x - i, y - i + 3, w + 2 * i, h + 2 * i, radius + i, a << 24);
      }
   }

   /** Rounded panel. [frame] is a coloured edge (selection, hover) or 0 for none. */
   public static void block(Canvas c, int x, int y, int w, int h, int fill, int frame) {
      c.card(x, y, w, h, fill, frame, frame != 0 ? 1 : 0, 7.0F);
   }

   /** Flat inset area (rows, fields). */
   public static void well(Canvas c, int x, int y, int w, int h, int fill) {
      c.roundRect(x, y, w, h, 5.0F, fill & 0x00FFFFFF | 0xB0000000);
   }

   /** Button: accent when [primary], lighter on hover. */
   public static void button(Canvas c, int x, int y, int w, int h, String label, boolean hover, boolean primary) {
      float r = Math.min(h / 2.0F, 7.0F);
      if (primary) {
         shadow(c, x, y, w, h, r, 0x40);
      }
      int fill = primary ? (hover ? ACCENT_HI : ACCENT) : (hover ? BLOCK_HI : BLOCK);
      c.card(x, y, w, h, fill, primary ? 0 : (hover ? 0x66FFFFFF : 0x1AFFFFFF), primary ? 0 : 1, r);
      c.centeredText(label, x + w / 2, y + (h - 8) / 2, primary || hover ? TEXT : DIM, primary);
   }

   /** Pill switch, 26×12: a rounded track and a round knob. */
   public static void toggle(Canvas c, int x, int y, boolean on) {
      c.roundRect(x, y, 26, 12, 6.0F, on ? ACCENT : 0xFF2A3042);
      c.roundRect(on ? x + 16 : x + 2, y + 2, 8, 8, 4.0F, TEXT);
   }

   /** Status lamp: a round light with a soft halo when on. */
   public static void lamp(Canvas c, int x, int y, boolean on) {
      if (on) {
         c.roundRect(x - 2, y - 2, 10, 10, 5.0F, 0x383ECF8E);
      }
      c.roundRect(x, y, 6, 6, 3.0F, on ? OK : 0xFF2A3042);
   }

   /** Smooth progress bar: a dark rounded track and a rounded fill ([segs] is kept for old callers). */
   public static void segBar(Canvas c, int x, int y, int w, int h, float frac, int color, int segs) {
      float r = h / 2.0F;
      c.roundRect(x, y, w, h, r, 0xB0000000 | 0x1A1F2B);
      int fw = Math.round(w * Math.max(0.0F, Math.min(1.0F, frac)));
      if (fw > 0) {
         c.roundRect(x, y, Math.max(h, fw), h, r, color);
      }
   }

   /** The speed streak: three rounded bars that shrink, drawn under titles. */
   public static void streak(Canvas c, int x, int y) {
      c.roundRect(x, y, 28, 2, 1.0F, ACCENT);
      c.roundRect(x + 31, y, 12, 2, 1.0F, ACCENT & 0x00FFFFFF | 0x99000000);
      c.roundRect(x + 46, y, 4, 2, 1.0F, ACCENT & 0x00FFFFFF | 0x55000000);
   }

   /** Section title: capitals with a soft shadow, then the streak. */
   public static void title(Canvas c, String s, int x, int y) {
      c.text(s.toUpperCase(java.util.Locale.ROOT), x, y, TEXT, true);
      streak(c, x, y + 11);
   }

   /** Little tag (category, badge): rounded accent label. */
   public static void tag(Canvas c, int x, int y, String s, int color) {
      int w = c.textWidth(s) + 8;
      c.roundRect(x, y, w, 11, 4.0F, color);
      c.text(s, x + 4, y + 2, TEXT, false);
   }

   // ---- Zip, the mascot: a little speedster, 24x26 grid ----

   public static final int ZIP_W = 24;
   public static final int ZIP_H = 26;

   private static final String[] IDLE = {
      "......KKKKKKKKKKKK......",
      ".....KBBBBBBBBBBBBK.....",
      "....KBBBBBBYYBBBBBBK....",
      "...KBBLLLLYYBBBBBBBBK...",
      "..KBBLLLBYYYBBBBBBBBBK..",
      ".YKBBBBBBYYBBBBBBBBBBKY.",
      "YYYBDDDDDDDDDDDDDDDDBYYY",
      ".YKBBBKSSSSSSSSSSKBBBKY.",
      "Y.KBBKWWWWSSSWWWWSKBBK.Y",
      "..KBKSWWKWSSSWWKWSSKBK..",
      "..KBKSWKKWSSSWKKWSSKBK..",
      "..KBKSWKKWSSSWKKWSSKBK..",
      "..KBKPPSSKSSSKSSPPSKBK..",
      "...KBKSSSSKKKSSSSSKBK...",
      "....KBKSSSPPSSSSSKBK....",
      ".....KBKKKKKKKKKKBK.....",
      "....KBKKKKKKKKKKKKBK....",
      "....KBBKBBBYYBBBKBBK....",
      "....KKKKBBBYBBBBKKKK....",
      "....KSSKBBYBBBBBKSSK....",
      "....KSSKKBBBBBBKKSSK....",
      ".....KKKBKKKKKKBKKK.....",
      ".......KKKKKKKKKK.......",
      "......KYYYYKKYYYYK......",
      "......KYYYYKKYYYYK......",
      ".......KKKK..KKKK.......",
   };
   private static final String[] RUN_A = {
      ".......KKKKKKKKKKKK.....",
      "......KBBBBBBBBBBBBK....",
      ".....KBBBBBBYYBBBBBBK...",
      "....KBBLLLLYYBBBBBBBBK..",
      "...KBBLLLBYYYBBBBBBBBBK.",
      "..YKBBBBBBYYBBBBBBBBBBKY",
      ".YYYBDDDDDDDDDDDDDDDDBYY",
      "..YKBBBKSSSSSSSSSSKBBBKY",
      ".Y.KBBKWWWWSSSWWWWSKBBK.",
      "...KBKSWWWKSSSWWWKSSKBK.",
      "...KBKSWWKKSSSWWKKSSKBK.",
      "...KBKSWWKKSSSWWKKSSKBK.",
      "...KBKPPSSKSSSKSSPPSKBK.",
      "....KBKSSSSKKKSSSSSKBK..",
      ".....KBKSSSPPSSSSSKBKK..",
      "KKKKKKKBKKKKKKKKKKBKK...",
      "SSSKBBBKKKKKKKKKKKKK....",
      "SSSKBBBKKBBBYYBBBKBK....",
      "KKKKKKK.KBBBYBBBBKK.....",
      ".......KKBBYBBBBBK......",
      "......KBBKBBBBBBKKK.....",
      "....KKKKBBKKKKKKBKKKK...",
      "..KKKKBBKBK..KKBKYYYYK..",
      ".KYYYYKBKK.....KKYYYYK..",
      ".KYYYYKK.........KKKK...",
      "..KKKK..................",
   };
   private static final String[] RUN_B = {
      ".......KKKKKKKKKKKK.....",
      "......KBBBBBBBBBBBBK....",
      ".....KBBBBBBYYBBBBBBK...",
      "....KBBLLLLYYBBBBBBBBK..",
      "...KBBLLLBYYYBBBBBBBBBK.",
      "..YKBBBBBBYYBBBBBBBBBBKY",
      ".YYYBDDDDDDDDDDDDDDDDBYY",
      "..YKBBBKSSSSSSSSSSKBBBKY",
      ".Y.KBBKWWWWSSSWWWWSKBBK.",
      "...KBKSWWWKSSSWWWKSSKBK.",
      "...KBKSWWKKSSSWWKKSSKBK.",
      "...KBKSWWKKSSSWWKKSSKBK.",
      "...KBKPPSSKSSSKSSPPSKBK.",
      "....KBKSSSSKKKSSSSSKBK..",
      "...KKKBKSSSPPSSSSSKBKKK.",
      "..KBBKKBKKKKKKKKKKBKSSSK",
      "..KBBK.KKKKKKKKKKKKKSSSK",
      "..KKKK..KBBBYYBBBKBBKKK.",
      ".KSSSK..KBBBYBBBBKKKKK..",
      ".KSSSK.KKBBYBBBBBK......",
      "..KKK..KBKBBBBBBKK......",
      ".......KBKKKKKKKBK......",
      "........KKBKYKBBBK......",
      "..........KKYKKKKK......",
      "............KKYYYYK.....",
      "..............KKKK......",
   };

   /**
    * Draws Zip at [scale] px per sprite pixel. [t] is a running clock in ms; with [run] the two stride
    * frames alternate and speed lines trail behind him.
    */
   public static void zip(Canvas c, int x, int y, int scale, boolean run, long t) {
      String[] body = run ? ((t / 140L) % 2L == 0L ? RUN_A : RUN_B) : IDLE;
      int bob = scale > 1 ? (int)((t / (run ? 140L : 700L)) % 2L) : 0;
      if (run) {
         int shift = (int)((t / 90L) % 3L);
         c.fill(x - (11 - shift) * scale, y + 8 * scale, x - 3 * scale, y + 9 * scale, ACCENT);
         c.fill(x - (8 - shift) * scale, y + 12 * scale, x, y + 13 * scale, ACCENT_HI);
         c.fill(x - (11 - shift) * scale, y + 16 * scale, x - 4 * scale, y + 17 * scale, ACCENT);
      }
      sprite(c, body, x, y + bob * scale, scale);
   }

   private static void sprite(Canvas c, String[] rows, int x, int y, int s) {
      for (int r = 0; r < rows.length; r++) {
         String row = rows[r];
         int cx = 0;
         while (cx < row.length()) {
            char k = row.charAt(cx);
            int col = color(k);
            if (col == 0) {
               cx++;
               continue;
            }
            int run = 1;
            while (cx + run < row.length() && row.charAt(cx + run) == k) {
               run++;
            }
            c.fill(x + cx * s, y + r * s, x + (cx + run) * s, y + (r + 1) * s, col);
            cx += run;
         }
      }
   }

   private static int color(char k) {
      return switch (k) {
         case 'K' -> INK;
         case 'B' -> ACCENT;
         case 'D' -> ACCENT_LO;
         case 'L' -> 0xFF9CC4FF;
         case 'S' -> 0xFFFFD6B0;
         case 'W' -> 0xFFFFFFFF;
         case 'Y' -> 0xFFFFD23F;
         case 'P' -> 0xFFFF7896;
         case 'O' -> 0xFFFF9A1F;
         default -> 0;
      };
   }
}
