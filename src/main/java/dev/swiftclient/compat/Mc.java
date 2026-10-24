package dev.swiftclient.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;

/**
 * Client state whose access moved between Minecraft versions. Code outside this package calls these instead
 * of the vanilla fields, so the version conditions stay here (written for 26.2, see stonecutter.gradle.kts).
 */
public final class Mc {
   private Mc() {
   }

   public static Minecraft get() {
      return Minecraft.getInstance();
   }

   /** The open screen (26.2 moved it from Minecraft into Gui). */
   public static Screen screen() {
      //? if >=26.2 {
      return get().gui.screen();
      //?} else {
      /*return get().screen;
      *///?}
   }

   public static void setScreen(Screen screen) {
      //? if >=26.2 {
      get().gui.setScreen(screen);
      //?} else {
      /*get().setScreen(screen);
      *///?}
   }

   /** The loading overlay, if one is up. */
   public static Overlay overlay() {
      //? if >=26.2 {
      return get().gui.overlay();
      //?} else {
      /*return get().getOverlay();
      *///?}
   }

   public static ToastManager toasts() {
      //? if >=26.2 {
      return get().gui.toastManager();
      //?} else {
      /*return get().getToastManager();
      *///?}
   }

   /** The in-game HUD (`Hud` since 26.2, `Gui` before: the type name is swapped by Stonecutter). */
   public static Hud hud() {
      //? if >=26.2 {
      return get().gui.hud;
      //?} else {
      /*return get().gui;
      *///?}
   }

   /** F1: the HUD is hidden. */
   public static boolean hudHidden() {
      //? if >=26.2 {
      return hud().isHidden();
      //?} else {
      /*return get().options.hideGui;
      *///?}
   }

   public static net.minecraft.client.Camera camera() {
      //? if >=26.2 {
      return get().gameRenderer.mainCamera();
      //?} else {
      /*return get().gameRenderer.getMainCamera();
      *///?}
   }

   /** Prints a client-side line in the chat. */
   public static void chat(net.minecraft.network.chat.Component message) {
      //? if >=26.1 {
      hud().getChat().addClientSystemMessage(message);
      //?} else
      /*hud().getChat().addMessage(message);*/
   }

   /** Takes a screenshot like F2 does. */
   public static void screenshot() {
      Minecraft mc = get();
      //? if >=26.2 {
      net.minecraft.client.Screenshot.grab(mc, false);
      //?} else {
      /*net.minecraft.client.Screenshot.grab(mc.gameDirectory, mainTarget(), msg -> mc.execute(() -> chat(msg)));
      *///?}
   }

   /** The main framebuffer the world and the GUI are drawn into. */
   public static RenderTarget mainTarget() {
      //? if >=26.2 {
      return get().gameRenderer.mainRenderTarget();
      //?} else {
      /*return get().getMainRenderTarget();
      *///?}
   }
}
