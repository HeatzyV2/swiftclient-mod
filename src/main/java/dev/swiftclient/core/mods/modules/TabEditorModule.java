package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

/** The player list (Tab): more columns, ping in milliseconds, slide-in animation. */
@ConsumedBy("PlayerTabOverlayMixin")
public final class TabEditorModule extends Module {
   public static final int PER_COLUMN = 20;
   public final ModuleSetting columns;
   public final ModuleSetting pingNumbers;
   public final ModuleSetting slide;

   public TabEditorModule() {
      super("tab_editor", "Tab Editor", "Reshape the player list: more columns, ping in ms, slide-in animation.", "World", "widertab", false);
      this.columns = this.slider("columns", "Max columns", 9.0, 4.0, 12.0, 1.0, "")
         .desc("How many columns the player list may use. Vanilla stops at 4, so big servers get cut.");
      this.pingNumbers = this.toggle("ping_numbers", "Ping in ms", true).desc("Write each player's latency in milliseconds instead of the signal bars.");
      this.slide = this.toggle("slide", "Slide animation", true).desc("The list slides in from the top when you press Tab.");
   }

   /** Maximum number of players shown in the tab list. */
   public long limit() {
      return Math.max(4, (int)Math.round(this.columns.value())) * (long)PER_COLUMN;
   }
}
