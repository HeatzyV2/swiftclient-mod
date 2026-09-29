package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class PackDisplayModule extends Module {
   public PackDisplayModule() {
      super("hud_packs", "Pack Display", "The resource packs you are using. Move it in the HUD Editor.", "World", "palette", false);
      this.slider("max", "Max packs", 3.0, 1.0, 8.0, 1.0, "").desc("How many packs are listed, starting from the top one.");
   }
}
