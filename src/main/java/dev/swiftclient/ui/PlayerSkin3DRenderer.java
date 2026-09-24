package dev.swiftclient.ui;

import dev.swiftclient.core.log.Log;
import org.slf4j.Logger;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.player.PlayerSkin;
import org.lwjgl.glfw.GLFW;

public final class PlayerSkin3DRenderer {
   private static final Logger LOG = Log.get("Ui");
   private static PlayerSkinWidget widget;
   private static int currentW = -1;
   private static int currentH = -1;
   private static Field fRotationX;
   private static Field fRotationY;
   private static Field fWideModel;
   private static Field fSlimModel;
   private static final float DEFAULT_YAW = 25.0F;
   private static final float DEFAULT_PITCH = 0.0F;
   private static float yawRot = 25.0F;
   private static float pitchRot = 0.0F;
   private static boolean dragging = false;
   private static int lastMouseX = 0;
   private static int lastMouseY = 0;
   private static final float SENS_YAW = 1.5F;
   private static final float SENS_PITCH = 1.0F;
   private static final float PITCH_MIN = -45.0F;
   private static final float PITCH_MAX = 45.0F;

   private PlayerSkin3DRenderer() {
   }

   public static boolean render(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int mouseX, int mouseY, float delta) {
      Object skin = SkinRenderer.getCachedSkinTexturesObj();
      if (!(skin instanceof PlayerSkin)) {
         return false;
      } else {
         try {
            Minecraft mc = Minecraft.getInstance();
            if (widget == null || currentW != w || currentH != h) {
               widget = new PlayerSkinWidget(w, h, mc.getEntityModels(), () -> (PlayerSkin)SkinRenderer.getCachedSkinTexturesObj());
               currentW = w;
               currentH = h;
            }

            widget.setX(x);
            widget.setY(y);
            long handle = mc.getWindow().handle();
            boolean lmbDown = GLFW.glfwGetMouseButton(handle, 0) == 1;
            boolean insidePanel = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
            if (lmbDown && !dragging && insidePanel) {
               dragging = true;
            } else if (!lmbDown && dragging) {
               dragging = false;
            }

            if (dragging) {
               int dx = mouseX - lastMouseX;
               int dy = mouseY - lastMouseY;
               yawRot += dx * 1.5F;
               pitchRot -= dy * 1.0F;
               if (pitchRot < -45.0F) {
                  pitchRot = -45.0F;
               }

               if (pitchRot > 45.0F) {
                  pitchRot = 45.0F;
               }
            }

            lastMouseX = mouseX;
            lastMouseY = mouseY;
            applyRotation(widget, pitchRot, yawRot);
            resetModelPose(widget);
            widget.extractRenderState(ctx, mouseX, mouseY, delta);
            return true;
         } catch (Throwable var16) {
            LOG.warn("Rendu du skin 3D impossible", var16);
            return false;
         }
      }
   }

   private static void resetModelPose(PlayerSkinWidget w) {
      try {
         if (fWideModel == null || fSlimModel == null) {
            fWideModel = PlayerSkinWidget.class.getDeclaredField("wideModel");
            fSlimModel = PlayerSkinWidget.class.getDeclaredField("slimModel");
            fWideModel.setAccessible(true);
            fSlimModel.setAccessible(true);
         }

         if (fWideModel.get(w) instanceof Model m) {
            m.resetPose();
         }

         if (fSlimModel.get(w) instanceof Model m) {
            m.resetPose();
         }
      } catch (Throwable var3) {
      }
   }

   private static void applyRotation(PlayerSkinWidget w, float pitch, float yaw) {
      try {
         if (fRotationX == null || fRotationY == null) {
            fRotationX = PlayerSkinWidget.class.getDeclaredField("rotationX");
            fRotationY = PlayerSkinWidget.class.getDeclaredField("rotationY");
            fRotationX.setAccessible(true);
            fRotationY.setAccessible(true);
         }

         fRotationX.setFloat(w, pitch);
         fRotationY.setFloat(w, yaw);
      } catch (Throwable var4) {
      }
   }

   public static void resetRotation() {
      yawRot = 25.0F;
      pitchRot = 0.0F;
   }
}
