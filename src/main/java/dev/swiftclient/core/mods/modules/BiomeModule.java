package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

@ConsumedBy({"HudManager"})
public final class BiomeModule extends Module {
   public BiomeModule() {
      super("hud_biome", "Biome", "Current biome. Move it in the HUD Editor.", "HUD", "leaf", false);
      this.settings.add(ModuleSetting.toggle("hud_biome", "label", "Show \"Biome\" label", true).desc("Prefix the biome name with the word \"Biome\"."));
   }
}
