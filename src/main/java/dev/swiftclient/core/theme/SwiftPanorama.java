package dev.swiftclient.core.theme;

import java.util.function.Predicate;

public final class SwiftPanorama {
   private static Predicate<String> swapper;
   private static boolean persistedApplied;

   private SwiftPanorama() {
   }

   public static void bind(Predicate<String> s) {
      swapper = s;
      if (!persistedApplied) {
         ensurePersistedApplied();
      }
   }

   public static boolean apply(String location) {
      return swapper != null && location != null && !location.isBlank() ? swapper.test(location) : false;
   }

   public static void oublier() {
      persistedApplied = false;
   }

   public static void ensurePersistedApplied() {
      if (!persistedApplied) {
         try {
            String loc = ThemeManager.currentPanorama();
            if (loc == null || loc.isBlank()) {
               persistedApplied = true;
               return;
            }

            if (apply(loc)) {
               persistedApplied = true;
            }
         } catch (Throwable var1) {
         }
      }
   }
}
