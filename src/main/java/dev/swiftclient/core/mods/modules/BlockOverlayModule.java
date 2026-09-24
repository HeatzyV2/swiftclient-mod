package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

@ConsumedBy({"BlockOverlayState", "BlockOutlineColorMixin"})
public final class BlockOverlayModule extends Module {
   public static final int MODE_HIDDEN = 1;
   public final ModuleSetting mode;
   public final ModuleSetting color;
   public final ModuleSetting opacity;

   public BlockOverlayModule() {
      super("blockoverlay", "Block Overlay", "Recolor or hide the outline of the block you are looking at.", "Render", "blockoverlay", false);
      this.mode = this.cycle("mode", "Mode", new String[]{"Custom outline", "Hidden"}, 0)
         .desc("Draw the outline of the block you are looking at in your colour, or hide it.");
      this.color = this.color("color", "Outline color", -10761985).desc("Colour of the outline.");
      this.opacity = this.slider("opacity", "Opacity", 80.0, 5.0, 100.0, 5.0, "%").desc("How opaque the outline is.");
   }
}
