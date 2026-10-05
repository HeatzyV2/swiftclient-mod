package dev.swiftclient.core.ui;

import dev.swiftclient.core.gfx.Canvas;

/**
 * Swift's pixel-block language, shared by every screen and HUD element: raised blocks with an ink outline,
 * square switches, lamps, segmented bars and the speed streak. Same family as the launcher.
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

   /** Raised block: ink outline, flat fill, solid side. [frame] is a colour frame (selection) or 0. */
   public static void block(Canvas c, int x, int y, int w, int h, int fill, int frame) {
      c.card(x, y, w, h, fill, frame, frame != 0 ? 1 : 0, 4.0F);
   }

   /** Flat inset block with no side (rows, fields). */
   public static void well(Canvas c, int x, int y, int w, int h, int fill) {
      c.card(x, y, w, h, fill & 0x00FFFFFF | 0xB0000000, 0, 0, 2.0F);
   }

   /** Chunky button: accent when [primary], raised, white frame on hover. */
   public static void button(Canvas c, int x, int y, int w, int h, String label, boolean hover, boolean primary) {
      int fill = primary ? (hover ? ACCENT_HI : ACCENT) : (hover ? BLOCK_HI : BLOCK);
      c.card(x, y, w, h, fill, hover ? TEXT : 0, hover ? 1 : 0, 4.0F);
      if (primary) {
         c.fill(x + 2, y + h - 4, x + w - 2, y + h - 2, 0x40000000);
      }
      int ty = y + (h - 8) / 2;
      c.centeredText(label, x + w / 2, hover ? ty - 1 : ty, primary || hover ? TEXT : DIM, true);
   }

   /** Square switch: 26×12, a notched track and a square knob that snaps. */
   public static void toggle(Canvas c, int x, int y, boolean on) {
      int w = 26;
      int h = 12;
      c.card(x, y, w, h, on ? ACCENT : 0xFF222836, 0, 0, 2.0F);
      int k = h - 4;
      int kx = on ? x + w - k - 2 : x + 2;
      c.fill(kx, y + 2, kx + k, y + 2 + k, INK);
      c.fill(kx + 1, y + 3, kx + k - 1, y + 1 + k, TEXT);
      if (on) {
         c.fill(x + 3, y + 5, x + 5, y + 7, 0x66FFFFFF);
      }
   }

   /** Status lamp: lit square when on, dark when off. */
   public static void lamp(Canvas c, int x, int y, boolean on) {
      c.fill(x, y, x + 5, y + 5, INK);
      c.fill(x + 1, y + 1, x + 4, y + 4, on ? OK : 0xFF2A3042);
      if (on) {
         c.fill(x + 1, y + 1, x + 2, y + 2, 0xCCFFFFFF);
      }
   }

   /** Segmented bar: [segs] cells filled to [frac]. */
   public static void segBar(Canvas c, int x, int y, int w, int h, float frac, int color, int segs) {
      segs = Math.max(2, segs);
      c.fill(x - 1, y - 1, x + w + 1, y + h + 1, INK);
      c.fill(x, y, x + w, y + h, 0xFF1A1F2B);
      int gap = 1;
      int cell = Math.max(1, (w - gap * (segs - 1)) / segs);
      int lit = Math.round(Math.max(0F, Math.min(1F, frac)) * segs);
      for (int i = 0; i < lit; i++) {
         int cx = x + i * (cell + gap);
         c.fill(cx, y, cx + cell, y + h, color);
         c.fill(cx, y, cx + cell, y + 1, 0x55FFFFFF);
      }
   }

   /** The speed streak: three bars that shrink, drawn under titles. */
   public static void streak(Canvas c, int x, int y) {
      c.fill(x, y, x + 28, y + 2, ACCENT);
      c.fill(x + 31, y, x + 43, y + 2, ACCENT & 0x00FFFFFF | 0x99000000);
      c.fill(x + 46, y, x + 50, y + 2, ACCENT & 0x00FFFFFF | 0x55000000);
   }

   /** Section title in the Swift sign style: pixel text with a flat shadow, then the streak. */
   public static void title(Canvas c, String s, int x, int y) {
      c.text(s.toUpperCase(java.util.Locale.ROOT), x, y, TEXT, true);
      streak(c, x, y + 11);
   }

   /** Little tag (category, badge): flat accent block. */
   public static void tag(Canvas c, int x, int y, String s, int color) {
      int w = c.textWidth(s) + 8;
      c.card(x, y, w, 11, color, 0, 0, 2.0F);
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
