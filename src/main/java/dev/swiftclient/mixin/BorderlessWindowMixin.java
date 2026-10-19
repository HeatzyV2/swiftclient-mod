package dev.swiftclient.mixin;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.MonitorManager;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import dev.swiftclient.modules.BorderlessModule;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Borderless Fullscreen: "fullscreen" becomes a borderless window covering the monitor, so alt-tab is
 * instant and overlays (Discord, OBS, notifications) stay on top.
 */
@Mixin({Window.class})
public abstract class BorderlessWindowMixin implements BorderlessModule.Switchable {
   @Shadow
   @Final
   private long handle;
   @Shadow
   @Final
   private MonitorManager monitorManager;
   @Shadow
   private boolean fullscreen;
   @Shadow
   private boolean actuallyFullscreen;
   @Shadow
   private int windowedX;
   @Shadow
   private int windowedY;
   @Shadow
   private int windowedWidth;
   @Shadow
   private int windowedHeight;
   @Shadow
   private int x;
   @Shadow
   private int y;
   @Shadow
   private int width;
   @Shadow
   private int height;
   @Unique
   private boolean swiftclient$borderless;

   @Inject(
      method = {"setMode"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$setMode(CallbackInfo ci) {
      if (this.fullscreen && BorderlessModule.wanted()) {
         Monitor monitor = this.monitorManager.findBestMonitor((Window)(Object)this);
         if (monitor != null) {
            boolean exclusive = GLFW.glfwGetWindowMonitor(this.handle) != 0L;
            if (!exclusive && !this.swiftclient$borderless) {
               this.windowedX = this.x;
               this.windowedY = this.y;
               this.windowedWidth = this.width;
               this.windowedHeight = this.height;
            }

            VideoMode mode = monitor.currentMode();
            this.x = monitor.x();
            this.y = monitor.y();
            this.width = mode.getWidth();
            this.height = mode.getHeight();
            GLFW.glfwSetWindowAttrib(this.handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
            GLFW.glfwSetWindowMonitor(this.handle, 0L, this.x, this.y, this.width, this.height, -1);
            this.swiftclient$borderless = true;
            ci.cancel();
         }
      } else if (this.swiftclient$borderless) {
         this.swiftclient$borderless = false;
         GLFW.glfwSetWindowAttrib(this.handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
         if (!this.fullscreen) {
            this.x = this.windowedX;
            this.y = this.windowedY;
            this.width = this.windowedWidth;
            this.height = this.windowedHeight;
         }
      }
   }

   @Override
   public boolean swiftclient$isBorderless() {
      return this.swiftclient$borderless;
   }

   @Override
   public void swiftclient$reapplyMode() {
      // updateFullscreenIfChanged() runs setMode() again on the next frame.
      this.actuallyFullscreen = !this.fullscreen;
   }
}
