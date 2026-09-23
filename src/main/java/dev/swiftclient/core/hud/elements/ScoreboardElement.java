package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import java.util.List;

public final class ScoreboardElement extends HudElement {
   private static final int PAD_X = 3;
   private static final int PAD_Y = 2;
   private static final int GAP = 6;
   private static final int SCORE = -43691;

   public ScoreboardElement() {
      super("scoreboard", "Scoreboard");
   }

   private boolean showNumbers() {
      return this.opt("numbers", true);
   }

   private boolean showTitle() {
      return this.opt("title", true);
   }

   private boolean background() {
      return this.opt("background", true);
   }

   private int backgroundColor() {
      return this.optColor("bg_color", 1711276032);
   }

   private static int largeur(Canvas c, String nu, Object composant) {
      if (composant != null) {
         int w = c.richWidth(composant);
         if (w >= 0) {
            return w;
         }
      }

      return c.textWidth(nu);
   }

   private static void ecrire(Canvas c, String nu, Object composant, int x, int y, int argb) {
      if (composant == null || !c.richText(composant, x, y, argb, true)) {
         c.text(nu, x, y, argb, true);
      }
   }

   private List<HudData.SidebarLine> lines(HudData d) {
      HudData.Sidebar s = d == null ? null : d.sidebar();
      return s != null && s.lines() != null ? s.lines() : List.of();
   }

   private HudData.Sidebar bar(HudData d) {
      return d == null ? null : d.sidebar();
   }

   private int innerWidth(Canvas c, HudData d) {
      HudData.Sidebar s = this.bar(d);
      if (s == null) {
         return 0;
      } else {
         int w = 0;
         if (this.showTitle()) {
            w = largeur(c, s.title() == null ? "" : s.title(), s.titleText());
         }

         for (HudData.SidebarLine l : s.lines()) {
            int lw = largeur(c, l.text(), l.textComponent());
            if (this.showNumbers() && !l.score().isEmpty()) {
               lw += 6 + largeur(c, l.score(), l.scoreComponent());
            }

            w = Math.max(w, lw);
         }

         return w;
      }
   }

   private int rows(HudData d) {
      int n = this.lines(d).size();
      return n == 0 ? 0 : n + (this.showTitle() ? 1 : 0);
   }

   @Override
   public int width(Canvas c, HudData d) {
      return this.lines(d).isEmpty() ? 0 : this.innerWidth(c, d) + 6;
   }

   @Override
   public int height(Canvas c) {
      return c.lineHeight() + 4;
   }

   @Override
   public int height(Canvas c, HudData d) {
      int n = this.rows(d);
      return n == 0 ? 0 : n * c.lineHeight() + 4;
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      HudData.Sidebar s = this.bar(d);
      if (s != null && !s.lines().isEmpty()) {
         int w = this.width(c, d);
         int h = this.height(c, d);
         if (this.background()) {
            c.card(x, y, w, h, this.backgroundColor(), 587202559, 1, 3.0F);
         }

         int lh = c.lineHeight();
         int yy = y + 2;
         if (this.showTitle()) {
            String titre = s.title() == null ? "" : s.title();
            int tw = largeur(c, titre, s.titleText());
            ecrire(c, titre, s.titleText(), x + (w - tw) / 2, yy, -1);
            yy += lh;
         }

         for (HudData.SidebarLine line : s.lines()) {
            ecrire(c, line.text(), line.textComponent(), x + 3, yy, -1);
            if (this.showNumbers() && !line.score().isEmpty()) {
               int sw = largeur(c, line.score(), line.scoreComponent());
               ecrire(c, line.score(), line.scoreComponent(), x + w - 3 - sw, yy, -43691);
            }

            yy += lh;
         }
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"SERVER", "Kills         12", "Coins      1 340"};
   }
}
