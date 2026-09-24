package dev.swiftclient.core.screen;

import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.theme.Theme;
import dev.swiftclient.core.theme.ThemeManager;
import dev.swiftclient.core.theme.Themes;
import dev.swiftclient.core.ui.Kit;
import dev.swiftclient.core.ui.UiScreen;
import java.util.List;

public class ThemeScreen extends UiScreen {
   private static final int COLS = 3;
   private static final int GAP = 10;
   private static final int NOM_H = 20;
   private List<Theme> themes = List.of();
   private Kit.Zone z;

   @Override
   public String title() {
      return Tr.of("swift.theme.title");
   }

   @Override
   public void layout(int width, int height) {
      super.layout(width, height);
      this.themes = Themes.all();
      this.z = Kit.zone(width);
   }

   private int carteW() {
      return (this.z.w() - 20) / 3;
   }

   private int vignetteH() {
      return Math.round(this.carteW() * 0.56F);
   }

   private int carteH() {
      return this.vignetteH() + 20 + 8;
   }

   private int haut() {
      return 40;
   }

   private int[] rectCarte(int i) {
      int x = this.z.x() + i % 3 * (this.carteW() + 10);
      int y = this.haut() + i / 3 * (this.carteH() + 10);
      return new int[]{x, y, this.carteW(), this.carteH()};
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      Kit.titre(c, this.z, this.title());
      Kit.sousTitre(c, this.z, this.title(), Tr.of(this.themes.size() > 1 ? "swift.theme.count" : "swift.theme.count_one", this.themes.size()));
      String actif = ThemeManager.currentId();

      for (int i = 0; i < this.themes.size(); i++) {
         Theme t = this.themes.get(i);
         int[] r = this.rectCarte(i);
         boolean survol = Kit.dedans(mouseX, mouseY, r);
         boolean choisi = t.id().equals(actif);
         Kit.carte(c, r[0], r[1], r[2], r[3], choisi, survol);
         int vx = r[0] + 4;
         int vy = r[1] + 4;
         int vw = r[2] - 8;
         int vh = this.vignetteH();
         if (t.top() == t.bottom()) {
            c.fill(vx, vy, vx + vw, vy + vh, t.top());
         } else {
            c.gradientV(vx, vy, vw, vh, t.top(), t.bottom());
         }

         c.text(Kit.tronque(c, t.name(), r[2] - 16), r[0] + 8, vy + vh + 6 + 2, !choisi && !survol ? -6644317 : -1, false);
      }
   }

   @Override
   public boolean click(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      } else {
         for (int i = 0; i < this.themes.size(); i++) {
            if (Kit.dedans(mouseX, mouseY, this.rectCarte(i))) {
               ThemeManager.select(this.themes.get(i).id());
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public boolean closeOnEscape() {
      return !ThemeManager.isAnimating();
   }
}
