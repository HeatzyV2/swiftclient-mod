package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class BlockOverlayModule extends Module {
   public BlockOverlayModule() {
      super("blockoverlay", "Block Overlay", "Recolor or hide the outline of the block you are looking at.", "Render", "blockoverlay", false);
      this.settings
         .add(
            ModuleSetting.cycle("blockoverlay", "mode", "Mode", new String[]{"Custom outline", "Hidden"}, 0)
               .desc("How the block you are looking at is marked: outline, filled face, or both.")
         );
      this.settings.add(ModuleSetting.color("blockoverlay", "color", "Outline color", -10761985).desc("Colour of the outline and of the filled face."));
      this.settings
         .add(ModuleSetting.slider("blockoverlay", "opacity", "Opacity", 80.0, 5.0, 100.0, 5.0, "%").desc("How strongly the overlay covers the block."));
   }
}
