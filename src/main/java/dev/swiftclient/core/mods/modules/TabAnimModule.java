package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"PlayerTabOverlayMixin"})
public final class TabAnimModule extends Module {
   public TabAnimModule() {
      super("tabanim", "Tab Animation", "The player list slides in from the top when you press Tab.", "Render", "tabanim", false);
   }
}
