package dev.swiftclient.core;

import dev.swiftclient.core.log.Log;
import org.slf4j.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

/**
 * Launcher "borderless" loses to options.txt {@code exclusiveFullscreen:true}.
 * Strip exclusive mode so MC uses borderless fullscreen; keep F11 from dumping a tiny window.
 */
public final class DisplayModeFix {
   private static final Logger LOG = Log.get("Display");
   private static boolean applied;
   private static Boolean lastFs;

   private DisplayModeFix() {
   }

   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.getWindow() == null || mc.options == null) {
         return;
      }

      if (!applied) {
         applied = true;
         Options opt = mc.options;
         // The exclusive fullscreen option only exists since 26.1.
         //? if >=26.1 {
         if (Boolean.TRUE.equals(opt.exclusiveFullscreen().get())) {
            opt.exclusiveFullscreen().set(false);
            opt.save();
            try {
               var win = mc.getWindow();
               if (dev.swiftclient.compat.Input.isFullscreen()) {
                  // Re-enter so GLFW drops exclusive for borderless
                  dev.swiftclient.compat.Input.toggleFullscreen();
                  dev.swiftclient.compat.Input.toggleFullscreen();
               }
            } catch (Throwable t) {
               LOG.warn("Bascule plein ecran sans bordure impossible", t);
            }
         }
         //?}
         lastFs = dev.swiftclient.compat.Input.isFullscreen();
         return;
      }

      boolean fs = dev.swiftclient.compat.Input.isFullscreen();
      if (lastFs != null && lastFs && !fs) {
         ensureWindowedSize(mc);
      }
      lastFs = fs;
   }

   public static void ensureWindowedSize(Minecraft mc) {
      try {
         var win = mc.getWindow();
         if (dev.swiftclient.compat.Input.isFullscreen()) {
            return;
         }
         int sw = Math.max(1280, win.getScreenWidth() * 4 / 5);
         int sh = Math.max(720, win.getScreenHeight() * 4 / 5);
         if (win.getWidth() < 1000 || win.getHeight() < 560) {
            int mx = Math.max(0, (win.getScreenWidth() - sw) / 2);
            int my = Math.max(0, (win.getScreenHeight() - sh) / 2);
            dev.swiftclient.compat.Input.setWindowBounds(mx, my, sw, sh);
         }
      } catch (Throwable ignored) {
      }
   }
}
