package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class FoodStatusModule extends Module {
   public FoodStatusModule() {
      super("food_status", "Food and Saturation", "Affiche la barre de saturation et la previsu des gains de faim (AppleSkin).", "HUD", "flask", false);
      // No behaviour yet: nothing reads "food_status". Hidden until implemented.
      this.notImplemented();
      this.settings.add(ModuleSetting.toggle("food_status", "show_saturation", "Barre de saturation", true).desc("Affiche la barre de saturation cachee au-dessus de la faim."));
      this.settings.add(ModuleSetting.toggle("food_status", "heal_preview", "Previsu du soin", true).desc("Clignotement des coeurs et gigots que l'aliment va rendre."));
   }
}
