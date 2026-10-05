package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.music.MusicState;
import dev.swiftclient.core.music.SpotifyManager;
import dev.swiftclient.core.ui.Px;

/**
 * Now playing, Swift style: a pocket player. A rounded cover with a play-state badge, the title, a live
 * equalizer that dances while a track plays, a smooth progress bar with a round playhead, and rounded
 * transport keys. The panel is translucent: the game stays visible behind it.
 */
public final class MusicElement extends HudElement {
   private static final int W = 190;
   private static final int BASE_H = 48;
   private static final int PAD = 6;
   private static final int COVER = 36;
   private static final int CTRL_H = 18;
   private static final int NEXT_H = 17;

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
      return BASE_H + (this.opt("controls", false) ? CTRL_H : 0) + (this.opt("nextsong", false) && SpotifyManager.hasNext() ? NEXT_H : 0);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      if (!this.moduleOn() && !MusicState.hasMusic()) {
         return;
      }
      // Kick the SMTC / Spotify pollers even before the first track arrives.
      MusicState.hasMusic();

      boolean has = MusicState.hasMusic();
      boolean cover = this.opt("cover", true);
      boolean showArtist = this.opt("artist", true);
      boolean showBar = has && this.opt("progress", true) && MusicState.durationMs() > 0L;
      boolean showTime = has && this.opt("time", true) && MusicState.durationMs() > 0L;
      boolean showCtrl = has && this.opt("controls", false);
      boolean showNext = has && this.opt("nextsong", false) && SpotifyManager.hasNext();
      int opacity = clamp255((int)Math.round(this.optValue("opacity", 70.0) / 100.0 * 255.0));
      int cTitle = this.optColor("col_title", -1);
      int cSub = this.optColor("col_sub", 0xFF9AA6BA);
      int cProg = this.optColor("col_prog", Px.ACCENT);
      int cBg = this.optColor("col_bg", 0xFF0A0D14);
      int bg = opacity << 24 | cBg & 0xFFFFFF;
      int h = BASE_H + (showCtrl ? CTRL_H : 0) + (showNext ? NEXT_H : 0);
      boolean playing = has && MusicState.playing();

      // The panel: rounded, see-through, with a thin accent line along the top edge
      c.card(x, y, W, h, bg, 0x26FFFFFF, 1, 9.0F);
      c.roundRect(x + 10, y + 1, W - 20, 2, 1.0F, cProg & 0x00FFFFFF | 0xCC000000);

      int textX = x + PAD;
      int top = y + 8;
      if (cover) {
         int cx = x + PAD;
         int cy = top;
         Object art = MusicState.artHandle();
         c.roundRect(cx - 1, cy - 1, COVER + 2, COVER + 2, 7.0F, 0x33FFFFFF);
         if (art != null && has) {
            c.roundRect(cx, cy, COVER, COVER, 6.0F, 0xFF000000);
            c.textureRegion(art, cx + 1, cy + 1, COVER - 2, COVER - 2, 0, 0, 100, 100, 100, 100);
         } else {
            c.roundRect(cx, cy, COVER, COVER, 6.0F, 0xFF1B2540);
            Px.zip(c, cx + 6, cy + 5, 1, false, System.currentTimeMillis());
         }
         // Play-state badge in the corner of the cover
         int bx = cx + COVER - 12;
         int by = cy + COVER - 12;
         c.roundRect(bx - 1, by - 1, 14, 14, 7.0F, 0xCC000000);
         c.roundRect(bx, by, 12, 12, 6.0F, playing ? Px.OK : cProg);
         if (playing) {
            c.roundRect(bx + 3, by + 3, 2, 6, 1.0F, 0xFF000000);
            c.roundRect(bx + 7, by + 3, 2, 6, 1.0F, 0xFF000000);
         } else {
            triRight(c, bx + 4, by + 3, 5, 0xFF000000);
         }
         textX = cx + COVER + 8;
      }

      int textW = x + W - PAD - textX;
      if (!has) {
         c.text("NOW PLAYING", textX, top + 6, cTitle, true);
         c.text("Waiting for media…", textX, top + 18, cSub, false);
         eq(c, x + W - PAD - 17, top, false, cProg);
         return;
      }

      // Equalizer in the top-right corner; the title yields to it
      int eqW = 17;
      eq(c, x + W - PAD - eqW, top, playing, cProg);
      boolean twoLines = showArtist && !MusicState.artist().isEmpty();
      c.text(fit(c, MusicState.title(), textW - eqW - 4), textX, twoLines ? top + 1 : top + 6, cTitle, true);
      if (twoLines) {
         c.text(fit(c, MusicState.artist(), textW), textX, top + 12, cSub, true);
      }

      if (showBar || showTime) {
         int rowY = top + COVER - 7;
         int bx = textX;
         int bw = textW;
         if (showTime) {
            String el = mmss(MusicState.positionMs());
            String to = mmss(MusicState.durationMs());
            int toW = c.textWidth(to);
            c.text(el, textX, rowY - 11, cSub, true);
            c.text(to, x + W - PAD - toW, rowY - 11, cSub, true);
         }
         if (showBar && bw > 8) {
            Px.segBar(c, bx, rowY, bw, 4, MusicState.progress(), cProg, 0);
            int fw = Math.round(bw * MusicState.progress());
            c.roundRect(Math.max(bx, bx + fw - 3), rowY - 2, 6, 8, 3.0F, 0xFFFFFFFF);
         }
      }

      if (showCtrl) {
         int cy = y + BASE_H - 3;
         int mid = x + W / 2;
         key(c, mid - 36, cy, 24, 14, cSub, 0);
         key(c, mid - 12, cy, 24, 14, cTitle, playing ? 3 : 1);
         key(c, mid + 12, cy, 24, 14, cSub, 2);
      }

      if (showNext) {
         int ny = y + BASE_H + (showCtrl ? CTRL_H : 0) - 3;
         c.fill(x + 8, ny, x + W - 8, ny + 1, 0x22FFFFFF);
         int tx = x + 8;
         Object na = SpotifyManager.nextArtHandle();
         if (na != null) {
            c.roundRect(tx, ny + 4, 11, 11, 3.0F, 0xFF000000);
            c.textureRegion(na, tx + 1, ny + 5, 9, 9, 0, 0, 100, 100, 100, 100);
            tx += 15;
         }
         Px.tag(c, tx, ny + 4, "NEXT", cProg);
         int lblW = c.textWidth("NEXT") + 12;
         String nt = SpotifyManager.nextTitle();
         String nar = SpotifyManager.nextArtist();
         if (!nar.isBlank()) {
            nt = nt + "  " + nar;
         }
         c.text(fit(c, nt, W - (tx - x) - 8 - lblW), tx + lblW, ny + 6, cSub, true);
      }
   }

   /** Four-bar equalizer: smooth, continuous motion while playing, resting low otherwise. */
   private static void eq(Canvas c, int x, int y, boolean playing, int color) {
      double t = System.currentTimeMillis() / 1000.0;
      for (int i = 0; i < 4; i++) {
         int hgt = playing ? 3 + (int)Math.round(8.0 * Math.abs(Math.sin(t * (2.6 + i * 0.9) + i * 1.7))) : 2;
         c.roundRect(x + i * 4, y + 11 - hgt, 3, hgt, 1.5F, color);
      }
   }

   /** Transport key: kind 0 prev, 1 play, 2 next, 3 pause. */
   private static void key(Canvas c, int x, int y, int w, int h, int col, int kind) {
      c.card(x, y, w, h, 0x2AFFFFFF, 0x1AFFFFFF, 1, 5.0F);
      int cx = x + w / 2;
      int cy = y + h / 2;
      switch (kind) {
         case 0 -> {
            c.roundRect(cx - 4, cy - 3, 2, 6, 1.0F, col);
            triLeft(c, cx - 2, cy - 3, 6, col);
         }
         case 1 -> triRight(c, cx - 2, cy - 3, 6, col);
         case 2 -> {
            triRight(c, cx - 4, cy - 3, 6, col);
            c.roundRect(cx + 3, cy - 3, 2, 6, 1.0F, col);
         }
         default -> {
            c.roundRect(cx - 3, cy - 3, 2, 6, 1.0F, col);
            c.roundRect(cx + 1, cy - 3, 2, 6, 1.0F, col);
         }
      }
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

   private static int clamp255(int v) {
      return v < 0 ? 0 : Math.min(v, 255);
   }

   private static String mmss(long ms) {
      long s = Math.max(0L, ms / 1000L);
      return s / 60L + ":" + String.format("%02d", s % 60L);
   }
}
