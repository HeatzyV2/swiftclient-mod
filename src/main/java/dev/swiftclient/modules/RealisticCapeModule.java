package dev.swiftclient.modules;

import dev.swiftclient.core.mods.CapeSimManager;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.mods.RealisticCapeState;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

public final class RealisticCapeModule extends Module {
   /** Beyond this distance the cape is drawn with the plain vanilla model (see CapeLayerMixin). */
   private static final double SIM_RANGE_SQ = 32.0 * 32.0;
   public final ModuleSetting amplitude;
   public final ModuleSetting speed;

   public RealisticCapeModule() {
      super("realisticcape", "Realistic Cape", "Continuous wave effect on the cape.", "Render", "cape", false);
      this.amplitude = this.slider("amplitude", "Amplitude", 8.0, 2.0, 20.0, 1.0, "°").desc("How far the cape swings away from your back.");
      this.speed = this.slider("speed", "Speed", 50.0, 10.0, 100.0, 5.0, "%").desc("How fast the wave travels down the cape.");
   }

   @Override
   protected void onTick() {
      RealisticCapeState.configure(true, this.speed.value(), this.amplitude.value());
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         long now = System.currentTimeMillis();
         Set<UUID> simulated = new HashSet<>();

         for (AbstractClientPlayer p : mc.level.players()) {
            if (p == mc.player || p.distanceToSqr(mc.player) <= SIM_RANGE_SQ) {
               simulated.add(p.getUUID());
               CapeSimManager.tick(p.getUUID(), p.getX(), p.getY(), p.getZ(), p.yOld, p.yBodyRot, p.isCrouching(), p.isUnderWater(), now);
            }
         }

         CapeSimManager.retainOnly(simulated);
      } else {
         CapeSimManager.clear();
      }
   }

   @Override
   protected void onDisable() {
      RealisticCapeState.configure(false, this.speed.value(), this.amplitude.value());
      CapeSimManager.clear();
   }
}
