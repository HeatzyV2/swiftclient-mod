package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.perf.FrameStats;
import dev.swiftclient.core.platform.Tr;

/**
 * Performance monitor: average FPS, the 1% and 0.1% lows (the numbers that tell stutter from smoothness),
 * a frame-time graph, and, when something is off, one line of advice.
 */
public final class PerfElement extends HudElement {
   private static final int GRAPH_H = 22;
   private static final int GRAPH_MIN_W = 96;
   private static final int GREEN = -9707413;
   private static final int AMBER = -865972;
   private static final int RED = -38037;
   private static final int DIM = -6644317;

   public PerfElement() {
      super("perf", "Performance");
   }

   @Override
   public String icon() {
      return "gauge";
   }

   private static int fpsColor(double fps) {
      return fps >= 60.0 ? GREEN : (fps >= 30.0 ? AMBER : RED);
   }

   private String head() {
      return Math.round(FrameStats.WINDOW.averageFps()) + " FPS";
   }

   private String lows() {
      return "1% " + Math.round(FrameStats.WINDOW.lowFps(1.0)) + "   0.1% " + Math.round(FrameStats.WINDOW.lowFps(0.1));
   }

   private String advice(HudData d) {
      if (!this.opt("advice", true) || FrameStats.WINDOW.size() < 120) {
         return null;
      }

      FrameStats.Advice a = FrameStats.advise(FrameStats.WINDOW.averageFps(), FrameStats.WINDOW.lowFps(1.0), d == null ? 12 : d.renderDistance());
      return a == null ? null : Tr.of("swift.perf.advice." + a.key);
   }

   private int innerWidth(Canvas c, String advice) {
      int w = Math.max(c.textWidth(this.head()), this.opt("lows", true) ? c.textWidth(this.lows()) : 0);
      if (this.opt("graph", true)) {
         w = Math.max(w, GRAPH_MIN_W);
      }

      return advice == null ? w : Math.max(w, Math.min(c.textWidth(advice), 190));
   }

   @Override
   public int width(Canvas c, HudData d) {
      return this.innerWidth(c, this.advice(d)) + 17;
   }

   @Override
   public int height(Canvas c) {
      return this.height(c, null);
   }

   @Override
   public int height(Canvas c, HudData d) {
      int lines = 1 + (this.opt("lows", true) ? 1 : 0);
      int h = chipH(c, lines);
      if (this.opt("graph", true)) {
         h += GRAPH_H + 3;
      }

      return this.advice(d) == null ? h : h + c.lineHeight() + 3;
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      String advice = this.advice(d);
      int iw = this.innerWidth(c, advice);
      plate(c, x, y, iw + 17, this.height(c, d));
      int tx = x + 9;
      int ty = y + 4;
      c.text(this.head(), tx, ty, fpsColor(FrameStats.WINDOW.averageFps()), true);
      ty += c.lineHeight() + 2;
      if (this.opt("lows", true)) {
         c.text(this.lows(), tx, ty, fpsColor(FrameStats.WINDOW.lowFps(1.0)), true);
         ty += c.lineHeight() + 2;
      }

      if (this.opt("graph", true)) {
         this.graph(c, tx, ty + 1, iw);
         ty += GRAPH_H + 3;
      }

      if (advice != null) {
         c.text(advice, tx, ty, AMBER, true);
      }
   }

   /** One column per recent frame: tall = slow. The 16.7 ms (60 FPS) line is drawn for reference. */
   private void graph(Canvas c, int x, int y, int w) {
      c.fill(x, y, x + w, y + GRAPH_H, 0x33000000);
      float[] ms = FrameStats.WINDOW.recent(w);
      int offset = w - ms.length;
      for (int i = 0; i < ms.length; i++) {
         int h = Math.max(1, Math.min(GRAPH_H, Math.round(ms[i] / 33.4F * GRAPH_H)));
         int color = ms[i] <= 16.8F ? GREEN : (ms[i] <= 33.4F ? AMBER : RED);
         c.fill(x + offset + i, y + GRAPH_H - h, x + offset + i + 1, y + GRAPH_H, color);
      }

      int ref = y + GRAPH_H - Math.round(16.7F / 33.4F * GRAPH_H);
      c.fill(x, ref, x + w, ref + 1, DIM & 0x00FFFFFF | 0x66000000);
   }

   @Override
   public boolean drawsWithoutData() {
      return true;
   }

   @Override
   public String[] previewLines() {
      return new String[]{"144 FPS", "1% 96   0.1% 71"};
   }

   @Override
   public int[] previewSize(Canvas c) {
      int w = Math.max(GRAPH_MIN_W, c.textWidth("1% 96   0.1% 71"));
      return new int[]{w + 17, chipH(c, 2) + GRAPH_H + 3};
   }
}
