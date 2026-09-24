package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

@ConsumedBy("GameRendererBlurMixin")
public final class GuiBlurModule extends Module {
   public final ModuleSetting strength;
   public final ModuleSetting darkness;

   public GuiBlurModule() {
      super("gui_blur", "GUI Blur", "Blurs the game behind open menus and inventories.", "Render", "cloud", false);
      this.strength = this.slider("strength", "Blur strength", 60.0, 0.0, 100.0, 5.0, "%");
      this.darkness = this.slider("darkness", "Dim", 25.0, 0.0, 100.0, 5.0, "%");
   }
}
