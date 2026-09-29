package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class CooldownsModule extends Module {
   public CooldownsModule() {
      super("hud_cooldowns", "Cooldowns", "Items you cannot use yet (pearls, chorus fruit, shield...) with the time left. Move it in the HUD Editor.", "PvP", "hourglass", false);
      this.toggle("background", "Show background", true).desc("Draw a dark panel behind the items.");
      this.color("color", "Bar color", -12877066).desc("Colour of the bar that drains under each item.");
   }
}
