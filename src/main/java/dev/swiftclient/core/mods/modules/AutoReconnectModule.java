package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class AutoReconnectModule extends Module {
   public AutoReconnectModule() {
      super("auto_reconnect", "Auto Reconnect", "Bouton Reconnecter direct et compte a rebours automatique lors d'un kick.", "Utility", "clock", false);
      // No behaviour yet: nothing reads "auto_reconnect". Hidden until implemented.
      this.notImplemented();
      this.settings.add(ModuleSetting.cycle("auto_reconnect", "delay", "Delai de reconnexion", new String[]{"3 secondes", "5 secondes", "10 secondes", "Manuel uniquement"}, 1).desc("Delai avant tentative de reconnexion automatique."));
      this.settings.add(ModuleSetting.toggle("auto_reconnect", "sound_alert", "Alerte sonore", true).desc("Joue une alerte sonore lors de la deconnexion."));
   }
}
