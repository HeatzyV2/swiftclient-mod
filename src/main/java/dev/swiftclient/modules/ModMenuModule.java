package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.input.SwiftKeys;

/** Ways into the Swift mods menu: its key, and the shortcuts at the bottom of the pause menu. */
@ConsumedBy("GameMenuScreenMixin")
public final class ModMenuModule extends Module {
   public final ModuleSetting pauseLinks;

   public ModMenuModule() {
      super("mod_menu", "Mod Menu", "Shortcuts to this menu: its key, and the Mods / Cosmetics / Language links of the pause menu.", "Interface", "menu", true);
      this.action("key", "Menu key", () -> SwiftKeys.label(SwiftKeys.MENU), SwiftKeys::openControls)
         .desc("Key that opens this menu in game. Change it in Options > Controls > Key Binds.");
      this.pauseLinks = this.toggle("pause_links", "Pause menu shortcuts", true).desc("Show the Mods, Cosmetics and Language links at the bottom of the pause menu.");
   }

   public static boolean pauseLinks() {
      if (!ModuleManager.isLoaded()) {
         return true;
      }

      ModMenuModule m = ModuleManager.get(ModMenuModule.class);
      return m.isEnabled() && m.pauseLinks.boolValue();
   }
}
