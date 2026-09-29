package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

/** Dropped items fall flat on the ground and stay still instead of floating and spinning. */
@ConsumedBy("ItemEntityRendererMixin")
public final class ItemPhysicsModule extends Module {
   public ItemPhysicsModule() {
      super("item_physics", "Item Physics", "Dropped items tumble as they fall, then lie flat on the ground instead of spinning in the air.", "PvP", "physics", false);
   }
}
