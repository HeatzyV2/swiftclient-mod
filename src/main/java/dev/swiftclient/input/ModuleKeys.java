package dev.swiftclient.input;

import dev.swiftclient.compat.Mc;
import dev.swiftclient.modules.CoordSnapperModule;
import dev.swiftclient.modules.ElytraSwapModule;
//? if submit
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
      key = dev.swiftclient.compat.Input.toCoreKey(key);
      action = dev.swiftclient.compat.Input.toCoreAction(action);
      try {
         if (Mc.screen() != null) {
            return false;
         }

         // Coord Snapper and Elytra Swap never consume the key; the inspect key replaces swap-hands.
         CoordSnapperModule.onKey(key, action);
         ElytraSwapModule.onKey(key, action);
         //? if submit {
         return WeaponInspectModule.onKey(key, action);
         //?} else
         /*return false;*/
      } catch (Throwable t) {
         return false;
      }
   }
}
