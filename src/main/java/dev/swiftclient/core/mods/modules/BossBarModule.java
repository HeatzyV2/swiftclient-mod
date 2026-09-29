package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class BossBarModule extends Module {
   public BossBarModule() {
      super("hud_bossbar", "Boss Bar", "Replaces the boss bars with movable, resizable Swift bars. Move it in the HUD Editor.", "HUD", "bossbar", false);
      this.toggle("title", "Show title", true).desc("Write the boss name above each bar.");
      this.slider("max", "Max bars", 3.0, 1.0, 6.0, 1.0, "").desc("How many boss bars can be shown at once.");
      this.toggle("own_color", "Custom color", false).desc("Use your colour instead of the one chosen by the server.");
      this.color("color", "Bar color", -12877066).desc("Colour of the bars when Custom color is on.");
   }
}
