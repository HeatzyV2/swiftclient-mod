package dev.swiftclient.renderer;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Swift block drawing. The class keeps its old name because every screen and HUD element reaches the
 * screen through it, but nothing here is rounded any more: every shape is a hard-edged pixel block with a
 * stepped corner, a black ink outline and a solid "side" underneath. No shaders, no blur, no soft edges.
 */
public class RoundedButtons {
   /** Ink: the outline colour of every block. */
   public static final int INK = 0xFF000000;
   /** Flat button fill, hovered fill and disabled fill. */
   public static final int BG_NORMAL = 0xFF1A1F2B;
   public static final int BG_HOVER = 0xFF243044;
   public static final int BG_DISABLED = 0xFF12151C;
   /** The Swift blue, used for the hover frame. */
   public static final int BORDER_HOVER = 0xFF3B82F6;
   public static final int PANEL_BG = 0xE60B0E14;
   /** The solid side under raised blocks: the accent, darkened. */
   public static final int SIDE = 0xFF0B1A44;
   private static GuiGraphicsExtractor CTX;

   public static void bind(GuiGraphicsExtractor ctx) {
      CTX = ctx;
   }

   public static int currentBg(boolean hovered, boolean active) {
      return !active ? BG_DISABLED : (hovered ? BG_HOVER : BG_NORMAL);
   }

   /** Kept for callers that ask for a pill radius: blocks are square, so this is a small notch. */
   public static float pillRadius(int h) {
      return 2.0F;
   }

   /** Notch (corner cut) in pixels for a requested radius: 0, 1 or 2. */
   private static int notch(float radius, int w, int h) {
      int cap = Math.min(w, h) / 3;
      int n = radius < 2.0F ? 0 : (radius < 6.0F ? 1 : 2);
      return Math.max(0, Math.min(n, cap));
   }

   /** A filled block with stepped corners. */
   private static void block(int x, int y, int w, int h, int n, int color) {
      if (CTX == null || w <= 0 || h <= 0) {
         return;
      }
      if (n <= 0) {
         CTX.fill(x, y, x + w, y + h, color);
      } else if (n == 1) {
         CTX.fill(x + 1, y, x + w - 1, y + h, color);
         CTX.fill(x, y + 1, x + 1, y + h - 1, color);
         CTX.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
      } else {
         CTX.fill(x + 2, y, x + w - 2, y + 1, color);
         CTX.fill(x + 1, y + 1, x + w - 1, y + 2, color);
         CTX.fill(x, y + 2, x + w, y + h - 2, color);
         CTX.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, color);
         CTX.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
      }
   }

   private static void roundRect(float x, float y, float w, float h, float radius, int color) {
      int iw = Math.round(w);
      int ih = Math.round(h);
      block(Math.round(x), Math.round(y), iw, ih, notch(radius, iw, ih), color);
   }

   /** Vanilla-screen button: ink outline, flat fill, light top edge, dark bottom edge; blue frame on hover. */
   public static void drawButtonBackground(int x, int y, int w, int h, boolean hovered, boolean active) {
      if (CTX == null) {
         return;
      }
      int n = h >= 18 ? 1 : 0;
      block(x, y + 2, w, h, n, SIDE);
      block(x, y, w, h, n, INK);
      int frame = hovered && active ? BORDER_HOVER : INK;
      block(x, y, w, h, n, frame);
      block(x + 1, y + 1, w - 2, h - 2, 0, currentBg(hovered, active));
      if (active) {
         CTX.fill(x + 1, y + 1, x + w - 1, y + 2, hovered ? 0x40FFFFFF : 0x22FFFFFF);
         CTX.fill(x + 1, y + h - 3, x + w - 1, y + h - 1, 0x55000000);
      }
   }

   public static void drawSoftPanel(int x, int y, int w, int h) {
      drawCard(x, y, w, h, PANEL_BG, 0x33FFFFFF, 1, 4.0F);
   }

   public static void drawCircle(int x, int y, int size, boolean hovered, boolean active) {
      drawButtonBackground(x, y, size, size, hovered, active);
   }

   /**
    * The block every panel is made of: a hard drop side, a black ink outline, an optional coloured frame
    * (selection, hover) and a flat fill with a light top edge. [borderColor] is the frame; a faint one is
    * treated as no frame, so only the ink shows.
    */
   public static void drawCard(int x, int y, int w, int h, int bgColor, int borderColor, int borderThickness, float radius) {
      if (CTX == null || w <= 0 || h <= 0) {
         return;
      }
      int n = notch(radius, w, h);
      boolean opaque = (bgColor >>> 24) >= 0xC0;
      if (opaque && w >= 14 && h >= 14) {
         int side = h >= 28 ? 3 : 2;
         block(x, y + side, w, h, n, SIDE);
      }
      boolean framed = borderThickness > 0 && (borderColor >>> 24) >= 0x80;
      block(x, y, w, h, n, framed ? INK : INK);
      if (framed) {
         block(x + 1, y + 1, w - 2, h - 2, Math.max(0, n - 1), borderColor);
         block(x + 2, y + 2, w - 4, h - 4, Math.max(0, n - 2), bgColor);
      } else if ((bgColor >>> 24) > 0) {
         block(x + 1, y + 1, w - 2, h - 2, Math.max(0, n - 1), bgColor);
      }
      if (opaque && w > 8 && h > 8) {
         int inset = framed ? 2 : 1;
         CTX.fill(x + inset + n, y + inset, x + w - inset - n, y + inset + 1, 0x22FFFFFF);
      }
   }

   public static void drawRoundedRect(int x, int y, int w, int h, float radius, int color) {
      roundRect(x, y, w, h, radius, color);
   }
}
