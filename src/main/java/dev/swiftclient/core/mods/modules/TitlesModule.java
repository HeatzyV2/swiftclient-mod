package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

/** The big titles servers put in the middle of the screen: smaller, moved up, or hidden. */
@ConsumedBy("HudOverlaysMixin")
public final class TitlesModule extends Module {
   public final ModuleSetting scale;
   public final ModuleSetting offset;
   public final ModuleSetting hideTitles;
   public final ModuleSetting hideSubtitles;

   public TitlesModule() {
      super("titles", "Titles", "The big titles servers show in the middle of the screen: smaller, higher, or hidden.", "HUD", "titles", false);
      this.scale = this.slider("scale", "Size", 60.0, 25.0, 100.0, 5.0, "%").desc("Size of titles and subtitles. 100% is vanilla.");
      this.offset = this.slider("offset", "Height", 25.0, 0.0, 45.0, 5.0, "%").desc("How far above the centre of the screen they appear.");
      this.hideTitles = this.toggle("hide_titles", "Hide titles", false).desc("Never show the big title.");
      this.hideSubtitles = this.toggle("hide_subtitles", "Hide subtitles", false).desc("Never show the smaller line under it.");
   }
}
