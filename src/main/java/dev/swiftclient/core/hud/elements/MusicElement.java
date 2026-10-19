package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.music.MusicLyrics;
import dev.swiftclient.core.music.MusicState;
import dev.swiftclient.core.music.SpotifyManager;
import dev.swiftclient.core.music.lyrics.Lyrics;
import dev.swiftclient.core.music.lyrics.SyncedLyrics;
import dev.swiftclient.core.ui.Px;
import java.util.Locale;

/**
 * Now playing, as a compact card: the cover on the left; "Now playing", a scrolling title and the artist
 * on the right, with the time, the transport keys and a thin progress bar. Below, the synced lyrics
 * scroll smoothly line by line, the current line filling up like a karaoke screen.
 */
public final class MusicElement extends HudElement {
   private static final int W = 180;
   private static final int PAD = 6;
   private static final int ART = 42;
   private static final int LYRIC_LH = 11;
   private static final int NEXT_H = 15;
   private static final float SMALL = 0.7F;
   private static final float MARQUEE_SPEED = 22.0F;
   private static final float MARQUEE_PAUSE = 1.6F;
   private static final int MARQUEE_GAP = 28;
   /** Lyric line the view is scrolled to, eased towards the line being sung. */
   private float scroll = Float.NaN;
   private Object scrollSource;
   private long lastFrame;
   private float titleClock;
   private String titleShown = "";

   public MusicElement() {
      super("music", "Now playing");
   }

   @Override
   public String icon() {
      return "music";
   }

   @Override
   public boolean drawsWithoutData() {
      return true;
   }

   private boolean moduleOn() {
      return ModuleManager.active(this.moduleId());
   }

   @Override
   public int width(Canvas c, HudData d) {
      // Always reserve space when the module is on, otherwise the editor treats the element as missing.
      return this.moduleOn() || MusicState.hasMusic() ? W : 0;
   }

   @Override
   public int height(Canvas c) {
      if (!this.moduleOn() && !MusicState.hasMusic()) {
         return 0;
      }

      return this.headerHeight() + this.lyricsHeight() + (this.showNext() ? NEXT_H : 0);
   }

   private int headerHeight() {
      return PAD + ART + PAD;
   }

   private boolean showNext() {
      return MusicState.hasMusic() && this.opt("nextsong", false) && SpotifyManager.hasNext();
   }

   private int lyricLines() {
      return this.opt("lyrics", false) && MusicState.hasMusic() ? Math.max(1, Math.min(3, (int)Math.round(this.optValue("lyric_lines", 2.0)))) : 0;
   }

   private int lyricsHeight() {
      int n = this.lyricLines();
      return n == 0 ? 0 : n * LYRIC_LH + 4;
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      if (!this.moduleOn() && !MusicState.hasMusic()) {
         return;
      }

      long nowNs = System.nanoTime();
      float dt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (nowNs - this.lastFrame) / 1.0E9F);
      this.lastFrame = nowNs;
      boolean has = MusicState.hasMusic();
      boolean playing = has && MusicState.playing();
      int opacity = clamp255((int)Math.round(this.optValue("opacity", 70.0) / 100.0 * 255.0));
      int cTitle = this.optColor("col_title", -1);
      int cSub = this.optColor("col_sub", 0xFF9AA6BA);
      int cProg = this.optColor("col_prog", Px.ACCENT);
      int cBg = this.optColor("col_bg", 0xFF0A0D14);
      int h = this.height(c);
      c.card(x, y, W, h, opacity << 24 | cBg & 0xFFFFFF, 0x1FFFFFFF, 1, 7.0F);

      boolean cover = this.opt("cover", true);
      int left = x + PAD;
      if (cover) {
         this.drawCover(c, left, y + PAD, has, playing, cProg);
         left += ART + 7;
      }

      int column = x + W - PAD - left;
      int top = y + PAD;
      small(c, has ? (playing ? "NOW PLAYING" : "PAUSED") : "NOW PLAYING", left, top + 1, withAlpha(cSub, 0.8F));
      if (playing) {
         this.equalizer(c, x + W - PAD - 11, top, cProg);
      }

      if (!has) {
         c.text("Waiting for media…", left, top + 12, cSub, true);
         return;
      }

      // Title (scrolls when it is too long), then the artist.
      String title = MusicState.title();
      if (!title.equals(this.titleShown)) {
         this.titleShown = title;
         this.titleClock = 0.0F;
      }

      if (playing) {
         this.titleClock += dt;
      }

      this.marquee(c, title, left, top + 9, column, cTitle, this.titleClock);
      boolean showArtist = this.opt("artist", true) && !MusicState.artist().isEmpty();
      if (showArtist) {
         c.text(fit(c, MusicState.artist(), column), left, top + 20, cSub, true);
      }

      // Time, transport keys, progress bar.
      long dur = MusicState.durationMs();
      int rowY = y + PAD + ART - 9;
      if (this.opt("time", true) && dur > 0L) {
         long pos = MusicState.positionMs();
         small(c, mmss(pos), left, rowY, cSub);
         String rest = "-" + mmss(Math.max(0L, dur - pos));
         int restW = Math.round(c.textWidth(rest) * SMALL);
         small(c, rest, x + W - PAD - restW, rowY, cSub);
      }

      if (this.opt("controls", true)) {
         transport(c, left + column / 2, rowY + 2, playing, cTitle, cSub);
      }

      if (this.opt("progress", true) && dur > 0L) {
         int barY = y + PAD + ART - 2;
         c.roundRect(left, barY, column, 2, 1.0F, 0x26FFFFFF);
         int fill = Math.round(column * MusicState.progress());
         if (fill > 0) {
            c.roundRect(left, barY, fill, 2, 1.0F, playing ? cProg : cSub);
         }
      }

      int below = y + this.headerHeight();
      if (this.lyricLines() > 0) {
         this.drawLyrics(c, x + PAD, below, W - PAD * 2, this.lyricLines(), dt, this.optColor("col_lyric", -1), cSub, cProg);
         below += this.lyricsHeight();
      }

      if (this.showNext()) {
         c.fill(x + 8, below, x + W - 8, below + 1, 0x1AFFFFFF);
         int tx = x + PAD;
         small(c, "NEXT", tx, below + 5, cProg);
         int lblW = Math.round(c.textWidth("NEXT") * SMALL) + 5;
         String nt = SpotifyManager.nextTitle();
         String nar = SpotifyManager.nextArtist();
         if (!nar.isBlank()) {
            nt = nt + " · " + nar;
         }

         small(c, fitScaled(c, nt, W - PAD * 2 - lblW), tx + lblW, below + 5, cSub);
      }
   }

   private void drawCover(Canvas c, int x, int y, boolean has, boolean playing, int accent) {
      Object art = MusicState.artHandle();
      if (art != null && has) {
         c.roundRect(x, y, ART, ART, 5.0F, 0xFF000000);
         c.textureRegion(art, x, y, ART, ART, 0, 0, 100, 100, 100, 100);
      } else {
         c.roundRect(x, y, ART, ART, 5.0F, 0x1AFFFFFF);
         // Three resting bars, like Anubis' empty cover.
         int bar = 3;
         int gap = 3;
         int base = y + Math.round(ART * 0.64F);
         int l = x + (ART - bar * 3 - gap * 2) / 2;
         float[] heights = {0.2F, 0.34F, 0.26F};
         for (int i = 0; i < 3; i++) {
            int bh = Math.round(ART * heights[i]);
            c.roundRect(l + i * (bar + gap), base - bh, bar, bh, 1.0F, playing ? withAlpha(accent, 0.7F) : 0x66FFFFFF);
         }
      }

      c.card(x, y, ART, ART, 0, 0x1FFFFFFF, 1, 5.0F);
   }

   /** Title scrolling left when it does not fit, with a pause at the start of each loop. */
   private void marquee(Canvas c, String text, int x, int y, int avail, int color, float clock) {
      int tw = c.textWidth(text);
      if (tw <= avail) {
         c.text(text, x, y, color, true);
         return;
      }

      float loop = (tw + MARQUEE_GAP) / MARQUEE_SPEED;
      float t = clock % (loop + MARQUEE_PAUSE);
      int offset = t <= MARQUEE_PAUSE ? 0 : Math.round((t - MARQUEE_PAUSE) * MARQUEE_SPEED);
      c.pushScissor(x, y - 1, avail, c.lineHeight() + 2);
      c.text(text, x - offset, y, color, true);
      c.text(text, x - offset + tw + MARQUEE_GAP, y, color, true);
      c.popScissor();
   }

   /**
    * The lyrics view: lines slide up as the song goes on; the line being sung is bright and fills from
    * left to right, the next ones wait dimmer below.
    */
   private void drawLyrics(Canvas c, int x, int y, int w, int lines, float dt, int cLit, int cSub, int accent) {
      Lyrics lyrics = MusicLyrics.current();
      int areaH = lines * LYRIC_LH;
      if (lyrics == null || lyrics.status() != Lyrics.Status.SYNCED) {
         this.scrollSource = null;
         String msg = lyrics == null ? "" : switch (lyrics.status()) {
            case LOADING -> "Looking for lyrics…";
            case PLAIN -> "No synced lyrics for this song";
            case INSTRUMENTAL -> "♪ Instrumental";
            case UNAVAILABLE -> "Lyrics unavailable";
            default -> "No lyrics found";
         };
         small(c, msg, x, y + 3, withAlpha(cSub, 0.8F));
         return;
      }

      SyncedLyrics synced = lyrics.synced();
      long pos = MusicLyrics.position();
      int current = synced.indexAt(pos);
      float target = Math.max(0, current);
      if (synced != this.scrollSource || Float.isNaN(this.scroll) || Math.abs(target - this.scroll) > 3.0F) {
         this.scrollSource = synced;
         this.scroll = target;
      } else {
         this.scroll += (target - this.scroll) * (1.0F - (float)Math.exp(-12.0F * dt));
         if (Math.abs(target - this.scroll) < 0.002F) {
            this.scroll = target;
         }
      }

      c.pushScissor(x, y, w, areaH + 2);
      int first = (int)Math.floor(this.scroll) - 1;
      for (int i = Math.max(0, first); i < synced.size() && i <= first + lines + 2; i++) {
         float rel = i - this.scroll;
         int ly = y + 1 + Math.round(rel * LYRIC_LH);
         if (ly < y - LYRIC_LH || ly > y + areaH) {
            continue;
         }

         // Fade out what scrolls away above, and the bottom of the view.
         float alpha = rel < 0.0F ? Math.max(0.0F, 1.0F + rel) * 0.45F : 1.0F;
         if (rel > lines - 1) {
            alpha *= Math.max(0.0F, lines - rel);
         }

         String text = synced.line(i).text();
         if (text.isBlank()) {
            if (i == current) {
               this.breathingDots(c, x, ly + 3, accent);
            }

            continue;
         }

         String shown = fit(c, text, w);
         if (i == current) {
            int mode = this.optCycle("lyric_highlight", 0);
            int lit = this.optColor("col_highlight", 0xFF6FA8FF);
            if (mode == 0) {
               // Karaoke: the line lights up as it is sung.
               int lineW = c.textWidth(shown);
               c.text(shown, x, ly, withAlpha(cLit, 0.9F * alpha), true);
               int fill = Math.round(lineW * this.lineProgress(synced, i, pos));
               if (fill > 0) {
                  c.pushScissor(x, ly - 1, fill, LYRIC_LH);
                  c.text(shown, x, ly, withAlpha(lit, alpha), true);
                  c.popScissor();
               }
            } else {
               c.text(shown, x, ly, withAlpha(mode == 1 ? lit : cLit, alpha), true);
            }
         } else {
            float dim = i < current ? 0.5F : (i == current + 1 ? 0.7F : 0.45F);
            c.text(shown, x, ly, withAlpha(cSub, dim * alpha), true);
         }
      }

      c.popScissor();
   }

   /** How far the line being sung is, 0..1 (until the next line starts, at most 6 s). */
   private float lineProgress(SyncedLyrics synced, int index, long pos) {
      long start = synced.line(index).timeMs();
      long end = index + 1 < synced.size() ? synced.line(index + 1).timeMs() : start + 4000L;
      end = Math.min(end, start + 6000L);
      if (end <= start) {
         return 1.0F;
      }

      float t = (float)(pos - start) / (float)(end - start);
      return t < 0.0F ? 0.0F : Math.min(1.0F, t * 1.15F);
   }

   private void breathingDots(Canvas c, int x, int y, int accent) {
      double t = System.currentTimeMillis() / 1000.0;
      for (int i = 0; i < 3; i++) {
         float a = 0.35F + 0.65F * (float)Math.abs(Math.sin(t * 2.2 + i * 0.6));
         c.roundRect(x + i * 6, y, 3, 3, 1.5F, withAlpha(accent, a));
      }
   }

   /** Four little bars dancing while a track plays. */
   private void equalizer(Canvas c, int x, int y, int color) {
      double t = System.currentTimeMillis() / 1000.0;
      for (int i = 0; i < 4; i++) {
         int hgt = 2 + (int)Math.round(4.0 * Math.abs(Math.sin(t * (2.6 + i * 0.9) + i * 1.7)));
         c.fill(x + i * 3, y + 6 - hgt, x + i * 3 + 2, y + 6, color);
      }
   }

   /** Previous / play-pause / next, drawn as glyphs centred at (cx, cy). */
   private static void transport(Canvas c, int cx, int cy, boolean playing, int main, int side) {
      int s = 7;
      // previous: bar, then a triangle pointing left
      int px = cx - 18;
      c.fill(px, cy - 3, px + 2, cy + 4, side);
      triLeft(c, px + 3, cy - 3, s, side);
      // play / pause
      if (playing) {
         c.fill(cx - 3, cy - 3, cx - 1, cy + 4, main);
         c.fill(cx + 1, cy - 3, cx + 3, cy + 4, main);
      } else {
         triRight(c, cx - 2, cy - 3, s, main);
      }

      // next: a triangle pointing right, then a bar
      int nx = cx + 8;
      triRight(c, nx, cy - 3, s, side);
      c.fill(nx + s + 1, cy - 3, nx + s + 3, cy + 4, side);
   }

   private static void small(Canvas c, String s, int x, int y, int argb) {
      c.pushScale(x, y, SMALL);
      c.text(s, 0, 0, argb, false);
      c.popScale();
   }

   private static String fitScaled(Canvas c, String s, int maxW) {
      return fit(c, s, Math.round(maxW / SMALL));
   }

   @Override
   public String[] previewLines() {
      return new String[]{"Now playing", "Waiting for media…"};
   }

   private static void triRight(Canvas c, int x, int y, int s, int col) {
      for (int ry = 0; ry < s; ry++) {
         int w = Math.round(s * (1.0F - Math.abs(ry - (s - 1) / 2.0F) / (s / 2.0F)));
         if (w > 0) {
            c.fill(x, y + ry, x + w, y + ry + 1, col);
         }
      }
   }

   private static void triLeft(Canvas c, int x, int y, int s, int col) {
      for (int ry = 0; ry < s; ry++) {
         int w = Math.round(s * (1.0F - Math.abs(ry - (s - 1) / 2.0F) / (s / 2.0F)));
         if (w > 0) {
            c.fill(x + s - w, y + ry, x + s, y + ry + 1, col);
         }
      }
   }

   private static String fit(Canvas c, String s, int maxW) {
      if (s == null) {
         return "";
      } else if (c.textWidth(s) <= maxW) {
         return s;
      } else {
         int ew = c.textWidth("…");
         StringBuilder sb = new StringBuilder();
         for (int i = 0; i < s.length() && c.textWidth(sb.toString() + s.charAt(i)) + ew <= maxW; i++) {
            sb.append(s.charAt(i));
         }

         return sb.append("…").toString();
      }
   }

   private static int withAlpha(int argb, float k) {
      int a = Math.round((argb >>> 24) * Math.max(0.0F, Math.min(1.0F, k)));
      return a << 24 | argb & 0xFFFFFF;
   }

   private static int clamp255(int v) {
      return v < 0 ? 0 : Math.min(v, 255);
   }

   private static String mmss(long ms) {
      long s = Math.max(0L, ms / 1000L);
      return s / 60L + ":" + String.format(Locale.ROOT, "%02d", s % 60L);
   }
}
