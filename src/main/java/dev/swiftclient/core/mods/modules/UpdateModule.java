package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.update.ModUpdate;

/** Tells you in game when a newer Swift Client is out. The launcher installs it; the mod only announces it. */
@ConsumedBy({"ModUpdate"})
public final class UpdateModule extends Module {
   public UpdateModule() {
      super("updates", "Update notice", "Tells you in game, once per session, when a newer Swift Client is out. The launcher installs it.", "Interface", "reload", true);
      this.action("check", "Check now", () -> Tr.of("swift.update.check"), () -> ModUpdate.check(true)).desc("Ask GitHub for the latest release right now.");
   }
}
