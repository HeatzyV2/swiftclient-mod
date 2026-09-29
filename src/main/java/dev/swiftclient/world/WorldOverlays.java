package dev.swiftclient.world;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;

/**
 * Everything Swift draws in the world (chunk borders, light levels, waypoints, hitboxes...), emitted as
 * vanilla gizmos right after the debug renderers (see DebugRendererMixin). Each module draws itself.
 */
public final class WorldOverlays {
   private WorldOverlays() {
   }

   /** A module that draws in the world. Called once per frame while it is enabled. */
   public interface WorldOverlay {
      void emitGizmos(Minecraft mc, Vec3 camera, float partialTick);
   }

   public static void emit(double camX, double camY, double camZ, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null || !ModuleManager.isLoaded()) {
         return;
      }

      Vec3 cam = new Vec3(camX, camY, camZ);
      for (Module m : ModuleManager.modules()) {
         if (m instanceof WorldOverlay o && m.isEnabled()) {
            try {
               o.emitGizmos(mc, cam, partialTick);
            } catch (RuntimeException e) {
               // A broken overlay must not stop the frame; the others still draw.
            }
         }
      }

      tntCountdown(mc, partialTick);
   }

   /** TNT Countdown: seconds left written above each lit TNT. */
   private static void tntCountdown(Minecraft mc, float partialTick) {
      Module m = ModuleManager.byId("hud_tnt");
      ModuleSetting world = m == null ? null : m.setting("world");
      if (m == null || !m.isEnabled() || world == null || !world.boolValue()) {
         return;
      }

      for (PrimedTnt t : mc.level.getEntitiesOfClass(PrimedTnt.class, mc.player.getBoundingBox().inflate(48.0), e -> true)) {
         float secs = Math.max(0.0F, (t.getFuse() - partialTick) / 20.0F);
         int color = secs > 2.0F ? 0xFF6BD36B : (secs > 1.0F ? 0xFFF2C94C : 0xFFFF6B6B);
         Vec3 p = t.getPosition(partialTick).add(0.0, 1.35, 0.0);
         Gizmos.billboardText(String.format(Locale.ROOT, "%.1fs", secs), p, TextGizmo.Style.forColorAndCentered(color).withScale(0.6F)).setAlwaysOnTop();
      }
   }
}
