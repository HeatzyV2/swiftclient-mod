package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class HitSoundsModule extends Module {
   public HitSoundsModule() {
      super("hit_sounds", "Hit Sounds", "Joue un son d'impact personnalise quand vous touchez un adversaire.", "Combat", "bell", false);
      // No behaviour yet: nothing reads "hit_sounds". Hidden until implemented.
      this.notImplemented();
      this.settings.add(ModuleSetting.cycle("hit_sounds", "sound", "Type de son", new String[]{"Ding (Aigu)", "CoD Hitmarker", "Osu Click", "Skeet", "Bonk"}, 0).desc("Effet sonore joue a chaque coup reussi."));
      this.settings.add(ModuleSetting.slider("hit_sounds", "volume", "Volume sonore", 80.0, 10.0, 100.0, 5.0, "%").desc("Volume de l'effet sonore (en pourcent)."));
   }
}
