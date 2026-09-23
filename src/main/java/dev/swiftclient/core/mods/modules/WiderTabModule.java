package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;

public final class WiderTabModule extends Module {
   public static final int PAR_COLONNE = 20;
   public static final int COLONNES_DEFAUT = 9;
   public static final int WIDE_LIMIT = 180;

   public WiderTabModule() {
      super("widertab", "Wider Tab", "Show more players in the tab list (vanilla stops at 4 columns).", "Render", "widertab", false);
      this.settings
         .add(
            ModuleSetting.slider("widertab", "columns", "Max columns", 9.0, 4.0, 12.0, 1.0, "")
               .desc("How many columns the player list may use before it starts scrolling.")
         );
   }

   public static long limite() {
      Module m = ModuleManager.byId("widertab");
      if (m == null) {
         return 180L;
      } else {
         ModuleSetting s = m.setting("columns");
         int colonnes = s == null ? 9 : (int)Math.round(s.value());
         if (colonnes < 4) {
            colonnes = 4;
         }

         return colonnes * 20L;
      }
   }
}
