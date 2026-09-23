package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.music.SpotifyManager;

public final class MusicModule extends Module {
   public MusicModule() {
      super("hud_music", "Now playing", "Current track (Spotify, browser, any Windows media). Move it in the HUD Editor.", "HUD", "music", false);
      this.settings
         .add(ModuleSetting.toggle("hud_music", "cover", "Album cover", true).desc("Show the album artwork on the left of the panel.").group("Display"));
      this.settings.add(ModuleSetting.toggle("hud_music", "artist", "Show artist", true).desc("Show the artist under the track title.").group("Display"));
      this.settings.add(ModuleSetting.toggle("hud_music", "time", "Show time", true).desc("Show the elapsed time and the track length.").group("Display"));
      this.settings.add(ModuleSetting.toggle("hud_music", "progress", "Progress bar", true).desc("Draw a progress bar under the title.").group("Display"));
      this.settings
         .add(
            ModuleSetting.toggle("hud_music", "controls", "Show controls", false)
               .desc("Show previous / play / next buttons. They act on the player that is running.")
               .group("Display")
         );
      this.settings
         .add(
            ModuleSetting.toggle("hud_music", "nextsong", "Show next song (Spotify)", false)
               .desc("Show the next track in the queue. Spotify only, and needs the account linked below.")
               .group("Display")
         );
      this.settings
         .add(
            ModuleSetting.action(
                  "hud_music", "spotify", "Spotify account", () -> SpotifyManager.authorized() ? "Connected" : "Connect", SpotifyManager::connect
               )
               .desc("Link a Spotify account to read the queue and control playback.")
               .group("Account")
         );
      this.settings
         .add(ModuleSetting.toggle("hud_music", "glass", "Glass style", true).desc("Frosted background that blurs the scene behind the panel.").group("Style"));
      this.settings
         .add(
            ModuleSetting.slider("hud_music", "blur", "Glass blur", 60.0, 0.0, 100.0, 5.0, "%")
               .desc("How strongly the scene behind the panel is blurred.")
               .group("Style")
         );
      this.settings
         .add(
            ModuleSetting.slider("hud_music", "opacity", "Background opacity", 55.0, 0.0, 100.0, 5.0, "%")
               .desc("How opaque the panel background is.")
               .group("Style")
         );
      this.settings
         .add(
            ModuleSetting.slider("hud_music", "bloom", "Bloom", 20.0, 0.0, 100.0, 5.0, "%")
               .desc("Halo picked from the album artwork, spread around the panel.")
               .group("Style")
         );
      this.settings.add(ModuleSetting.color("hud_music", "col_title", "Title color", -1).desc("Colour of the track title.").group("Colors"));
      this.settings
         .add(ModuleSetting.color("hud_music", "col_sub", "Artist / time color", -6642766).desc("Colour of the artist and of the times.").group("Colors"));
      this.settings
         .add(ModuleSetting.color("hud_music", "col_prog", "Progress color", -12868259).desc("Colour of the filled part of the progress bar.").group("Colors"));
      this.settings.add(ModuleSetting.color("hud_music", "col_bg", "Background color", -15987700).desc("Colour of the panel background.").group("Colors"));
   }
}
