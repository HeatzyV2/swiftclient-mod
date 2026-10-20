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
 * Language picker, Swift style: a grid of chamfered slots (the shape of the module tiles), each with its country
 * flag (or its code on a slanted tag for invented languages), the language name and a star. The chosen one is framed in accent with the speed streak;
 * favourites float to the top.
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
         c.roundRect(bx, top, 4, bottom - top, 2.0F, 0x22FFFFFF);
         c.roundRect(bx, barY, 4, barH, 2.0F, Px.ACCENT);
      }

      c.text(Tr.of("swift.language.count", this.rows.size(), this.all.size()), PAD, this.height - 15, Px.FAINT, false);
   }

   private void drawSearch(Canvas c, int mouseX, int mouseY) {
      int[] r = this.searchRect();
      boolean over = mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
      Px.slot(c, r[0], r[1], r[2], r[3], 5, 0xFF0C0F16, this.searchFocused ? Px.ACCENT : (over ? 0xFF3A4560 : 0xFF222838));
      c.icon("zoom", r[0] + 7, r[1] + (r[3] - 10) / 2, 10, this.searchFocused ? Px.ACCENT : Px.FAINT);
      int tx = r[0] + 21;
      int ty = r[1] + (r[3] - c.lineHeight()) / 2 + 1;
      if (this.query.isEmpty() && !this.searchFocused) {
         c.text(Tr.of("swift.language.search"), tx, ty, Px.FAINT, false);
      } else {
         c.text(this.query, tx, ty, Px.TEXT, false);
         if (this.searchFocused && this.caretBlink % 40L < 20L) {
            int cx = tx + c.textWidth(this.query) + 1;
            c.fill(cx, r[1] + 5, cx + 1, r[1] + r[3] - 5, Px.ACCENT);
         }
      }
   }

   /** A language slot: chamfered like the module tiles, the code on a slanted tag, a streak on the chosen one. */
   private void drawTile(Canvas c, Row row, int[] r, int mouseX, int mouseY, boolean inView) {
      boolean sel = row.lang().code().equals(this.selected);
      boolean over = inView && mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
      boolean overStar = over && mouseX >= r[0] + r[2] - 22;
      int x = r[0];
      int y = r[1];
      int w = r[2];
      int h = r[3];
      Px.slot(c, x, y, w, h, 6, sel ? 0xFF0F1830 : (over ? 0xFF161B26 : 0xFF11141B), sel ? Px.ACCENT : (over ? 0xFF3A4560 : 0xFF222838));

      int tagW = 24;
      int tx = x + 8;
      int cell = Flags.cell(row.lang().code());
      if (cell >= 0) {
         int fh = 18;
         int fy = y + (h - fh) / 2;
         Px.chamfer(c, tx, fy, tagW, fh, 3, sel ? Px.ACCENT : 0xFF2A3142);
         Flags.draw(c, cell, tx + 2, fy + 2, tagW - 4, fh - 4);
      } else {
         String code = row.lang().code().split("[_-]")[0].toUpperCase(Locale.ROOT);
         code = code.length() > 3 ? code.substring(0, 3) : code;
         int tagH = 13;
         int ty = y + (h - tagH) / 2;
         Px.chamfer(c, tx, ty, tagW, tagH, 3, sel ? Px.ACCENT : 0xFF232A3A);
         c.text(code, tx + (tagW - c.textWidth(code)) / 2, ty + 3, sel ? Px.TEXT : 0xFFB4BDCC, false);
      }

      int nx = tx + tagW + 8;
      int streakW = sel ? 34 : 0;
      int maxW = x + w - 24 - streakW - nx;
      c.text(fit(c, row.lang().name(), maxW), nx, y + 6, sel || over ? Px.TEXT : 0xFFC9D3E6, false);
      String region = row.lang().region();
      if (region != null && !region.isBlank()) {
         c.text(fit(c, region, maxW), nx, y + 17, sel ? 0xFF8FA8D6 : Px.FAINT, false);
      }

      if (sel) {
         Px.dashes(c, x + w - 24 - streakW + 2, y + h / 2, Px.ACCENT);
      }

      boolean fav = row.fav();
      c.icon("star", x + w - 18, y + (h - 11) / 2, 11, fav ? 0xFFFFD23F : (overStar ? Px.TEXT : 0xFF3A4152));
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

   /** Country flags of the languages (textures/gui/flags.png, built by tools/gen_flags.mjs from flag-icons). */
   private static final class Flags {
      private static final String TEXTURE = "flags.png";
      private static java.util.Map<String, Integer> cells;
      private static int cellW;
      private static int cellH;
      private static int cols;
      private static int texW;
      private static int texH;

      static int cell(String lang) {
         if (cells == null) {
            load();
         }

         Integer i = cells.get(lang.toLowerCase(Locale.ROOT));
         return i == null ? -1 : i;
      }

      static void draw(Canvas c, int cell, int x, int y, int w, int h) {
         c.sprite(TEXTURE, x, y, w, h, cell % cols * cellW, cell / cols * cellH, cellW, cellH, texW, texH);
      }

      private static void load() {
         cells = new java.util.HashMap<>();
         try (java.io.InputStream in = LanguageScreen.class.getResourceAsStream("/assets/swiftclient/textures/gui/flags.json")) {
            if (in == null) {
               return;
            }

            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            cellW = o.get("cellW").getAsInt();
            cellH = o.get("cellH").getAsInt();
            cols = o.get("cols").getAsInt();
            texW = o.get("width").getAsInt();
            texH = o.get("height").getAsInt();
            for (var e : o.getAsJsonObject("langs").entrySet()) {
               cells.put(e.getKey(), e.getValue().getAsInt());
            }
         } catch (java.io.IOException | RuntimeException e) {
            cells = new java.util.HashMap<>();
         }
      }
   }
}
