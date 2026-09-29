package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

/** Colour grading of the world: saturation, contrast, brightness. The HUD and menus are not affected. */
@ConsumedBy("GameRendererBlurMixin")
public final class SaturationModule extends Module {
   public final ModuleSetting saturation;
   public final ModuleSetting contrast;
   public final ModuleSetting brightness;

   public SaturationModule() {
      super("saturation", "Color Saturation", "Makes the world more vivid or more washed out, with contrast and brightness. The HUD is not affected.", "Visual", "saturation", false);
      this.saturation = this.slider("saturation", "Saturation", 130.0, 0.0, 200.0, 5.0, "%").desc("100% is vanilla, 0% is black and white, 200% is very vivid.");
      this.contrast = this.slider("contrast", "Contrast", 100.0, 50.0, 150.0, 5.0, "%").desc("100% is vanilla. Higher darkens shadows and brightens highlights.");
      this.brightness = this.slider("brightness", "Brightness", 100.0, 70.0, 130.0, 5.0, "%").desc("100% is vanilla.");
   }
}
