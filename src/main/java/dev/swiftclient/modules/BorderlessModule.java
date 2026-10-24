package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import net.minecraft.client.Minecraft;

/**
 * Fullscreen (F11) as a borderless window covering the monitor: instant alt-tab, and Discord, OBS or
 * Windows notifications can show on top of the game.
 */
@ConsumedBy({"BorderlessWindowMixin", "HideInventoryModule"})
public final class BorderlessModule extends Module {
   public BorderlessModule() {
      super("borderless", "Borderless Fullscreen", "Fullscreen becomes a borderless window: instant alt-tab, and overlays (Discord, OBS) stay visible.", "Interface", "expand", false);
   }

   /** Implemented on the game window by BorderlessWindowMixin. */
   public interface Switchable {
      boolean swiftclient$isBorderless();

      void swiftclient$reapplyMode();
   }

   /** This module, or Hide Inventory's Stream only (its overlay cannot show over exclusive fullscreen). */
   public static boolean wanted() {
      return ModuleManager.isLoaded() && (ModuleManager.active("borderless") || HideInventoryModule.wantsBorderless());
   }

   @Override
   protected void onEnable() {
      reapply();
   }

   @Override
   protected void onDisable() {
      reapply();
   }

   /** A fullscreen window switches to the new mode right away instead of at the next F11. */
   public static void reapply() {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.getWindow() != null && dev.swiftclient.compat.Input.isFullscreen() && (Object)mc.getWindow() instanceof Switchable s && s.swiftclient$isBorderless() != wanted()) {
            s.swiftclient$reapplyMode();
         }
      } catch (Throwable ignored) {
         // No window yet (tests, early start): the mode is picked when the window goes fullscreen.
      }
   }
}
