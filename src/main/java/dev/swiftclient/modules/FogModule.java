package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.level.material.FogType;

/** Fog Customizer: push back or remove the distance fog, the water and lava fog, and recolour it. */
@ConsumedBy("FogRendererMixin")
public final class FogModule extends Module {
   private static final float FAR = 1.0E6F;
   public final ModuleSetting distance;
   public final ModuleSetting density;
   public final ModuleSetting water;
   public final ModuleSetting lava;
   public final ModuleSetting tint;
   public final ModuleSetting color;

   public FogModule() {
      super("fog", "Fog Customizer", "Push back or remove the fog: render distance, weather, water and lava. Recolour it if you like.", "Visual", "fog", false);
      this.distance = this.toggle("distance", "Remove distance fog", true).desc("No fog at the edge of your render distance.");
      this.density = this.slider("density", "Weather fog", 100.0, 0.0, 300.0, 10.0, "%")
         .desc("Distance of the fog of rain, the Nether and the End. 100% is vanilla, 300% pushes it 3 times further, 0% keeps it very close.");
      this.water = this.toggle("water", "Clear water", false).desc("Remove most of the fog under water.");
      this.lava = this.toggle("lava", "Clear lava", false).desc("See further when you are in lava.");
      this.tint = this.toggle("tint", "Custom color", false).desc("Replace the fog colour with yours.");
      this.color = this.color("color", "Fog color", 0xFF9DB7E8).desc("Colour of the fog when Custom color is on.");
   }

   /** Applied to the fog computed by vanilla for this frame. */
   public static FogData adjust(FogData fog, FogType type) {
      if (!ModuleManager.isLoaded()) {
         return fog;
      }

      FogModule m = ModuleManager.get(FogModule.class);
      if (!m.isEnabled()) {
         return fog;
      }

      if (type == FogType.WATER) {
         if (m.water.boolValue()) {
            fog.environmentalStart = FAR;
            fog.environmentalEnd = FAR;
         }
      } else if (type == FogType.LAVA) {
         if (m.lava.boolValue()) {
            fog.environmentalStart = 4.0F;
            fog.environmentalEnd = 48.0F;
         }
      } else if (type == FogType.NONE || type == FogType.ATMOSPHERIC) {
         float k = (float)(m.density.value() / 100.0);
         if (k >= 2.95F) {
            fog.environmentalStart = FAR;
            fog.environmentalEnd = FAR;
         } else {
            fog.environmentalStart *= k;
            fog.environmentalEnd *= Math.max(0.05F, k);
         }
      }

      if (m.distance.boolValue() && type != FogType.WATER && type != FogType.LAVA) {
         fog.renderDistanceStart = FAR;
         fog.renderDistanceEnd = FAR;
      }

      if (m.tint.boolValue()) {
         int c = m.color.colorValue();
         fog.color.set((c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F, 1.0F);
      }

      return fog;
   }
}
