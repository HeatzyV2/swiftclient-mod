package dev.swiftclient.input;

import dev.swiftclient.compat.Mc;
import dev.swiftclient.core.platform.Platform;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import dev.swiftclient.compat.Input;

/**
 * Swift Client key bindings, listed and rebindable in Options > Controls > Key Binds (saved by vanilla
 * in options.txt). Toggle Sprint / Toggle Sneak use the vanilla Sprint and Sneak keys.
 */
public final class SwiftKeys {
   //? if >=1.21.9 {
   public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("swiftclient", "main"));
   //?} else
   /*public static final String CATEGORY = "key.category.swiftclient.main";*/
   public static KeyMapping MENU;
   public static KeyMapping ZOOM;
   public static KeyMapping FREELOOK;
   public static KeyMapping SNAPLOOK;
   public static KeyMapping STOPWATCH;
   public static KeyMapping STOPWATCH_RESET;
   public static KeyMapping WAYPOINT;
   public static KeyMapping RADIAL;
   public static KeyMapping CYCLE_PROFILE;

   private SwiftKeys() {
   }

   public static void register() {
      MENU = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swiftclient.modsmenu", InputConstants.KEY_RSHIFT, CATEGORY));
      ZOOM = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swiftclient.zoom", legacyKey("mod.zoom.key", InputConstants.KEY_C), CATEGORY));
      FREELOOK = KeyMappingHelper.registerKeyMapping(
         new KeyMapping("key.swiftclient.freelook", legacyKey("mod.freelook.key", InputConstants.KEY_LALT), CATEGORY)
      );
      SNAPLOOK = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swiftclient.snaplook", InputConstants.KEY_H, CATEGORY));
      STOPWATCH = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swiftclient.stopwatch", InputConstants.KEY_K, CATEGORY));
      STOPWATCH_RESET = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swiftclient.stopwatch_reset", Input.KEY_NONE, CATEGORY));
      WAYPOINT = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swiftclient.waypoint", InputConstants.KEY_J, CATEGORY));
      RADIAL = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swiftclient.radial", InputConstants.KEY_G, CATEGORY));
      CYCLE_PROFILE = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swiftclient.cycle_profile", Input.KEY_NONE, CATEGORY));
   }

   /**
    * Zoom and freelook keys used to be module settings stored in swiftclient.properties. Their saved value
    * becomes the default of the new key binding, so a custom key survives the update; from then on
    * vanilla stores it in options.txt.
    */
   private static int legacyKey(String configKey, int def) {
      try {
         String v = Platform.game().getConfig(configKey, null);
         if (v != null) {
            int code = (int)Double.parseDouble(v);
            return code >= 0 ? Input.fromCoreKey(code) : def;
         }
      } catch (Throwable ignored) {
      }

      return def;
   }

   /** Label for a module setting button, e.g. "C". */
   public static String label(KeyMapping key) {
      return key == null ? "?" : key.getTranslatedKeyMessage().getString();
   }

   /** Opens vanilla Key Binds on top of the current screen. */
   public static void openControls() {
      Minecraft mc = Minecraft.getInstance();
      mc.setScreenAndShow(new KeyBindsScreen(Mc.screen(), mc.options));
   }
}
