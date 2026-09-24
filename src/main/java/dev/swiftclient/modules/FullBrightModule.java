package dev.swiftclient.modules;

import dev.swiftclient.core.gfx.Shaders;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.mixin.SimpleOptionAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

public final class FullBrightModule extends Module {
   public final ModuleSetting level;
   /** The player's gamma, restored when the module is switched off. */
   private Double savedGamma;

   public FullBrightModule() {
      super(
         "fullbright",
         "FullBright",
         "Voir dans le noir comme en plein jour. Sous shaders, la luminosite est bridee : au-dela, l'ecran vire au rouge.",
         "Render",
         "fullbright",
         false
      );
      this.level = this.slider("level", "Brightness", 100.0, 20.0, 100.0, 5.0, "%")
         .desc("How far the darkness is lifted. Capped automatically when a shader pack is loaded, because going further turns the whole scene red.");
   }

   private static OptionInstance<Double> gamma() {
      return Minecraft.getInstance().options.gamma();
   }

   @Override
   protected void onEnable() {
      this.savedGamma = gamma().get();
   }

   @Override
   protected void onTick() {
      // Out of the option's range on purpose, hence the raw setter.
      ((SimpleOptionAccessor)(Object)gamma()).swiftclient$setRaw(Shaders.gammaFullBright(this.level.value()));
   }

   @Override
   protected void onDisable() {
      if (this.savedGamma != null) {
         gamma().set(this.savedGamma);
         this.savedGamma = null;
      }
   }

   /** Called right before options.txt is written, so the boosted gamma is never saved as the player's. */
   public void beforeOptionsSave() {
      if (this.isEnabled() && this.savedGamma != null) {
         ((SimpleOptionAccessor)(Object)gamma()).swiftclient$setRaw(this.savedGamma);
      }
   }
}
