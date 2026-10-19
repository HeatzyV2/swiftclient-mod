package dev.swiftclient.core.music;

import dev.swiftclient.core.music.lyrics.Lyrics;
import dev.swiftclient.core.music.lyrics.LyricsQuery;
import dev.swiftclient.core.music.lyrics.LyricsService;

/**
 * Synced lyrics of the track playing, from LRCLIB (free, no account), for the Now Playing element. The
 * service only starts the first time lyrics are asked for, and caches what it found on disk.
 */
public final class MusicLyrics {
   /** Lines switch a little early, like a karaoke screen: you read the line as it is sung. */
   public static final long LEAD_MS = 200L;
   private static LyricsService service;
   private static String lastKey = "";
   private static LyricsQuery lastQuery;

   private MusicLyrics() {
   }

   /** The lyrics of the track playing (LOADING while they are looked up), or null without music. */
   public static synchronized Lyrics current() {
      if (!MusicState.hasMusic()) {
         return null;
      }

      if (service == null) {
         service = new LyricsService();
      }

      String key = MusicState.title() + "\n" + MusicState.artist() + "\n" + MusicState.durationMs() / 1000L;
      if (!key.equals(lastKey) || lastQuery == null) {
         lastKey = key;
         lastQuery = new LyricsQuery(MusicState.title(), MusicState.artist(), "", 0).withDurationMillis(MusicState.durationMs());
      }

      return service.lookup(lastQuery);
   }

   /** Where the lyrics are: the playback position plus the karaoke lead. */
   public static long position() {
      return MusicState.positionMs() + LEAD_MS;
   }
}
