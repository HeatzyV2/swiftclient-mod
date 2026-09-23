package dev.swiftclient.core.cosmetics;

public final class CapeLayout {
   public static final int ELYTRA_X0 = 23;
   public static final int ELYTRA_X1 = 44;
   public static final int ELYTRA_Y0 = 1;
   public static final int ELYTRA_Y1 = 21;
   public static final int MIN_PIXELS = 40;
   public static final int MIN_ALPHA = 16;

   private CapeLayout() {
   }

   public static boolean assezDePixels(int pixelsVisibles) {
      return pixelsVisibles >= 40;
   }

   public static boolean zonePresente(int largeur, int hauteur) {
      return largeur >= 44 && hauteur >= 21;
   }
}
