package dev.swiftclient.core.theme;

import java.util.function.Predicate;

public final class SwiftPanorama {
   private static Predicate<String> swapper;
   private static boolean persistedApplied;

   private SwiftPanorama() {
   }

   /**
    * Called while Minecraft is being built: resources (and so this mod's textures) are not loaded yet,
    * so the saved theme is applied later, by {@link #applyWhenReady()}.
    */
   public static void bind(Predicate<String> s) {
      swapper = s;
   }

   /** Applies the saved theme once, the first time it is called after the resources finished loading. */
   public static void applyWhenReady() {
      if (!persistedApplied && swapper != null) {
         ensurePersistedApplied();
         // Tried with the resources loaded: if the theme is broken, do not retry every tick.
         persistedApplied = true;
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
