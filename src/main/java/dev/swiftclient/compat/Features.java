package dev.swiftclient.compat;

import java.util.Set;

/**
 * What this build of the mod can do on its Minecraft version (see `featureSources` in build.gradle.kts).
 * A module whose feature is missing is not listed in the mods menu.
 */
public final class Features {
   /** Custom GPU passes (blur, post effects, rounded-rect shader, sky, aura...): the 26.2+ render API. */
   //? if gpu {
   public static final boolean GPU = true;
   //?} else
   /*public static final boolean GPU = false;*/
   /** GLFW window (Borderless Fullscreen); SDL3 from 26.3. */
   //? if glfw {
   public static final boolean GLFW = true;
   //?} else
   /*public static final boolean GLFW = false;*/
   /** Entity rendering of 1.21.9+ (SubmitNodeCollector): capes, wings, knives, dropped items... */
   //? if submit {
   public static final boolean SUBMIT = true;
   //?} else
   /*public static final boolean SUBMIT = false;*/
   /** GeckoLib pets. */
   //? if pets {
   public static final boolean PETS = true;
   //?} else
   /*public static final boolean PETS = false;*/

   private Features() {
   }

   /** Modules that exist in the shared code but do nothing on this version. */
   public static Set<String> unavailableModules() {
      Set<String> out = new java.util.HashSet<>();
      if (!GPU) {
         out.addAll(Set.of("gui_blur", "motion_blur", "saturation"));
      }
      if (!SUBMIT) {
         out.addAll(Set.of("item_physics", "items_2d", "uhc_overlay"));
      }
      if (!GLFW) {
         out.add("borderless");
      }
      return out;
   }
}
