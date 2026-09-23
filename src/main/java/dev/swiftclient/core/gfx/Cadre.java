package dev.swiftclient.core.gfx;

public final class Cadre {
   private Cadre() {
   }

   public static void gauche(Canvas c, int x, int y, int w, int h, int argb, float r) {
      if (w > 0 && h > 0) {
         int d = debord(r);
         c.pushScissor(x, y, w, h);
         c.roundRect(x, y, w + d, h, r, argb);
         c.popScissor();
      }
   }

   public static void bas(Canvas c, int x, int y, int w, int h, int argb, float r) {
      if (w > 0 && h > 0) {
         int d = debord(r);
         c.pushScissor(x, y, w, h);
         c.roundRect(x, y - d, w, h + d, r, argb);
         c.popScissor();
      }
   }

   public static void haut(Canvas c, int x, int y, int w, int h, int argb, float r) {
      if (w > 0 && h > 0) {
         int d = debord(r);
         c.pushScissor(x, y, w, h);
         c.roundRect(x, y, w, h + d, r, argb);
         c.popScissor();
      }
   }

   private static int debord(float r) {
      return Math.round(r) + 1;
   }
}
