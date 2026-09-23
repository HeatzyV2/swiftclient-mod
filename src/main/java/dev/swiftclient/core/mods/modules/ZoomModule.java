package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class ZoomModule extends Module {
   public ZoomModule() {
      super("zoom", "Zoom", "Hold the zoom key to zoom in (scroll to adjust).", "Render", "zoom", false);
      this.settings.add(ModuleSetting.key("zoom", "key", "Zoom key", 67).desc("Key to hold, or to press, depending on the mode below."));
      this.settings
         .add(ModuleSetting.cycle("zoom", "mode", "Mode", new String[]{"Hold", "Toggle"}, 0).desc("Hold the key, or press once to toggle the zoom on and off."));
      this.settings
         .add(
            ModuleSetting.slider("zoom", "duration", "Smooth duration", 180.0, 50.0, 500.0, 10.0, "ms")
               .desc("How long the zoom takes to reach its target. Shorter feels snappier, longer feels cinematic.")
         );
      this.settings.add(ModuleSetting.toggle("zoom", "smooth", "Smooth transition", true).desc("Ease in and out instead of snapping to the zoomed view."));
      this.settings
         .add(
            ModuleSetting.slider("zoom", "fov", "Zoom FOV", 30.0, 10.0, 60.0, 5.0, "")
               .desc("Field of view while zoomed. The lower the value, the closer it gets.")
         );
   }
}
