package dev.swiftclient.core.ui;

import dev.swiftclient.core.gfx.Canvas;
import java.util.Locale;

public final class Kit {
   public static final int CARD = -15263718;
   public static final int CARD_HOV = -14605786;
   public static final int ROW = -15461097;
   public static final int LINE = 419430399;
   public static final int EDGE = 520093695;
   public static final int EDGE_HOV = 872415231;
   public static final int ACCENT = -12877066;
   public static final int ON = -12868259;
   public static final int OFF_TRACK = -12960962;
   public static final int TEXT = -1;
   public static final int DIM = -6644317;
   public static final int FAINT = -10394518;
   public static final int DANGER = -2067601;
   public static final float R_CARD = 6.0F;
   public static final float R_ROW = 5.0F;
   public static final float R_PILL = 4.0F;
   public static final int HEADER_H = 34;
   public static final int CHIPS_H = 26;
   public static final int PAD = 14;
   public static final int MAX_W = 600;

   private Kit() {
   }

   public static Kit.Zone zone(int width) {
      int w = Math.min(width, 600) - 28;
      return new Kit.Zone((width - w) / 2, w);
   }

   public static void titre(Canvas c, Kit.Zone z, String titre) {
      c.text(titre.toUpperCase(Locale.ROOT), z.x(), 10, -1, true);
      Px.streak(c, z.x(), 21);
   }

   public static void sousTitre(Canvas c, Kit.Zone z, String titre, String sous) {
      if (sous != null && !sous.isBlank()) {
         c.text(sous, z.x() + c.textWidth(titre.toUpperCase(Locale.ROOT)) + 10, 10, Px.FAINT, false);
      }
   }

   public static int[] rectRecherche(int width, Kit.Zone z) {
      int w = Math.max(96, Math.min(170, z.w() / 3));
      int droite = Math.min(z.right(), width - 36);
      return new int[]{droite - w, 9, w, 18};
   }

   public static void recherche(Canvas c, int[] r, String requete, boolean focus, long curseur, String indication, int mouseX, int mouseY) {
      boolean over = dedans(mouseX, mouseY, r);
      c.card(r[0], r[1], r[2], r[3], 0xFF0C0F16, focus ? Px.ACCENT : (over ? 0xFF3A4766 : 0), 1, 2.0F);
      c.icon("zoom", r[0] + 5, r[1] + (r[3] - 9) / 2, 9, focus ? Px.DIM : Px.FAINT);
      int tx = r[0] + 18;
      int maxW = r[2] - 24;
      boolean vide = requete.isEmpty() && !focus;
      c.text(tronque(c, vide ? indication : requete, maxW), tx, r[1] + (r[3] - c.lineHeight()) / 2 + 1, vide ? Px.FAINT : -1, false);
      if (focus && curseur / 20L % 2L == 0L) {
         int cx = tx + Math.min(c.textWidth(requete), maxW) + 1;
         c.fill(cx, r[1] + 4, cx + 2, r[1] + r[3] - 4, -1);
      }
   }

   public static int chipY() {
      return 37;
   }

   public static int largeurChip(Canvas c, String libelle) {
      return c.textWidth(libelle) + 18;
   }

   public static int chip(Canvas c, int x, String libelle, boolean actif, boolean indispo, int mouseX, int mouseY) {
      int w = largeurChip(c, libelle);
      int h = 16;
      int y = chipY();
      boolean over = !indispo && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
      int dy = actif ? 0 : (over ? -1 : 1);
      c.card(x, y + dy, w, h, actif ? Px.ACCENT : (over ? 0xFF182033 : 0xFF11151F), 0, 0, 3.0F);
      c.centeredText(libelle, x + w / 2, y + dy + (h - 8) / 2 + 1, indispo ? Px.FAINT : (actif || over ? -1 : Px.DIM), actif);
      return w;
   }

   public static void section(Canvas c, int x, int y, String libelle) {
      c.fill(x, y + 1, x + 4, y + 5, Px.ACCENT);
      c.text(libelle.toUpperCase(Locale.ROOT), x + 8, y, Px.FAINT, false);
   }

   public static void carte(Canvas c, int x, int y, int w, int h, boolean choisie, boolean survol) {
      c.card(x, y - (survol && !choisie ? 1 : 0), w, h, survol ? 0xFF182033 : 0xFF11151F, choisie ? Px.ACCENT : (survol ? 0xFF3A4766 : 0), 1, 4.0F);
   }

   public static void ligne(Canvas c, int x, int y, int w, int h, boolean choisie, boolean survol) {
      c.card(x, y, w, h, survol ? 0xFF182033 : 0xFF11151F, choisie ? Px.ACCENT : (survol ? 0xFF3A4766 : 0), 1, 3.0F);
   }

   public static void bouton(Canvas c, int[] r, String libelle, boolean survol) {
      Px.button(c, r[0], r[1], r[2], r[3], tronque(c, libelle, r[2] - 10), survol, false);
   }

   public static void interrupteur(Canvas c, int x, int y, boolean on) {
      Px.toggle(c, x, y, on);
   }

   public static void barre(Canvas c, int x, int haut, int bas, int decalage, int max) {
      if (max > 0) {
         int vh = bas - haut;
         int h = Math.max(24, vh * vh / (vh + max));
         int y = haut + (vh - h) * decalage / max;
         c.roundRect(x, y, 3, h, 1.5F, Px.ACCENT);
      }
   }

   public static boolean dedans(double mx, double my, int[] r) {
      return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
   }

   public static boolean dedans(double mx, double my, int x, int y, int w, int h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   public static String tronque(Canvas c, String s, int maxW) {
      if (s == null) {
         return "";
      } else if (c.textWidth(s) <= maxW) {
         return s;
      } else {
         while (s.length() > 1 && c.textWidth(s + "…") > maxW) {
            s = s.substring(0, s.length() - 1);
         }

         return s + "…";
      }
   }

   public record Zone(int x, int w) {
      public int right() {
         return this.x + this.w;
      }
   }
}
