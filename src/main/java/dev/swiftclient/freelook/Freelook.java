package dev.swiftclient.freelook;

import dev.swiftclient.input.SwiftKeys;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/** Freelook camera state, driven by FreelookModule and read by the Freelook mixins. */
public final class Freelook {
   public static volatile boolean active = false;
   private static volatile float maxYaw = 360.0F;
   private static CameraType saved;

   private Freelook() {
   }

   public static void tick(Minecraft mc, float maxYawDegrees) {
      maxYaw = maxYawDegrees;
      boolean held = mc.player != null && mc.gui.screen() == null && SwiftKeys.FREELOOK.isDown();
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

   public static void stop(Minecraft mc) {
      active = false;
      if (saved != null) {
         mc.options.setCameraType(saved);
         saved = null;
      }
   }

   public static float maxYaw() {
      return maxYaw;
   }
}
