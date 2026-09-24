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
         if (Boolean.TRUE.equals(opt.exclusiveFullscreen().get())) {
            opt.exclusiveFullscreen().set(false);
            opt.save();
            try {
               var win = mc.getWindow();
               if (win.isFullscreen()) {
                  // Re-enter so GLFW drops exclusive for borderless
                  win.toggleFullScreen();
                  win.toggleFullScreen();
               }
            } catch (Throwable t) {
               LOG.warn("Bascule plein ecran sans bordure impossible", t);
            }
         }
         lastFs = mc.getWindow().isFullscreen();
         return;
      }

      boolean fs = mc.getWindow().isFullscreen();
      if (lastFs != null && lastFs && !fs) {
         ensureWindowedSize(mc);
      }
      lastFs = fs;
   }

   public static void ensureWindowedSize(Minecraft mc) {
      try {
         var win = mc.getWindow();
         if (win.isFullscreen()) {
            return;
         }
         int sw = Math.max(1280, win.getScreenWidth() * 4 / 5);
         int sh = Math.max(720, win.getScreenHeight() * 4 / 5);
         if (win.getWidth() < 1000 || win.getHeight() < 560) {
            long handle = win.handle();
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(handle, sw, sh);
            int mx = Math.max(0, (win.getScreenWidth() - sw) / 2);
            int my = Math.max(0, (win.getScreenHeight() - sh) / 2);
            org.lwjgl.glfw.GLFW.glfwSetWindowPos(handle, mx, my);
         }
      } catch (Throwable ignored) {
      }
   }
}
