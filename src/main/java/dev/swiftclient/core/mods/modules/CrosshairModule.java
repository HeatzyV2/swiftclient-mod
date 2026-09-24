package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.screen.CrosshairEditorScreen;
import dev.swiftclient.core.ui.ScreenRequest;

@ConsumedBy({"Crosshair", "HudCrosshairMixin"})
public final class CrosshairModule extends Module {
   public static final int STYLE_CUSTOM = 4;
   public final ModuleSetting style;
   public final ModuleSetting color;
   public final ModuleSetting pixelSize;
   public final ModuleSetting length;
   public final ModuleSetting thickness;
   public final ModuleSetting gap;
   public final ModuleSetting dot;
   public final ModuleSetting outline;

   public CrosshairModule() {
      super("crosshair", "Crosshair", "Custom crosshair: style, color, size.", "Render", "gear", false);
      this.style = this.cycle("style", "Style", new String[]{"Cross", "Dot", "Circle", "T", "Custom"}, 0);
      this.color = this.color("color", "Color", -1);
      this.action("edit", "Pixel editor", () -> Tr.of("swift.common.open"), () -> ScreenRequest.open(new CrosshairEditorScreen()));
      this.pixelSize = this.slider("pixsize", "Pixel size", 2.0, 1.0, 5.0, 1.0, "px");
      this.length = this.slider("size", "Length", 4.0, 1.0, 12.0, 1.0, "px");
      this.thickness = this.slider("thick", "Thickness", 1.0, 1.0, 4.0, 1.0, "px");
      this.gap = this.slider("gap", "Gap", 2.0, 0.0, 8.0, 1.0, "px");
      this.dot = this.toggle("dot", "Center dot", false);
      this.outline = this.toggle("outline", "Outline", true);
   }
}
