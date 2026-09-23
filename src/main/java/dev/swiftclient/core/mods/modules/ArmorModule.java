package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class ArmorModule extends Module {
   public ArmorModule() {
      super("hud_armor", "Armor", "Equipped armor as item icons. Move it in the HUD Editor.", "HUD", "shield", false);
      this.settings
         .add(
            ModuleSetting.cycle("hud_armor", "layout", "Layout", new String[]{"Horizontal", "Vertical"}, 0)
               .desc("Arrange the four pieces in a row or in a column.")
         );
      this.settings
         .add(
            ModuleSetting.cycle("hud_armor", "durability", "Durability", new String[]{"Hidden", "Bar", "Percent", "Value"}, 1)
               .desc("What to show under each piece: nothing, a bar, a percentage or the raw value.")
         );
      this.settings
         .add(ModuleSetting.toggle("hud_armor", "held", "Show held item", true).desc("Also show the item currently in your hand, next to the armour."));
      this.settings
         .add(
            ModuleSetting.toggle("hud_armor", "background", "Show background", true)
               .desc("Draw a dark panel behind the icons so they stay readable on a bright sky.")
         );
   }
}
