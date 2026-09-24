package dev.swiftclient.core.screen;

import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Account;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.ui.Kit;
import dev.swiftclient.core.ui.UiScreen;
import java.util.List;

public class AccountScreen extends UiScreen {
   private static final int LIGNE_H = 36;
   private static final int ECART = 6;
   private static final int TETE = 20;
   private static final int ICONE = 11;
   private static final int AJOUT_H = 28;
   private List<Account> comptes = List.of();
   private String statut = "";
   private Kit.Zone z;

   @Override
   public String title() {
      return Tr.of("swift.accounts.title");
   }

   @Override
   public void layout(int width, int height) {
      super.layout(width, height);
      this.z = Kit.zone(width);
      this.rafraichir();
   }

   private void rafraichir() {
      this.comptes = Platform.game().accounts();
   }

   private int haut() {
      return 40;
   }

   private int yLigne(int i) {
      return this.haut() + i * 42;
   }

   private int[] rectAjout() {
      return new int[]{this.z.x(), this.yLigne(this.comptes.size()) + 4, this.z.w(), 28};
   }

   private int[] rectRetirer(int y) {
      return new int[]{this.z.right() - 28, y + 8, 20, 20};
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      this.rafraichir();
      Kit.titre(c, this.z, this.title());
      Kit.sousTitre(c, this.z, this.title(), this.comptes.isEmpty() ? "" : String.valueOf(this.comptes.size()));

      for (int i = 0; i < this.comptes.size(); i++) {
         this.dessinerLigne(c, this.comptes.get(i), this.yLigne(i), mouseX, mouseY);
      }

      int[] a = this.rectAjout();
      Kit.bouton(c, a, Tr.of("swift.accounts.add"), Kit.dedans(mouseX, mouseY, a));
      if (!this.statut.isBlank()) {
         c.text(Kit.tronque(c, this.statut, this.z.w()), this.z.x(), a[1] + a[3] + 10, -10394518, false);
      }
   }

   private void dessinerLigne(Canvas c, Account a, int y, int mouseX, int mouseY) {
      boolean survol = Kit.dedans(mouseX, mouseY, this.z.x(), y, this.z.w(), 36);
      Kit.ligne(c, this.z.x(), y, this.z.w(), 36, a.active(), survol);
      int tx = this.z.x() + 10;
      int ty = y + 8;
      c.playerHead(a.uuid(), tx, ty, 20);
      int nx = tx + 20 + 10;
      c.text(Kit.tronque(c, a.username(), this.z.w() - 90), nx, y + 8, !a.active() && !survol ? -6644317 : -1, false);
      String sous = a.active() ? Tr.of("swift.accounts.active") : (a.offline() ? Tr.of("swift.accounts.offline") : Tr.of("swift.accounts.microsoft"));
      c.text(sous, nx, y + 20, a.active() ? -6644317 : -10394518, false);
      if (survol) {
         int[] r = this.rectRetirer(y);
         boolean surCroix = Kit.dedans(mouseX, mouseY, r);
         c.icon("close", r[0] + (r[2] - 11) / 2, r[1] + (r[3] - 11) / 2, 11, surCroix ? -2067601 : -10394518);
      }
   }

   @Override
   public boolean click(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      } else if (Kit.dedans(mouseX, mouseY, this.rectAjout())) {
         Platform.game().playClick();
         this.statut = Tr.of("swift.accounts.opening");
         Platform.game().addAccount(msg -> this.statut = msg);
         return true;
      } else {
         for (int i = 0; i < this.comptes.size(); i++) {
            Account a = this.comptes.get(i);
            int y = this.yLigne(i);
            if (Kit.dedans(mouseX, mouseY, this.z.x(), y, this.z.w(), 36)) {
               if (Kit.dedans(mouseX, mouseY, this.rectRetirer(y))) {
                  Platform.game().playClick();
                  Platform.game().removeAccount(a.uuid());
               } else if (!a.active()) {
                  Platform.game().playClick();
                  this.statut = Tr.of("swift.accounts.switching", a.username());
                  Platform.game().switchAccount(a.uuid());
               }

               return true;
            }
         }

         return false;
      }
   }
}
