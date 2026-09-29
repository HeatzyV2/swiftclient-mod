package dev.swiftclient.core.theme;

import java.util.function.Predicate;

/** The title screen panorama. Bloomy is the Swift Client look: there is no other and it cannot be changed. */
public final class SwiftPanorama {
   public static final String BLOOMY = "swiftclient:textures/panorama/bloomy/panorama";
   private static Predicate<String> swapper;
   private static boolean applied;

   private SwiftPanorama() {
   }

   /**
    * Called while Minecraft is being built: resources (and so this mod's textures) are not loaded yet,
    * so the panorama is applied later, by {@link #applyWhenReady()}.
    */
   public static void bind(Predicate<String> s) {
      swapper = s;
   }

   /** Applies Bloomy once, the first time it is called after the resources finished loading. */
   public static void applyWhenReady() {
      if (!applied && swapper != null) {
         try {
            swapper.test(BLOOMY);
         } catch (Throwable ignored) {
         }

         // Tried with the resources loaded: if the texture is broken, do not retry every tick.
         applied = true;
      }
   }
}
