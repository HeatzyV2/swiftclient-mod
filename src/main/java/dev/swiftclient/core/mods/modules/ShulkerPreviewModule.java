package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

public final class ShulkerPreviewModule extends Module {
   public ShulkerPreviewModule() {
      super("shulker_preview", "Shulker Preview", "Affiche le contenu des boites Shulker et coffres au survol dans l'inventaire.", "Utility", "package", true);
      this.settings.add(ModuleSetting.toggle("shulker_preview", "show_empty", "Afficher les slots vides", false).desc("Affiche les slots vides de la boite Shulker."));
      this.settings.add(ModuleSetting.toggle("shulker_preview", "colored_bg", "Fond colore dynamique", true).desc("Adapte la couleur de fond a la couleur de la Shulker Box."));
   }
}
