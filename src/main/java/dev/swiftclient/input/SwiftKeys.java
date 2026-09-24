package dev.swiftclient.input;

import dev.swiftclient.core.platform.Platform;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Swift Client key bindings, listed and rebindable in Options > Controls > Key Binds (saved by vanilla
 * in options.txt). Toggle Sprint / Toggle Sneak use the vanilla Sprint and Sneak keys.
 */
public final class SwiftKeys {
   public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("swiftclient", "main"));
   public static KeyMapping MENU;
   public static KeyMapping ZOOM;
   public static KeyMapping FREELOOK;

   private SwiftKeys() {
   }

   public static void register() {
      MENU = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swiftclient.modsmenu", GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
      ZOOM = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swiftclient.zoom", legacyKey("mod.zoom.key", GLFW.GLFW_KEY_C), CATEGORY));
      FREELOOK = KeyMappingHelper.registerKeyMapping(
         new KeyMapping("key.swiftclient.freelook", legacyKey("mod.freelook.key", GLFW.GLFW_KEY_LEFT_ALT), CATEGORY)
      );
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
            return code >= 0 ? code : def;
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
      mc.setScreenAndShow(new KeyBindsScreen(mc.gui.screen(), mc.options));
   }
}
