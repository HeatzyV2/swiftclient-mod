package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

@ConsumedBy("PlayerTabOverlayMixin")
public final class WiderTabModule extends Module {
   public static final int PAR_COLONNE = 20;
   public static final int COLONNES_DEFAUT = 9;
   public final ModuleSetting columns;

   public WiderTabModule() {
      super("widertab", "Wider Tab", "Show more players in the tab list (vanilla stops at 4 columns).", "Render", "widertab", false);
      this.columns = this.slider("columns", "Max columns", 9.0, 4.0, 12.0, 1.0, "")
         .desc("How many columns the player list may use before it starts scrolling.");
   }

   /** Maximum number of players shown in the tab list. */
   public long limit() {
      return Math.max(4, (int)Math.round(this.columns.value())) * (long)PAR_COLONNE;
   }
}
