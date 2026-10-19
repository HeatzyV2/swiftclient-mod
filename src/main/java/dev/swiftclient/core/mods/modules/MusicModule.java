package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.music.SpotifyManager;
import dev.swiftclient.core.music.WindowsSmtc;

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
            ModuleSetting.toggle("hud_music", "controls", "Show controls", true)
               .desc("Show the previous / play / next keys next to the time.")
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
            ModuleSetting.toggle("hud_music", "lyrics", "Synced lyrics", false)
               .desc("Show the lyrics line by line, in time with the song (from LRCLIB, when the song is known there).")
               .group("Lyrics")
         );
      this.settings
         .add(ModuleSetting.slider("hud_music", "lyric_lines", "Lyric lines", 2.0, 1.0, 3.0, 1.0, "").desc("The current line, then the next ones.").group("Lyrics"));
      this.settings
         .add(
            ModuleSetting.cycle("hud_music", "lyric_highlight", "Lyric highlight", new String[]{"Karaoke", "Line", "Off"}, 0)
               .desc("Karaoke: the sung line lights up as it goes. Line: the whole line is lit. Off: plain white.")
               .group("Lyrics")
         );
      this.settings
         .add(ModuleSetting.color("hud_music", "col_highlight", "Highlight color", 0xFF6FA8FF).desc("Colour of the lit part of the line being sung.").group("Lyrics"));
      this.settings
         .add(ModuleSetting.color("hud_music", "col_lyric", "Lyrics color", -1).desc("Colour of the line being sung when the highlight is Off.").group("Lyrics"));
      this.settings
         .add(
            ModuleSetting.action(
                  "hud_music", "spotify", "Spotify account", () -> SpotifyManager.authorized() ? Tr.of("swift.music.connected") : Tr.of("swift.music.connect"), SpotifyManager::connect
               )
               .desc("Link a Spotify account to read the queue and control playback.")
               .group("Account")
         );
      this.settings
         .add(
            ModuleSetting.slider("hud_music", "opacity", "Background opacity", 70.0, 0.0, 100.0, 5.0, "%")
               .desc("How opaque the panel background is.")
               .group("Style")
         );
      this.settings.add(ModuleSetting.color("hud_music", "col_title", "Title color", -1).desc("Colour of the track title.").group("Colors"));
      this.settings
         .add(ModuleSetting.color("hud_music", "col_sub", "Artist / time color", 0xFF9AA6BA).desc("Colour of the artist and of the times.").group("Colors"));
      this.settings
         .add(ModuleSetting.color("hud_music", "col_prog", "Progress color", -12868259).desc("Colour of the filled part of the progress bar.").group("Colors"));
      this.settings.add(ModuleSetting.color("hud_music", "col_bg", "Background color", -15987700).desc("Colour of the panel background.").group("Colors"));
   }

   // The Windows media reader (one PowerShell process) only runs while the module is on.
   @Override
   protected void onEnable() {
      WindowsSmtc.setActive(true);
   }

   @Override
   protected void onTick() {
      // Restarts the reader (with backoff) if the process died.
      WindowsSmtc.setActive(true);
   }

   @Override
   protected void onDisable() {
      WindowsSmtc.setActive(false);
   }
}
