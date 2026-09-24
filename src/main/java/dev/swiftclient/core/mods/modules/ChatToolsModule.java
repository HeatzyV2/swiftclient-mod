package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class ChatToolsModule extends Module {
   public ChatToolsModule() {
      super("chat_tools", "Chat Tools", "Copier du chat en 1 clic, historique persistant et alerte sonore sur mention.", "Utility", "list", false);
      // No behaviour yet: nothing reads "chat_tools". Hidden until implemented.
      this.notImplemented();
      this.settings.add(ModuleSetting.toggle("chat_tools", "copy_button", "Bouton copier le message", true).desc("Clic pour copier un message dans le presse-papier."));
      this.settings.add(ModuleSetting.toggle("chat_tools", "mention_sound", "Son sur mention", true).desc("Joue un tintement discret quand quelqu'un ecrit votre pseudo."));
      this.settings.add(ModuleSetting.toggle("chat_tools", "infinite_history", "Historique persistant", true).desc("Conserve l'historique du chat lors d'un changement de serveur."));
   }
}
