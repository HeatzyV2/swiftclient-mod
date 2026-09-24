package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"TitleScreenMixin", "GameMenuScreenMixin"})
public final class VanillaUiModule extends Module {
   public VanillaUiModule() {
      super("vanillaui", "Vanilla UI", "Hides the Swift Client buttons and 3D skin from the main menu (mod compatibility).", "Render", "vanillaui", false);
   }
}
