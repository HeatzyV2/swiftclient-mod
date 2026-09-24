package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class ParticleMultiplierModule extends Module {
   public ParticleMultiplierModule() {
      super("particle_multiplier", "Particle Multiplier", "Multiplie les particules de coups critiques pour une lisibilite maximale.", "Combat", "leaf", false);
      // No behaviour yet: nothing reads "particle_multiplier". Hidden until implemented.
      this.notImplemented();
      this.settings.add(ModuleSetting.cycle("particle_multiplier", "multiplier", "Multiplicateur", new String[]{"x1 (Normal)", "x2", "x3", "x5", "x10 (Extreme)"}, 1).desc("Nombre de particules generees par coup critique."));
      this.settings.add(ModuleSetting.toggle("particle_multiplier", "sharpness", "Particules Tranchant", true).desc("Active egalement la multiplication des particules d'enchantement."));
   }
}
