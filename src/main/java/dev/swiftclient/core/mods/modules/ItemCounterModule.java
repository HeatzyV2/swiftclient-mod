package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class ItemCounterModule extends Module {
   public ItemCounterModule() {
      super("hud_itemcounter", "Item Counter", "How many arrows, pearls, golden apples, potions, totems and blocks you carry. Move it in the HUD Editor.", "HUD", "stack", false);
      this.toggle("arrows", "Arrows", true).desc("Count arrows, tipped and spectral ones included.").group("Items");
      this.toggle("pearls", "Ender pearls", true).desc("Count ender pearls.").group("Items");
      this.toggle("gapples", "Golden apples", true).desc("Count golden apples, enchanted ones included.").group("Items");
      this.toggle("potions", "Splash potions", true).desc("Count splash and lingering potions.").group("Items");
      this.toggle("totems", "Totems", true).desc("Count totems of undying.").group("Items");
      this.toggle("blocks", "Blocks", true).desc("Count every placeable block, for bridging.").group("Items");
      this.cycle("layout", "Layout", new String[]{"Horizontal", "Vertical"}, 1).desc("Arrange the counters in a row or in a column.").group("Style");
      this.toggle("hide_empty", "Hide missing items", true).desc("Do not show a counter at zero.").group("Style");
      this.toggle("background", "Show background", true).desc("Draw a dark panel behind the counters.").group("Style");
   }
}
