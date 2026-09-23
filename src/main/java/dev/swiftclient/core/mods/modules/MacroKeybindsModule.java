package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class MacroKeybindsModule extends Module {
   public MacroKeybindsModule() {
      super("macro_keybinds", "Macro Keybinds", "Associez des raccourcis pour envoyer des commandes instantanees (/hub, /spawn...)", "Utility", "keyboard", true);
      this.settings.add(ModuleSetting.cycle("macro_keybinds", "macro_1", "Macro 1", new String[]{"/hub", "/spawn", "/lobby", "/tpa accept"}, 0).desc("Commande rapide pour la touche Macro 1."));
      this.settings.add(ModuleSetting.cycle("macro_keybinds", "macro_2", "Macro 2", new String[]{"/spawn", "/home", "/warp pvp", "/back"}, 0).desc("Commande rapide pour la touche Macro 2."));
   }
}
