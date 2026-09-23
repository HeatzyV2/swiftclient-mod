package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.screen.CrosshairEditorScreen;
import dev.swiftclient.core.ui.ScreenRequest;

public final class CrosshairModule extends Module {
   public CrosshairModule() {
      super("crosshair", "Crosshair", "Custom crosshair: style, color, size.", "Render", "gear", false);
      this.settings.add(ModuleSetting.cycle("crosshair", "style", "Style", new String[]{"Cross", "Dot", "Circle", "T", "Custom"}, 0));
      this.settings.add(ModuleSetting.color("crosshair", "color", "Color", -1));
      this.settings.add(ModuleSetting.action("crosshair", "edit", "Pixel editor", () -> "Open", () -> ScreenRequest.open(new CrosshairEditorScreen())));
      this.settings.add(ModuleSetting.slider("crosshair", "pixsize", "Pixel size", 2.0, 1.0, 5.0, 1.0, "px"));
      this.settings.add(ModuleSetting.slider("crosshair", "size", "Length", 4.0, 1.0, 12.0, 1.0, "px"));
      this.settings.add(ModuleSetting.slider("crosshair", "thick", "Thickness", 1.0, 1.0, 4.0, 1.0, "px"));
      this.settings.add(ModuleSetting.slider("crosshair", "gap", "Gap", 2.0, 0.0, 8.0, 1.0, "px"));
      this.settings.add(ModuleSetting.toggle("crosshair", "dot", "Center dot", false));
      this.settings.add(ModuleSetting.toggle("crosshair", "outline", "Outline", true));
   }
}
