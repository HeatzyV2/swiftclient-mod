package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class OldAnimationsModule extends Module {
   public OldAnimationsModule() {
      super("old_animations", "Old Animations (1.8)", "Retablit les animations classiques : parade d'epee (block-hit), arc, etc.", "Combat", "shield", false);
      // No behaviour yet: nothing reads "old_animations". Hidden until implemented.
      this.notImplemented();
      this.settings.add(ModuleSetting.toggle("old_animations", "block_hit", "Block-Hit 1.8", true).desc("Animation de parade a l'epee lors des attaques."));
      this.settings.add(ModuleSetting.toggle("old_animations", "old_eating", "Animation de nourriture", true).desc("Consommation d'aliments avec le rythme classique 1.8."));
      this.settings.add(ModuleSetting.toggle("old_animations", "old_bow", "Animation d'arc", true).desc("Tir a l'arc avec le maintien classique."));
   }
}
