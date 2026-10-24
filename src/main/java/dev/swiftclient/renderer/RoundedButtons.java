package dev.swiftclient.renderer;

import dev.swiftclient.compat.GuiPose;
//? if gpu
import dev.swiftclient.renderer.rect.RoundedRectRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class RoundedButtons {
   /** Soft glass fill (#101820 @ ~60%). */
   public static final int BG_NORMAL = -1726998496;
   /** Soft lift on hover (#1A2A40 @ ~67%). */
   public static final int BG_HOVER = -1441125824;
   public static final int BG_DISABLED = -2147154683;
   /** Soft Swift blue ring (#3B82F6 @ ~63%). */
   public static final int BORDER_HOVER = -1606712586;
   /** Soft floating panel behind menu clusters. */
   public static final int PANEL_BG = -1978656736;
   private static GuiGraphicsExtractor CTX;

   public static void bind(GuiGraphicsExtractor ctx) {
      CTX = ctx;
   }

   public static int currentBg(boolean hovered, boolean active) {
      return !active ? BG_DISABLED : (hovered ? BG_HOVER : BG_NORMAL);
   }

   /** Pill radius — fully rounded ends for a smooth look. */
   public static float pillRadius(int h) {
      return Math.max(2.0F, h * 0.5F);
   }

   private static float scale() {
      float gui = (float)Minecraft.getInstance().getWindow().getGuiScale();
      float matrice = CTX == null ? 1.0F : Math.abs(GuiPose.of(CTX).m00());
      return matrice > 0.0F ? gui * matrice : gui;
   }

   //? if gpu {
   private static java.lang.reflect.Field guiRenderStateField;
   private static java.lang.reflect.Field scissorStackField;
   private static java.lang.reflect.Method peekMethod;

   private static void roundRect(float x, float y, float w, float h, float radius, int color) {
      if (CTX != null && !(w <= 0.0F) && !(h <= 0.0F)) {
         try {
            if (guiRenderStateField == null) {
               guiRenderStateField = GuiGraphicsExtractor.class.getDeclaredField("guiRenderState");
               guiRenderStateField.setAccessible(true);
            }
            if (scissorStackField == null) {
               scissorStackField = GuiGraphicsExtractor.class.getDeclaredField("scissorStack");
               scissorStackField.setAccessible(true);
            }
            net.minecraft.client.renderer.state.gui.GuiRenderState renderState =
               (net.minecraft.client.renderer.state.gui.GuiRenderState) guiRenderStateField.get(CTX);
            Object scissorStack = scissorStackField.get(CTX);
            if (peekMethod == null && scissorStack != null) {
               peekMethod = scissorStack.getClass().getMethod("peek");
               peekMethod.setAccessible(true);
            }
            net.minecraft.client.gui.navigation.ScreenRectangle scissor = scissorStack != null ?
               (net.minecraft.client.gui.navigation.ScreenRectangle) peekMethod.invoke(scissorStack) : null;

            renderState.addGuiElement(RoundedRectRenderState.of(CTX.pose(), x, y, w, h, radius, scale(), color, scissor));
         } catch (Throwable ignored) {
         }
      }
   }
   //?} else {
   /*/^* Without the rounded-rect shader (before 26.2): rows of plain fills, one per pixel of the corners. *^/
   private static void roundRect(float x, float y, float w, float h, float radius, int color) {
      if (CTX == null || w <= 0.0F || h <= 0.0F) {
         return;
      }
      int x0 = Math.round(x);
      int y0 = Math.round(y);
      int x1 = Math.round(x + w);
      int y1 = Math.round(y + h);
      int r = Math.min(Math.round(radius), Math.min(x1 - x0, y1 - y0) / 2);
      if (r <= 0) {
         CTX.fill(x0, y0, x1, y1, color);
         return;
      }
      CTX.fill(x0, y0 + r, x1, y1 - r, color);
      for (int i = 0; i < r; i++) {
         double dy = r - i - 0.5;
         int inset = (int)Math.round(r - Math.sqrt(r * r - dy * dy));
         CTX.fill(x0 + inset, y0 + i, x1 - inset, y0 + i + 1, color);
         CTX.fill(x0 + inset, y1 - i - 1, x1 - inset, y1 - i, color);
      }
   }
   *///?}

   public static void drawButtonBackground(int x, int y, int w, int h, boolean hovered, boolean active) {
      float r = pillRadius(h);
      int bg = currentBg(hovered, active);
      if (hovered && active) {
         float t = 1.25F;
         roundRect(x, y, w, h, r, BORDER_HOVER);
         roundRect(x + t, y + t, w - 2.0F * t, h - 2.0F * t, Math.max(0.0F, r - t), bg);
      } else {
         roundRect(x, y, w, h, r, bg);
      }
   }

   public static void drawSoftPanel(int x, int y, int w, int h) {
      float r = Math.min(18.0F, Math.min(w, h) * 0.18F);
      roundRect(x, y, w, h, r, PANEL_BG);
   }

   public static void drawCircle(int x, int y, int size, boolean hovered, boolean active) {
      float r = size * 0.5F;
      int bg = currentBg(hovered, active);
      if (hovered && active) {
         float t = 1.25F;
         roundRect(x, y, size, size, r, BORDER_HOVER);
         roundRect(x + t, y + t, size - 2.0F * t, size - 2.0F * t, Math.max(0.0F, r - t), bg);
      } else {
         roundRect(x, y, size, size, r, bg);
      }
   }

   public static void drawCard(int x, int y, int w, int h, int bgColor, int borderColor, int borderThickness, float radius) {
      if (borderThickness > 0) {
         float t = borderThickness;
         roundRect(x, y, w, h, radius, borderColor);
         roundRect(x + t, y + t, w - 2.0F * t, h - 2.0F * t, Math.max(0.0F, radius - t), bgColor);
      } else {
         roundRect(x, y, w, h, radius, bgColor);
      }
   }

   public static void drawRoundedRect(int x, int y, int w, int h, float radius, int color) {
      roundRect(x, y, w, h, radius, color);
   }
}
