package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class FullBrightModule extends Module {
   public FullBrightModule() {
      super(
         "fullbright",
         "FullBright",
         "Voir dans le noir comme en plein jour. Sous shaders, la luminosite est bridee : au-dela, l'ecran vire au rouge.",
         "Render",
         "fullbright",
         false
      );
      this.settings
         .add(
            ModuleSetting.slider("fullbright", "level", "Brightness", 100.0, 20.0, 100.0, 5.0, "%")
               .desc("How far the darkness is lifted. Capped automatically when a shader pack is loaded, because going further turns the whole scene red.")
         );
   }
}
