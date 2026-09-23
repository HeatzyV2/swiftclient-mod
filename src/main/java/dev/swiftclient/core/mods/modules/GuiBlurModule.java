package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class GuiBlurModule extends Module {
   public GuiBlurModule() {
      super("gui_blur", "GUI Blur", "Blurs the game behind open menus and inventories.", "Render", "cloud", false);
      this.settings.add(ModuleSetting.slider("gui_blur", "strength", "Blur strength", 60.0, 0.0, 100.0, 5.0, "%"));
      this.settings.add(ModuleSetting.slider("gui_blur", "darkness", "Dim", 25.0, 0.0, 100.0, 5.0, "%"));
   }
}
