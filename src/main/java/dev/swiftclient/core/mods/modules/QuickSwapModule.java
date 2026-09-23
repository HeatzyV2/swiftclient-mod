package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class QuickSwapModule extends Module {
   public QuickSwapModule() {
      super("quick_swap", "Quick Swap", "Intervertit instantanement Plastron et Elytres (ou main gauche) avec une touche.", "Utility", "shirt", true);
      this.settings.add(ModuleSetting.cycle("quick_swap", "target", "Cible rapide", new String[]{"Elytres / Plastron", "Totem d'immortalite", "Bouclier"}, 0).desc("Objet a equiper automatiquement."));
      this.settings.add(ModuleSetting.toggle("quick_swap", "sound", "Son de confirmation", true).desc("Joue un son discret lors de l'echange."));
   }
}
