package dev.swiftclient.input;

import dev.swiftclient.modules.CoordSnapperModule;
import dev.swiftclient.modules.ElytraSwapModule;
import dev.swiftclient.visuals.WeaponInspectModule;
import net.minecraft.client.Minecraft;

/**
 * Keys that belong to a module (a {@code key} setting), read straight from the keyboard by
 * KeyboardHandlerMixin. Only while playing: never while a screen (chat, inventory, menu) is open.
 */
public final class ModuleKeys {
   private ModuleKeys() {
   }

   /** @return true when a module used the key, so its vanilla action must not run. */
   public static boolean dispatch(int key, int action) {
      try {
         if (Minecraft.getInstance().gui.screen() != null) {
            return false;
         }

         // Coord Snapper and Elytra Swap never consume the key; the inspect key replaces swap-hands.
         CoordSnapperModule.onKey(key, action);
         ElytraSwapModule.onKey(key, action);
         return WeaponInspectModule.onKey(key, action);
      } catch (Throwable t) {
         return false;
      }
   }
}
