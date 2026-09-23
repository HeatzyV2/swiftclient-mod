package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.music.MusicState;
import dev.swiftclient.core.music.SpotifyManager;

public final class MusicElement extends HudElement {
   private static final int W = 175;
   private static final int BASE_H = 44;
   private static final int PAD = 6;
   private static final int COVER = 32;
   private static final int PROG_H = 3;
   private static final int CTRL_H = 14;
   private static final int NEXT_H = 17;
   private static final int BORDER_SOLID = 352321535;
   private static final int BORDER_GLASS = 872415231;
   private static final int COVER_TOP = -14013910;
   private static final int COVER_BOT = -15461356;

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
      // Always reserve space when the module is on — otherwise the editor/HUD
      // treat the element as missing (0×0) until a track is detected.
      return this.moduleOn() || MusicState.hasMusic() ? 175 : 0;
   }

   @Override
   public int height(Canvas c) {
      if (!this.moduleOn() && !MusicState.hasMusic()) {
         return 0;
      }
      return 44 + (this.opt("controls", false) ? 14 : 0) + (this.opt("nextsong", false) && SpotifyManager.hasNext() ? 17 : 0);
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      if (!this.moduleOn() && !MusicState.hasMusic()) {
         return;
      }
      // Kick the SMTC / Spotify pollers even before the first track arrives.
      MusicState.hasMusic();

      if (!MusicState.hasMusic()) {
         this.drawPlaceholder(c, x, y);
         return;
      }

      boolean cover = this.opt("cover", true);
      boolean showArtist = this.opt("artist", true);
      boolean showBar = this.opt("progress", true) && MusicState.durationMs() > 0L;
      boolean showTime = this.opt("time", true) && MusicState.durationMs() > 0L;
      boolean showCtrl = this.opt("controls", false);
      boolean showNext = this.opt("nextsong", false) && SpotifyManager.hasNext();
      boolean glass = this.opt("glass", true);
      float bloom = (float)(this.optValue("bloom", 20.0) / 100.0);
      int opacity = clamp255((int)Math.round(this.optValue("opacity", 55.0) / 100.0 * 255.0));
      int cTitle = this.optColor("col_title", -1);
      int cSub = this.optColor("col_sub", -6642766);
      int cProg = this.optColor("col_prog", -12868259);
      int cBg = this.optColor("col_bg", -15987700);
      int bg = opacity << 24 | cBg & 16777215;
      int progBg = 637534208 | cSub & 16777215;
      int border = glass ? 872415231 : 352321535;
      int H = 44 + (showCtrl ? 14 : 0) + (showNext ? 17 : 0);
      if (bloom > 0.001F) {
         for (int i = 3; i >= 1; i--) {
            int grow = i * 3;
            int a = (int)(bloom * 34.0F / i);
            c.roundRect(x - grow, y - grow, 175 + 2 * grow, H + 2 * grow, 8.0F + grow, clamp255(a) << 24 | cProg & 16777215);
         }
      }

      int blurPx = (int)Math.round(this.optValue("blur", 60.0) / 100.0 * 24.0);
      boolean glassDrawn = glass && c.glassRect(x, y, 175, H, 6.0F, bg, blurPx);
      if (glassDrawn) {
         c.card(x, y, 175, H, 0, border, 1, 6.0F);
         c.gradientV(x + 1, y + 1, 173, H / 2, 352321535, 16777215);
      } else {
         c.card(x, y, 175, H, bg, border, 1, 6.0F);
         if (glass) {
            c.gradientV(x + 2, y + 2, 171, H / 2, 318767103, 16777215);
         }
      }

      int textX = x + 6;
      if (cover) {
         int cx = x + 6;
         int cy = y + 6;
         Object art = MusicState.artHandle();
         if (art != null) {
            c.roundRect(cx, cy, 32, 32, 4.0F, -16777216);
            c.textureRegion(art, cx, cy, 32, 32, 0, 0, 100, 100, 100, 100);
         } else {
            c.roundRect(cx, cy, 32, 32, 4.0F, -15461356);
            c.gradientV(cx + 1, cy + 1, 30, 30, -14013910, -15461356);
            c.roundRect(cx + 16 - 6, cy + 16 - 6, 12, 12, 6.0F, 788529151);
         }

         textX = cx + 32 + 6;
      }

      int textW = 175 - (textX - x) - 6;
      boolean twoLines = showArtist && !MusicState.artist().isEmpty();
      c.text(fit(c, MusicState.title(), textW), textX, twoLines ? y + 8 : y + 12, cTitle, true);
      if (twoLines) {
         c.text(fit(c, MusicState.artist(), textW), textX, y + 19, cSub, true);
      }

      if (showBar || showTime) {
         int rowY = y + 44 - 6 - 3;
         int bx = textX;
         int bw = textW;
         if (showTime) {
            String el = mmss(MusicState.positionMs());
            String to = mmss(MusicState.durationMs());
            int elW = sw(c, el);
            int toW = sw(c, to);
            drawSmall(c, el, textX, rowY - 2, cSub, 0.72F);
            drawSmall(c, to, x + 175 - 6 - toW, rowY - 2, cSub, 0.72F);
            bx = textX + elW + 4;
            bw = x + 175 - 6 - toW - 4 - bx;
         }

         if (showBar && bw > 4) {
            c.roundRect(bx, rowY, bw, 3, 1.5F, progBg);
            int fw = Math.round(bw * MusicState.progress());
            if (fw > 0) {
               c.roundRect(bx, rowY, fw, 3, 1.5F, cProg);
            }
         }
      }

      if (showCtrl) {
         int cy = y + 44 + 3;
         int mid = x + 87;
         drawPrev(c, mid - 22, cy, cSub);
         if (MusicState.playing()) {
            drawPause(c, mid - 3, cy, cTitle);
         } else {
            triRight(c, mid - 3, cy, 8, cTitle);
         }

         drawNext(c, mid + 14, cy, cSub);
      }

      if (showNext) {
         int ny = y + 44 + (showCtrl ? 14 : 0);
         c.fill(x + 6, ny, x + 175 - 6, ny + 1, 352321535);
         int tx = x + 6;
         Object na = SpotifyManager.nextArtHandle();
         if (na != null) {
            c.roundRect(tx, ny + 3, 11, 11, 2.0F, -16777216);
            c.textureRegion(na, tx, ny + 3, 11, 11, 0, 0, 100, 100, 100, 100);
            tx += 15;
         }

         drawSmall(c, "NEXT", tx, ny + 4, cProg, 0.7F);
         int lblW = (int)Math.ceil(c.textWidth("NEXT") * 0.7F) + 5;
         String nt = SpotifyManager.nextTitle();
         String nar = SpotifyManager.nextArtist();
         if (!nar.isBlank()) {
            nt = nt + "  " + nar;
         }

         c.text(fit(c, nt, 175 - (tx - x) - 6 - lblW), tx + lblW, ny + 5, cSub, true);
      }
   }

   /** Visible idle state so the element is selectable in the HUD editor. */
   private void drawPlaceholder(Canvas c, int x, int y) {
      boolean glass = this.opt("glass", true);
      int opacity = clamp255((int)Math.round(this.optValue("opacity", 55.0) / 100.0 * 255.0));
      int cBg = this.optColor("col_bg", -15987700);
      int cTitle = this.optColor("col_title", -1);
      int cSub = this.optColor("col_sub", -6642766);
      int bg = opacity << 24 | cBg & 16777215;
      int border = glass ? 872415231 : 352321535;
      int H = 44;
      int blurPx = (int)Math.round(this.optValue("blur", 60.0) / 100.0 * 24.0);
      boolean glassDrawn = glass && c.glassRect(x, y, 175, H, 6.0F, bg, blurPx);
      if (glassDrawn) {
         c.card(x, y, 175, H, 0, border, 1, 6.0F);
      } else {
         c.card(x, y, 175, H, bg, border, 1, 6.0F);
      }
      c.roundRect(x + 6, y + 6, 32, 32, 4.0F, -15461356);
      c.gradientV(x + 7, y + 7, 30, 30, -14013910, -15461356);
      c.roundRect(x + 16, y + 16, 12, 12, 6.0F, 788529151);
      c.text("Now playing", x + 44, y + 10, cTitle, true);
      c.text("Waiting for media…", x + 44, y + 22, cSub, true);
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

   private static void drawPause(Canvas c, int x, int y, int col) {
      c.roundRect(x, y, 2, 8, 1.0F, col);
      c.roundRect(x + 5, y, 2, 8, 1.0F, col);
   }

   private static void drawPrev(Canvas c, int x, int y, int col) {
      c.roundRect(x, y, 2, 8, 1.0F, col);
      triLeft(c, x + 3, y, 7, col);
   }

   private static void drawNext(Canvas c, int x, int y, int col) {
      triRight(c, x, y, 7, col);
      c.roundRect(x + 8, y, 2, 8, 1.0F, col);
   }

   private static int sw(Canvas c, String s) {
      return (int)Math.ceil(c.textWidth(s) * 0.72F);
   }

   private static void drawSmall(Canvas c, String s, int x, int y, int argb, float scale) {
      c.pushScale(x, y, scale);
      c.text(s, 0, 0, argb, true);
      c.popScale();
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
