package dev.swiftclient.core.gfx;

import java.lang.reflect.Method;

public final class Shaders {
   private static final String[] CLASSES = new String[]{"net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.api.v0.IrisApi"};
   private static boolean cherche = false;
   private static Object api;
   private static Method enUsage;

   private Shaders() {
   }

   public static boolean actifs() {
      if (!cherche) {
         cherche = true;

         for (String nom : CLASSES) {
            try {
               Class<?> c = Class.forName(nom);
               api = c.getMethod("getInstance").invoke(null);
               enUsage = c.getMethod("isShaderPackInUse");
               break;
            } catch (Throwable var6) {
            }
         }
      }

      if (api != null && enUsage != null) {
         try {
            return Boolean.TRUE.equals(enUsage.invoke(api));
         } catch (Throwable var5) {
            api = null;
            enUsage = null;
            return false;
         }
      } else {
         return false;
      }
   }

   public static double gammaFullBright(double niveau) {
      double voulu = niveau <= 0.0 ? 100.0 : niveau;
      return actifs() ? Math.min(voulu, 1.0) : voulu;
   }
}
