package dev.swiftclient.core.screen;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Lang;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.ui.Defilement;
import dev.swiftclient.core.ui.UiScreen;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LanguageScreen extends UiScreen {
   private static final int BG = 1426063360;
   private static final int ROW = -871494130;
   private static final int ROW_HOVER = -300016098;
   private static final int ROW_SEL = -299218310;
   private static final int TEXT = -1;
   private static final int TEXT_DIM = -5592406;
   private static final int TEXT_FAINT = -9735552;
   private static final int ACCENT = -12877066;
   private static final int STAR_ON = -340971;
   private static final int SEARCH_BG = -300805614;
   private static final int SEARCH_LINE = -14013910;
   private static final int ROW_H = 22;
   private static final int ROW_GAP = 3;
   private static final int LIST_W = 300;
   private static final int SEARCH_H = 22;
   private static final int STAR_W = 22;
   private static final String FAV_KEY = "lang.favorites";
   private List<Lang> all = List.of();
   private String selected = "";
   private final Set<String> favorites = new LinkedHashSet<>();
   private String query = "";
   private boolean searchFocused;
   private long caretBlink;
   private int listTop;
   private int listBottom;
   private int listX;
   private final Defilement defil = new Defilement();
   private List<LanguageScreen.Row> rows = List.of();

   @Override
   public String title() {
      return "Language";
   }

   @Override
   public void layout(int width, int height) {
      super.layout(width, height);
      this.all = Platform.game().languages();
      this.selected = Platform.game().currentLanguage();
      this.favorites.clear();

      for (String c : Platform.game().getConfig("lang.favorites", "").split(",")) {
         String t = c.trim();
         if (!t.isEmpty()) {
            this.favorites.add(t);
         }
      }

      this.listX = (width - 300) / 2;
      this.listTop = 74;
      this.listBottom = height - 32;
      this.rebuild();
   }

   private void rebuild() {
      String q = this.query.trim().toLowerCase(Locale.ROOT);
      List<LanguageScreen.Row> fav = new ArrayList<>();
      List<LanguageScreen.Row> rest = new ArrayList<>();

      for (Lang l : this.all) {
         if (q.isEmpty() || this.matches(l, q)) {
            boolean f = this.favorites.contains(l.code());
            (f ? fav : rest).add(new LanguageScreen.Row(l, f));
         }
      }

      List<LanguageScreen.Row> out = new ArrayList<>(fav);
      out.addAll(rest);
      this.rows = out;
      this.clampScroll();
   }

   private boolean matches(Lang l, String q) {
      return l.name().toLowerCase(Locale.ROOT).contains(q) || l.region().toLowerCase(Locale.ROOT).contains(q) || l.code().toLowerCase(Locale.ROOT).contains(q);
   }

   private int rowStride() {
      return 25;
   }

   private int viewHeight() {
      return Math.max(0, this.listBottom - this.listTop);
   }

   private void clampScroll() {
      this.defil.contenu(this.rows.size() * this.rowStride(), this.viewHeight());
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      this.caretBlink++;
      this.clampScroll();
      this.defil.anime();
      c.fill(0, 0, this.width, this.height, 1426063360);
      c.centeredText(this.title(), this.width / 2, 18, -1, true);
      this.drawSearch(c, mouseX, mouseY);
      c.pushScissor(this.listX, this.listTop, 300, this.viewHeight());
      int y = this.listTop - this.defil.px();
      boolean drewFavSep = false;

      for (int i = 0; i < this.rows.size(); i++) {
         LanguageScreen.Row r = this.rows.get(i);
         if (this.query.isEmpty() && !r.fav() && !drewFavSep && i > 0) {
            drewFavSep = true;
         }

         if (y + 22 >= this.listTop && y <= this.listBottom) {
            this.drawRow(c, r, y, mouseX, mouseY);
         }

         y += this.rowStride();
      }

      c.popScissor();
      if (this.rows.isEmpty()) {
         c.centeredText("No language matches \"" + this.query + "\"", this.width / 2, this.listTop + 20, -9735552, true);
      }

      String count = this.rows.size() + " / " + this.all.size() + " languages";
      c.centeredText(count, this.width / 2, this.height - 20, -9735552, true);
   }

   private void drawSearch(Canvas c, int mouseX, int mouseY) {
      int sw = 300;
      int sx = this.listX;
      int sy = 44;
      c.roundRect(sx, sy, sw, 22, 5.0F, -300805614);
      c.roundRect(sx, sy, sw, 22, 5.0F, this.searchFocused ? 1715176182 : -14013910);
      c.roundRect(sx + 1, sy + 1, sw - 2, 20, 4.0F, -300805614);
      int tx = sx + 8;
      int ty = sy + (22 - c.lineHeight()) / 2 + 1;
      if (this.query.isEmpty() && !this.searchFocused) {
         c.text("Search a language...", tx, ty, -9735552, false);
      } else {
         String shown = this.query;
         if (this.searchFocused && this.caretBlink % 60L < 30L) {
            shown = shown + "_";
         }

         c.text(shown, tx, ty, -1, false);
      }
   }

   private void drawRow(Canvas c, LanguageScreen.Row r, int y, int mouseX, int mouseY) {
      boolean sel = r.lang().code().equals(this.selected);
      boolean overRow = mouseX >= this.listX && mouseX <= this.listX + 300 && mouseY >= y && mouseY <= y + 22;
      boolean overStar = mouseX >= this.listX + 300 - 22 && overRow;
      c.roundRect(this.listX, y, 300, 22, 5.0F, sel ? -299218310 : (overRow ? -300016098 : -871494130));
      int ty = y + (22 - c.lineHeight()) / 2 + 1;
      c.text(r.lang().display(), this.listX + 10, ty, sel ? -1 : -5592406, true);
      int starX = this.listX + 300 - 22 + 5;
      int starColor = r.fav() ? -340971 : (overStar ? -5592406 : -9735552);
      c.centeredText(r.fav() ? "★" : "☆", starX + 5, ty, starColor, true);
   }

   @Override
   public boolean click(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      } else {
         boolean inSearch = mouseX >= this.listX && mouseX <= this.listX + 300 && mouseY >= 44.0 && mouseY <= 66.0;
         this.searchFocused = inSearch;
         if (inSearch) {
            Platform.game().playClick();
            return true;
         } else if (!(mouseX < this.listX) && !(mouseX > this.listX + 300) && !(mouseY < this.listTop) && !(mouseY > this.listBottom)) {
            int idx = (int)((mouseY - this.listTop + this.defil.px()) / this.rowStride());
            if (idx >= 0 && idx < this.rows.size()) {
               LanguageScreen.Row r = this.rows.get(idx);
               if (mouseX >= this.listX + 300 - 22) {
                  this.toggleFavorite(r.lang().code());
               } else {
                  Platform.game().playClick();
                  if (!r.lang().code().equals(this.selected)) {
                     this.selected = r.lang().code();
                     Platform.game().setLanguage(r.lang().code());
                  }
               }

               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   private void toggleFavorite(String code) {
      Platform.game().playClick();
      if (!this.favorites.remove(code)) {
         this.favorites.add(code);
      }

      Platform.game().setConfig("lang.favorites", String.join(",", this.favorites));
      this.rebuild();
   }

   @Override
   public boolean charTyped(String s) {
      if (!this.searchFocused) {
         return false;
      } else {
         this.query = this.query + s;
         this.rebuild();
         return true;
      }
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
      this.defil.cran(amount, this.rowStride());
      return true;
   }

   @Override
   public boolean closeOnEscape() {
      return !this.searchFocused && this.query.isEmpty();
   }

   private record Row(Lang lang, boolean fav) {
   }
}
