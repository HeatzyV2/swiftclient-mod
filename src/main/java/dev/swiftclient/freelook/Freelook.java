package dev.swiftclient.freelook;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class Freelook {
   public static volatile boolean active = false;
   private static CameraType saved;

   private Freelook() {
   }

   public static void tick(Minecraft mc) {
      boolean enabled = ModuleManager.active("freelook") && mc.player != null && mc.gui.screen() == null;
      boolean held = enabled && GLFW.glfwGetKey(mc.getWindow().handle(), key()) == 1;
      if (held && !active) {
         start(mc);
      } else if (!held && active) {
         stop(mc);
      }
   }

   private static void start(Minecraft mc) {
      saved = mc.options.getCameraType();
      if (saved == CameraType.FIRST_PERSON) {
         mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      }

      active = true;
   }

   private static void stop(Minecraft mc) {
      active = false;
      if (saved != null) {
         mc.options.setCameraType(saved);
         saved = null;
      }
   }

   public static float maxYaw() {
      Module m = ModuleManager.byId("freelook");
      if (m == null) {
         return 360.0F;
      } else {
         ModuleSetting s = m.setting("maxyaw");
         return s == null ? 360.0F : (float)s.value();
      }
   }

   private static int key() {
      Module m = ModuleManager.byId("freelook");
      if (m == null) {
         return 342;
      } else {
         ModuleSetting s = m.setting("key");
         return s == null ? 342 : s.keyCode();
      }
   }
}
