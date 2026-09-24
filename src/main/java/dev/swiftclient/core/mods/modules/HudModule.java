package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class HudModule extends Module {
   public HudModule(HudElement e) {
      super(e.moduleId(), e.label, "HUD element - move it in the HUD Editor.", "HUD", e.icon(), false);
   }
}
