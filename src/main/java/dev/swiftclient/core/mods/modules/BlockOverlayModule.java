package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

@ConsumedBy({"BlockOverlayState", "BlockOutlineColorMixin", "BlockInfoHud"})
public final class BlockOverlayModule extends Module {
   public static final int MODE_HIDDEN = 1;
   public final ModuleSetting mode;
   public final ModuleSetting color;
   public final ModuleSetting opacity;
   public final ModuleSetting info;
   public final ModuleSetting infoPosition;
   public final ModuleSetting infoTool;

   public BlockOverlayModule() {
      super("blockoverlay", "Block Outline", "Recolor or hide the outline of the block you are looking at.", "Visual", "blockoverlay", false);
      this.mode = this.cycle("mode", "Mode", new String[]{"Custom outline", "Hidden"}, 0)
         .desc("Draw the outline of the block you are looking at in your colour, or hide it.");
      this.color = this.color("color", "Outline color", -10761985).desc("Colour of the outline.");
      this.opacity = this.slider("opacity", "Opacity", 80.0, 5.0, 100.0, 5.0, "%").desc("How opaque the outline is.");
      this.info = this.toggle("info", "Block info", true).desc("A panel with the name of the block you are looking at and the mod it comes from.").group("Block info");
      this.infoPosition = this.cycle("info_position", "Position", new String[]{"Top", "Under crosshair"}, 0).desc("Where the panel sits on the screen.").group("Block info");
      this.infoTool = this.toggle("info_tool", "Show tool", true).desc("The tool that mines the block, ticked green when the item in your hand breaks it properly.").group("Block info");
   }
}
