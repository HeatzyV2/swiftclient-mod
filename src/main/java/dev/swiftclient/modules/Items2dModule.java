package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

/** Dropped items are drawn flat and always turned towards you, like old-school sprites. */
@ConsumedBy("ItemEntityRendererMixin")
public final class Items2dModule extends Module {
   public Items2dModule() {
      super("items_2d", "2D Items", "Dropped items are drawn flat and always face you, easier to read from afar.", "Visual", "items2d", false);
   }
}
