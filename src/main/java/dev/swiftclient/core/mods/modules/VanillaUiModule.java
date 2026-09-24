package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"TitleScreenMixin", "GameMenuScreenMixin"})
public final class VanillaUiModule extends Module {
   public VanillaUiModule() {
      super("vanillaui", "Vanilla UI", "Cache les boutons et le skin 3D Swift Client du menu principal (compat mods).", "Render", "vanillaui", false);
   }
}
