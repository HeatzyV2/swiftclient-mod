package dev.swiftclient.renderer;

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
      float gui = Minecraft.getInstance().getWindow().getGuiScale();
      float matrice = CTX == null ? 1.0F : Math.abs(CTX.pose().m00());
      return matrice > 0.0F ? gui * matrice : gui;
   }

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
