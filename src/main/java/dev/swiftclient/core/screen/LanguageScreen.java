package dev.swiftclient.core.screen;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Lang;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.ui.Defilement;
import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.ui.UiScreen;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Language picker, Swift style: a grid of "slots", each with a code plate, the language name and a star.
 * The selected slot is framed in white like the selected hotbar slot; favourites float to the top.
 */
public class LanguageScreen extends UiScreen {
   private static final String FAV_KEY = "lang.favorites";
   private static final int PAD = 14;
   private static final int TILE_H = 30;
   private static final int GAP = 6;
   private static final int TILE_MIN_W = 150;
   private static final int HEAD_H = 40;
   private List<Lang> all = List.of();
   private String selected = "";
   private final Set<String> favorites = new LinkedHashSet<>();
   private String query = "";
   private boolean searchFocused;
   private long caretBlink;
   private final Defilement defil = new Defilement();
   private List<Row> rows = List.of();

   @Override
   public String title() {
      return Tr.of("swift.language.title");
   }

   @Override
   public void layout(int width, int height) {
      super.layout(width, height);
      this.all = Platform.game().languages();
      this.selected = Platform.game().currentLanguage();
      this.favorites.clear();
      for (String c : Platform.game().getConfig(FAV_KEY, "").split(",")) {
         String t = c.trim();
         if (!t.isEmpty()) {
            this.favorites.add(t);
         }
      }
      this.rebuild();
   }

   private void rebuild() {
      String q = this.query.trim().toLowerCase(Locale.ROOT);
      List<Row> fav = new ArrayList<>();
      List<Row> rest = new ArrayList<>();
      for (Lang l : this.all) {
         if (q.isEmpty() || this.matches(l, q)) {
            boolean f = this.favorites.contains(l.code());
            (f ? fav : rest).add(new Row(l, f));
         }
      }
      List<Row> out = new ArrayList<>(fav);
      out.addAll(rest);
      this.rows = out;
      this.clampScroll();
   }

   private boolean matches(Lang l, String q) {
      return l.name().toLowerCase(Locale.ROOT).contains(q) || l.region().toLowerCase(Locale.ROOT).contains(q) || l.code().toLowerCase(Locale.ROOT).contains(q);
   }

   // ---- geometry ----

   private int gridX() {
      return PAD;
   }

   private int gridW() {
      return this.width - PAD * 2 - 8;
   }

   private int gridTop() {
      return HEAD_H + 6;
   }

   private int gridBottom() {
      return this.height - 22;
   }

   private int columns() {
      return Math.max(1, (this.gridW() + GAP) / (TILE_MIN_W + GAP));
   }

   private int tileW() {
      int cols = this.columns();
      return (this.gridW() - (cols - 1) * GAP) / cols;
   }

   private int[] tileRect(int i) {
      int cols = this.columns();
      int w = this.tileW();
      return new int[]{this.gridX() + (i % cols) * (w + GAP), this.gridTop() + (i / cols) * (TILE_H + GAP) - this.defil.px(), w, TILE_H};
   }

   private int[] searchRect() {
      int w = Math.min(190, this.width / 2);
      return new int[]{this.width - PAD - 8 - w, 8, w, 20};
   }

   private void clampScroll() {
      int cols = this.columns();
      int rowsN = (this.rows.size() + cols - 1) / cols;
      this.defil.contenu(Math.max(0, rowsN * (TILE_H + GAP) - GAP), Math.max(0, this.gridBottom() - this.gridTop()));
   }

   // ---- drawing ----

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      this.caretBlink++;
      this.clampScroll();
      this.defil.anime();
      Px.title(c, this.title(), PAD, 10);
      this.drawSearch(c, mouseX, mouseY);

      int top = this.gridTop();
      int bottom = this.gridBottom();
      c.pushScissor(0, top - 2, this.width, bottom - top + 4);
      for (int i = 0; i < this.rows.size(); i++) {
         int[] r = this.tileRect(i);
         if (r[1] + r[3] >= top - 2 && r[1] <= bottom) {
            this.drawTile(c, this.rows.get(i), r, mouseX, mouseY, mouseY >= top && mouseY <= bottom);
         }
      }
      c.popScissor();

      if (this.rows.isEmpty()) {
         c.centeredText(Tr.of("swift.language.no_match", this.query), this.width / 2, top + 24, Px.FAINT, true);
      }

      int maxPx = this.defil.maxPx();
      if (maxPx > 0) {
         int vh = bottom - top;
         int barH = Math.max(18, vh * vh / (vh + maxPx));
         int barY = top + (vh - barH) * this.defil.px() / maxPx;
         int bx = this.width - 12;
         c.fill(bx, top, bx + 5, bottom, 0x66000000);
         c.fill(bx, barY, bx + 5, barY + barH, Px.INK);
         c.fill(bx + 1, barY + 1, bx + 4, barY + barH - 1, Px.ACCENT);
      }

      c.text(Tr.of("swift.language.count", this.rows.size(), this.all.size()), PAD, this.height - 15, Px.FAINT, false);
   }

   private void drawSearch(Canvas c, int mouseX, int mouseY) {
      int[] r = this.searchRect();
      boolean over = mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
      c.card(r[0], r[1], r[2], r[3], 0xFF0C0F16, this.searchFocused ? Px.ACCENT : (over ? 0xFF3A4766 : 0), 1, 2.0F);
      int tx = r[0] + 6;
      int ty = r[1] + (r[3] - c.lineHeight()) / 2 + 1;
      if (this.query.isEmpty() && !this.searchFocused) {
         c.text(Tr.of("swift.language.search"), tx, ty, Px.FAINT, false);
      } else {
         c.text(this.query, tx, ty, Px.TEXT, false);
         if (this.searchFocused && this.caretBlink % 40L < 20L) {
            int cx = tx + c.textWidth(this.query) + 1;
            c.fill(cx, r[1] + 4, cx + 2, r[1] + r[3] - 4, Px.TEXT);
         }
      }
   }

   private void drawTile(Canvas c, Row row, int[] r, int mouseX, int mouseY, boolean inView) {
      boolean sel = row.lang().code().equals(this.selected);
      boolean over = inView && mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
      boolean overStar = over && mouseX >= r[0] + r[2] - 22;
      int y = r[1] - (over && !sel ? 2 : 0);
      c.card(r[0], y, r[2], r[3], sel ? 0xFF1D2535 : (over ? 0xFF182033 : 0xFF11151F), sel ? 0xFFFFFFFF : 0, sel ? 1 : 0, 3.0F);

      // Code plate: the language code on an accent block
      String code = row.lang().code().split("[_-]")[0].toUpperCase(Locale.ROOT);
      int plate = 22;
      int px = r[0] + 5;
      int py = y + (r[3] - plate) / 2;
      c.card(px, py, plate, plate, sel ? Px.ACCENT : 0xFF222B42, 0, 0, 2.0F);
      c.centeredText(code.length() > 3 ? code.substring(0, 3) : code, px + plate / 2, py + (plate - 8) / 2 + 1, Px.TEXT, true);

      int tx = px + plate + 7;
      int maxW = r[0] + r[2] - 26 - tx;
      c.text(fit(c, row.lang().name(), maxW), tx, y + 6, sel ? Px.TEXT : (over ? Px.TEXT : 0xFFC9D3E6), true);
      String region = row.lang().region();
      if (region != null && !region.isBlank()) {
         c.text(fit(c, region, maxW), tx, y + 17, Px.FAINT, false);
      }

      // Star
      int sx = r[0] + r[2] - 17;
      int sy = y + (r[3] - 9) / 2;
      boolean fav = row.fav();
      c.text(fav ? "★" : "☆", sx, sy, fav ? 0xFFFFD23F : (overStar ? Px.TEXT : Px.FAINT), true);
      if (sel) {
         Px.lamp(c, r[0] + r[2] - 14, y + r[3] - 9, true);
      }
   }

   private static String fit(Canvas c, String s, int maxW) {
      if (s == null) {
         return "";
      }
      if (c.textWidth(s) <= maxW) {
         return s;
      }
      while (s.length() > 1 && c.textWidth(s + "…") > maxW) {
         s = s.substring(0, s.length() - 1);
      }
      return s + "…";
   }

   // ---- input ----

   @Override
   public boolean click(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      }
      int[] sr = this.searchRect();
      boolean inSearch = mouseX >= sr[0] && mouseX < sr[0] + sr[2] && mouseY >= sr[1] && mouseY < sr[1] + sr[3];
      this.searchFocused = inSearch;
      if (inSearch) {
         Platform.game().playClick();
         return true;
      }
      if (mouseY < this.gridTop() || mouseY > this.gridBottom()) {
         return false;
      }
      for (int i = 0; i < this.rows.size(); i++) {
         int[] r = this.tileRect(i);
         if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]) {
            Row row = this.rows.get(i);
            if (mouseX >= r[0] + r[2] - 22) {
               this.toggleFavorite(row.lang().code());
            } else {
               Platform.game().playClick();
               if (!row.lang().code().equals(this.selected)) {
                  this.selected = row.lang().code();
                  Platform.game().setLanguage(row.lang().code());
               }
            }
            return true;
         }
      }
      return false;
   }

   private void toggleFavorite(String code) {
      Platform.game().playClick();
      if (!this.favorites.remove(code)) {
         this.favorites.add(code);
      }
      Platform.game().setConfig(FAV_KEY, String.join(",", this.favorites));
      this.rebuild();
   }

   @Override
   public boolean charTyped(String s) {
      if (!this.searchFocused) {
         return false;
      }
      this.query = this.query + s;
      this.rebuild();
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode) {
      if (!this.searchFocused) {
         return false;
      } else if (keyCode == 259) {
         if (!this.query.isEmpty()) {
            this.query = this.query.substring(0, this.query.length() - 1);
            this.rebuild();
         }
         return true;
      } else if (keyCode == 256) {
         this.searchFocused = false;
         if (!this.query.isEmpty()) {
            this.query = "";
            this.rebuild();
         }
         return true;
      } else {
         return keyCode == 257;
      }
   }

   @Override
   public boolean scroll(double mouseX, double mouseY, double amount) {
      this.defil.cran(amount, TILE_H + GAP);
      return true;
   }

   @Override
   public boolean closeOnEscape() {
      return !this.searchFocused && this.query.isEmpty();
   }

   private record Row(Lang lang, boolean fav) {
   }
}
