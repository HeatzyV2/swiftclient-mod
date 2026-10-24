package dev.swiftclient.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

/**
 * Keyboard, mouse and window calls that changed between versions: GLFW up to 26.2, SDL3 from 26.3, and the
 * window handle / key-state signatures that moved in 1.21.9. Key codes are always {@link InputConstants}
 * constants (their values follow the backend).
 */
public final class Input {
   /** "No key" for a key mapping that starts unbound. */
   public static final int KEY_NONE = InputConstants.UNKNOWN.getValue();
   //? if >=26.3 {
   /*public static final int KEY_LSUPER = InputConstants.KEY_LGUI;
   public static final int KEY_RSUPER = InputConstants.KEY_RGUI;
   *///?} else if >=1.21.9 {
   public static final int KEY_LSUPER = InputConstants.KEY_LSUPER;
   public static final int KEY_RSUPER = InputConstants.KEY_RSUPER;
   //?} else {
   /*public static final int KEY_LSUPER = InputConstants.KEY_LWIN;
   public static final int KEY_RSUPER = InputConstants.KEY_RWIN;
   *///?}

   private Input() {
   }

   public static int toCoreKey(int key) {
      //? if >=26.3 {
      /*return dev.swiftclient.core.platform.KeyCodes.fromSdl(key);
      *///?} else
      return key;
   }

   public static int fromCoreKey(int key) {
      //? if >=26.3 {
      /*return dev.swiftclient.core.platform.KeyCodes.toSdl(key);
      *///?} else
      return key;
   }

   public static int toCoreMouse(int button) {
      //? if >=26.3 {
      /*return dev.swiftclient.core.platform.KeyCodes.mouseFromSdl(button);
      *///?} else
      return button;
   }

   public static int toCoreAction(int action) {
      return action == InputConstants.REPEAT ? 2 : action;
   }

   private static Window window() {
      return Minecraft.getInstance().getWindow();
   }

   /** Native window handle (GLFW window, or SDL window from 26.3). */
   public static long handle() {
      //? if >=1.21.9 {
      return window().handle();
      //?} else {
      /*return window().getWindow();
      *///?}
   }

   public static boolean keyDown(int key) {
      //? if >=26.3 {
      /*return InputConstants.isKeyDown(key);
      *///?} else if >=1.21.9 {
      return InputConstants.isKeyDown(window(), key);
      //?} else {
      /*return InputConstants.isKeyDown(handle(), key);
      *///?}
   }

   public static boolean shiftDown() {
      return keyDown(InputConstants.KEY_LSHIFT) || keyDown(InputConstants.KEY_RSHIFT);
   }

   /** Ctrl, or Cmd on macOS. */
   public static boolean ctrlDown() {
      return keyDown(InputConstants.KEY_LCONTROL) || keyDown(InputConstants.KEY_RCONTROL) || keyDown(KEY_LSUPER) || keyDown(KEY_RSUPER);
   }

   public static boolean leftMouseDown() {
      return Minecraft.getInstance().mouseHandler.isLeftPressed();
   }

   /** The game window's Win32 HWND (Windows only). */
   public static long win32Hwnd() {
      //? if >=26.3 {
      /*int props = org.lwjgl.sdl.SDLVideo.SDL_GetWindowProperties(handle());
      return org.lwjgl.sdl.SDLProperties.SDL_GetPointerProperty(props, org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_WIN32_HWND_POINTER, 0L);
      *///?} else {
      return org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(handle());
      //?}
   }

   public static boolean isFullscreen() {
      //? if >=26.3 {
      /*return Minecraft.getInstance().options.fullscreen().get();
      *///?} else
      return window().isFullscreen();
   }

   public static void toggleFullscreen() {
      //? if >=26.3 {
      /*window().setFullscreen(!isFullscreen());
      *///?} else
      window().toggleFullScreen();
   }

   public static boolean minimized() {
      //? if >=26.3 {
      /*return window().isIconified();
      *///?} else
      return window().isMinimized();
   }

   /** Resizes and moves the (windowed) game window. */
   public static void setWindowBounds(int x, int y, int width, int height) {
      long h = handle();
      //? if >=26.3 {
      /*org.lwjgl.sdl.SDLVideo.SDL_SetWindowSize(h, width, height);
      org.lwjgl.sdl.SDLVideo.SDL_SetWindowPosition(h, x, y);
      *///?} else {
      org.lwjgl.glfw.GLFW.glfwSetWindowSize(h, width, height);
      org.lwjgl.glfw.GLFW.glfwSetWindowPos(h, x, y);
      //?}
   }
}
