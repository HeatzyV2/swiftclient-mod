package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.music.MusicState;
import dev.swiftclient.core.music.SpotifyManager;
import dev.swiftclient.core.ui.Px;

/**
 * Now playing, Swift style: a pocket player. A framed cover on the left, the title in sign text, a live
 * pixel equalizer that dances while a track plays, a segmented progress bar with a square playhead, and
 * real chunky transport keys. Nothing is rounded, blurred or glowing.
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
      int opacity = clamp255((int)Math.round(this.optValue("opacity", 90.0) / 100.0 * 255.0));
      int cTitle = this.optColor("col_title", -1);
      int cSub = this.optColor("col_sub", 0xFF9AA6BA);
      int cProg = this.optColor("col_prog", Px.ACCENT);
      int cBg = this.optColor("col_bg", 0xFF0C0F16);
      int bg = Math.max(opacity, 0x80) << 24 | cBg & 0xFFFFFF;
      int h = BASE_H + (showCtrl ? CTRL_H : 0) + (showNext ? NEXT_H : 0);
      boolean playing = has && MusicState.playing();

      // The player body: ink outline, solid side, accent cap on top
      c.card(x, y, W, h, bg, 0xFF222B42, 1, 4.0F);
      c.fill(x + 3, y + 2, x + W - 3, y + 4, cProg);

      int textX = x + PAD;
      int top = y + 8;
      if (cover) {
         int cx = x + PAD;
         int cy = top;
         Object art = MusicState.artHandle();
         c.fill(cx - 1, cy - 1, cx + COVER + 1, cy + COVER + 1, Px.INK);
         c.fill(cx, cy, cx + COVER, cy + COVER, 0xFFFFFFFF);
         if (art != null && has) {
            c.textureRegion(art, cx + 1, cy + 1, COVER - 2, COVER - 2, 0, 0, 100, 100, 100, 100);
         } else {
            c.fill(cx + 1, cy + 1, cx + COVER - 1, cy + COVER - 1, 0xFF1B2540);
            Px.zip(c, cx + 6, cy + 5, 1, false, System.currentTimeMillis());
         }
         // Play-state badge in the corner of the cover
         int bx = cx + COVER - 11;
         int by = cy + COVER - 11;
         c.fill(bx, by, bx + 11, by + 11, Px.INK);
         c.fill(bx + 1, by + 1, bx + 10, by + 10, playing ? Px.OK : cProg);
         if (playing) {
            c.fill(bx + 3, by + 3, bx + 5, by + 8, Px.INK);
            c.fill(bx + 6, by + 3, bx + 8, by + 8, Px.INK);
         } else {
            triRight(c, bx + 4, by + 3, 5, Px.INK);
         }
         textX = cx + COVER + 8;
      }

      int textW = x + W - PAD - textX;
      if (!has) {
         c.text("NOW PLAYING", textX, top + 6, cTitle, true);
         c.text("Waiting for media…", textX, top + 18, cSub, false);
         eq(c, x + W - PAD - 18, top, 0, false, cProg);
         return;
      }

      // Equalizer in the top-right corner; the title yields to it
      int eqW = 18;
      eq(c, x + W - PAD - eqW, top, 0, playing, cProg);
      boolean twoLines = showArtist && !MusicState.artist().isEmpty();
      c.text(fit(c, MusicState.title(), textW - eqW - 4), textX, twoLines ? top + 1 : top + 6, cTitle, true);
      if (twoLines) {
         c.text(fit(c, MusicState.artist(), textW), textX, top + 12, cSub, false);
      }

      if (showBar || showTime) {
         int rowY = top + COVER - 8;
         int bx = textX;
         int bw = textW;
         if (showTime) {
            String el = mmss(MusicState.positionMs());
            String to = mmss(MusicState.durationMs());
            int toW = c.textWidth(to);
            c.text(el, textX, rowY - 11, cSub, false);
            c.text(to, x + W - PAD - toW, rowY - 11, cSub, false);
         }
         if (showBar && bw > 8) {
            Px.segBar(c, bx, rowY, bw, 6, MusicState.progress(), cProg, Math.max(8, bw / 5));
            int fw = Math.round(bw * MusicState.progress());
            c.fill(bx + fw - 2, rowY - 2, bx + fw + 2, rowY + 8, Px.INK);
            c.fill(bx + fw - 1, rowY - 1, bx + fw + 1, rowY + 7, Px.TEXT);
         }
      }

      if (showCtrl) {
         int cy = y + BASE_H - 2;
         int mid = x + W / 2;
         key(c, mid - 34, cy, 22, 14, cSub, 0);
         key(c, mid - 11, cy, 22, 14, cTitle, playing ? 3 : 1);
         key(c, mid + 12, cy, 22, 14, cSub, 2);
      }

      if (showNext) {
         int ny = y + BASE_H + (showCtrl ? CTRL_H : 0) - 3;
         c.fill(x + 6, ny, x + W - 6, ny + 2, 0xFF000000);
         int tx = x + 6;
         Object na = SpotifyManager.nextArtHandle();
         if (na != null) {
            c.fill(tx, ny + 4, tx + 11, ny + 15, Px.INK);
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
         c.text(fit(c, nt, W - (tx - x) - 6 - lblW), tx + lblW, ny + 6, cSub, false);
      }
   }

   /** Four-bar equalizer. Bars bounce in steps while playing and rest low otherwise. */
   private static void eq(Canvas c, int x, int y, int unused, boolean playing, int color) {
      long t = System.currentTimeMillis() / 110L;
      for (int i = 0; i < 4; i++) {
         int hgt = playing ? 3 + (int)((t * (3 + i * 2) + i * 5) % 9L) : 2;
         int bx = x + i * 5;
         c.fill(bx, y + 10 - hgt, bx + 4, y + 11, Px.INK);
         c.fill(bx + 1, y + 11 - hgt, bx + 3, y + 10, color);
      }
   }

   /** Transport key: kind 0 prev, 1 play, 2 next, 3 pause. */
   private static void key(Canvas c, int x, int y, int w, int h, int col, int kind) {
      c.card(x, y, w, h, 0xFF1D2535, 0, 0, 2.0F);
      int cx = x + w / 2;
      int cy = y + h / 2;
      switch (kind) {
         case 0 -> {
            c.fill(cx - 4, cy - 3, cx - 2, cy + 3, col);
            triLeft(c, cx - 2, cy - 3, 6, col);
         }
         case 1 -> triRight(c, cx - 2, cy - 3, 6, col);
         case 2 -> {
            triRight(c, cx - 4, cy - 3, 6, col);
            c.fill(cx + 3, cy - 3, cx + 5, cy + 3, col);
         }
         default -> {
            c.fill(cx - 3, cy - 3, cx - 1, cy + 3, col);
            c.fill(cx + 1, cy - 3, cx + 3, cy + 3, col);
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
