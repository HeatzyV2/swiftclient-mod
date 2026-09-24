package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class DeathPointModule extends Module {
   public DeathPointModule() {
      super("death_point", "Death Coordinates", "Enregistre automatiquement les coordonnees de mort avec balise de distance.", "Utility", "pin", false);
      // No behaviour yet: nothing reads "death_point". Hidden until implemented.
      this.notImplemented();
      this.settings.add(ModuleSetting.toggle("death_point", "chat_log", "Message dans le chat", true).desc("Affiche les coordonnees X, Y, Z dans le chat lors de la mort."));
      this.settings.add(ModuleSetting.toggle("death_point", "beacon", "Balise visuelle", true).desc("Affiche un repere temporaire indiquant la direction du stuff."));
   }
}
