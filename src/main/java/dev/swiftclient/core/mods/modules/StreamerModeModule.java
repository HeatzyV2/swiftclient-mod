package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class StreamerModeModule extends Module {
   public StreamerModeModule() {
      super("streamer_mode", "Streamer Mode", "Masque votre pseudo, les skins et les adresses IP pour streamer en securite.", "Utility", "users", false);
      this.settings.add(ModuleSetting.cycle("streamer_mode", "fake_name", "Pseudo de remplacement", new String[]{"SwiftPlayer", "You", "Player", "Anonyme"}, 0).desc("Nom affiche a la place de votre vrai pseudo."));
      this.settings.add(ModuleSetting.toggle("streamer_mode", "hide_ips", "Masquer les adresses IP", true).desc("Cache les IP de serveurs dans les menus."));
      this.settings.add(ModuleSetting.toggle("streamer_mode", "hide_skins", "Skins par defaut", false).desc("Remplace les skins par Steve/Alex pour eviter les trolls."));
   }
}
