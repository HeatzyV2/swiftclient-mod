package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class ItemTrackerModule extends Module {
   public ItemTrackerModule() {
      super("hud_itemtracker", "Item Tracker", "A feed of the items you pick up. Move it in the HUD Editor.", "HUD", "pickup", false);
      this.slider("duration", "Display time", 4.0, 1.0, 10.0, 1.0, "s").desc("How long each line stays on screen.");
      this.toggle("losses", "Show lost items", false).desc("Also list the items that leave your inventory (dropped, used, crafted).");
   }
}
