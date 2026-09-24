package dev.swiftclient.core.hud;

import dev.swiftclient.core.platform.Platform;
import java.util.Arrays;

public final class CrosshairPixels {
   public static final int N = 16;
   private static final String KEY = "mod.crosshair.pixels";
   private static final boolean[] cells = new boolean[256];
   private static boolean loaded = false;

   private CrosshairPixels() {
   }

   private static void ensure() {
      if (!loaded) {
         loaded = true;

         try {
            String hex = Platform.game().getConfig("mod.crosshair.pixels", "");
            if (hex == null) {
               return;
            }

            for (int i = 0; i < hex.length() && i < 64; i++) {
               int v = Character.digit(hex.charAt(i), 16);
               if (v >= 0) {
                  for (int b = 0; b < 4; b++) {
                     int idx = i * 4 + b;
                     if (idx < 256 && (v & 1 << b) != 0) {
                        cells[idx] = true;
                     }
                  }
               }
            }
         } catch (Throwable ignored) {
         }
      }
   }

   private static void save() {
      try {
         StringBuilder sb = new StringBuilder(64);

         for (int i = 0; i < 64; i++) {
            int v = 0;

            for (int b = 0; b < 4; b++) {
               int idx = i * 4 + b;
               if (idx < 256 && cells[idx]) {
                  v |= 1 << b;
               }
            }

            sb.append(Integer.toHexString(v));
         }

         Platform.game().setConfig("mod.crosshair.pixels", sb.toString());
      } catch (Throwable ignored) {
      }
   }

   public static boolean get(int x, int y) {
      ensure();
      return x >= 0 && x < 16 && y >= 0 && y < 16 && cells[y * 16 + x];
   }

   public static void set(int x, int y, boolean on) {
      ensure();
      if (x >= 0 && x < 16 && y >= 0 && y < 16) {
         if (cells[y * 16 + x] != on) {
            cells[y * 16 + x] = on;
            save();
         }
      }
   }

   public static void clear() {
      ensure();
      Arrays.fill(cells, false);
      save();
   }

   public static boolean isEmpty() {
      ensure();

      for (boolean b : cells) {
         if (b) {
            return false;
         }
      }

      return true;
   }
}
