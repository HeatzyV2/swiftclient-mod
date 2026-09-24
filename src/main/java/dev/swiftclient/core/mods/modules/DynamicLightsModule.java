package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class DynamicLightsModule extends Module {
   public DynamicLightsModule() {
      super("dynamic_lights", "Dynamic Lights", "Les torches et objets lumineux tenus en main eclairent en temps reel.", "Render", "sunrise", false);
      // No behaviour yet: nothing reads "dynamic_lights". Hidden until implemented.
      this.notImplemented();
      this.settings.add(ModuleSetting.cycle("dynamic_lights", "mode", "Qualite", new String[]{"Desactive", "Rapide", "Detaille"}, 2).desc("Niveau de detail de l'eclairage dynamique."));
      this.settings.add(ModuleSetting.toggle("dynamic_lights", "dropped_items", "Items au sol lumineux", true).desc("Les torches jetees au sol emettent egalement de la lumiere."));
   }
}
