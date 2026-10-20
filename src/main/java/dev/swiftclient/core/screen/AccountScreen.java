package dev.swiftclient.core.screen;

import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Account;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.ui.Kit;
import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.ui.UiScreen;
import java.util.List;
import java.util.Locale;

/**
 * Accounts, Swift style: chamfered cards like the module tiles, the head in a chamfered frame, the state on a
 * slanted tag. The active account is framed in accent with the speed streak; hovering another one offers to use
 * it, and the cross removes it.
 */
public class AccountScreen extends UiScreen {
   private static final int LIGNE_H = 40;
   private static final int ECART = 6;
   private static final int AJOUT_H = 30;
   private static final int CUT = 7;
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
      List<Account> list = Platform.game().accounts();
      this.comptes = list == null ? List.of() : list;
   }

   private int haut() {
      return 40;
   }

   private int yLigne(int i) {
      return this.haut() + i * (LIGNE_H + ECART);
   }

   private int[] rectAjout() {
      return new int[]{this.z.x(), this.yLigne(this.comptes.size()) + 4, this.z.w(), AJOUT_H};
   }

   private int[] rectRetirer(int y) {
      return new int[]{this.z.right() - 26, y + (LIGNE_H - 18) / 2, 18, 18};
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      this.rafraichir();
      Px.title(c, this.title(), this.z.x(), 10);
      if (!this.comptes.isEmpty()) {
         String n = String.valueOf(this.comptes.size());
         int tx = this.z.x() + c.textWidth(this.title().toUpperCase(Locale.ROOT)) + 8;
         Px.chamfer(c, tx, 8, c.textWidth(n) + 10, 12, 3, 0xFF232A3A);
         c.text(n, tx + 5, 10, 0xFFB4BDCC, false);
      }

      for (int i = 0; i < this.comptes.size(); i++) {
         this.dessinerLigne(c, this.comptes.get(i), this.yLigne(i), mouseX, mouseY);
      }

      this.dessinerAjout(c, mouseX, mouseY);
      if (!this.statut.isBlank()) {
         int[] a = this.rectAjout();
         c.text(Kit.tronque(c, this.statut, this.z.w()), this.z.x(), a[1] + a[3] + 10, Px.FAINT, false);
      }
   }

   private void dessinerLigne(Canvas c, Account a, int y, int mouseX, int mouseY) {
      int x = this.z.x();
      int w = this.z.w();
      boolean on = a.active();
      boolean survol = Kit.dedans(mouseX, mouseY, x, y, w, LIGNE_H);
      Px.slot(c, x, y, w, LIGNE_H, CUT, on ? 0xFF0F1830 : (survol ? 0xFF161B26 : 0xFF11141B), on ? Px.ACCENT : (survol ? 0xFF3A4560 : 0xFF222838));

      int head = 22;
      int hx = x + 10;
      int hy = y + (LIGNE_H - head) / 2;
      Px.slot(c, hx - 3, hy - 3, head + 6, head + 6, 4, 0xFF0B0E15, on ? Px.ACCENT : 0xFF2A3142);
      c.playerHead(a.uuid(), hx, hy, head);

      int nx = hx + head + 12;
      c.text(Kit.tronque(c, a.username(), w - 150), nx, y + 9, on || survol ? Px.TEXT : 0xFFC9D3E6, false);
      String etat = (on ? Tr.of("swift.accounts.active") : (a.offline() ? Tr.of("swift.accounts.offline") : Tr.of("swift.accounts.microsoft"))).toUpperCase(Locale.ROOT);
      int tagW = c.textWidth(etat) + 10;
      int ty = y + 21;
      Px.chamfer(c, nx, ty, tagW, 11, 3, on ? Px.ACCENT : 0xFF232A3A);
      c.text(etat, nx + 5, ty + 2, on ? Px.TEXT : 0xFF8A93A0, false);

      int right = this.z.right() - 10;
      if (survol) {
         int[] r = this.rectRetirer(y);
         boolean surCroix = Kit.dedans(mouseX, mouseY, r);
         c.icon("close", r[0] + (r[2] - 10) / 2, r[1] + (r[3] - 10) / 2, 10, surCroix ? 0xFFE07A6F : Px.FAINT);
         right = r[0] - 8;
      }

      if (on) {
         Px.dashes(c, right - 31, y + LIGNE_H / 2, Px.ACCENT);
      } else if (survol) {
         String use = Tr.of("swift.accounts.use").toUpperCase(Locale.ROOT);
         c.text(use, right - c.textWidth(use), y + (LIGNE_H - c.lineHeight()) / 2 + 1, Px.ACCENT, false);
      }
   }

   private void dessinerAjout(Canvas c, int mouseX, int mouseY) {
      int[] a = this.rectAjout();
      boolean survol = Kit.dedans(mouseX, mouseY, a);
      Px.slot(c, a[0], a[1], a[2], a[3], 6, survol ? 0xFF121A2C : 0xFF0D1018, survol ? Px.ACCENT : 0xFF2A3348);
      String label = Tr.of("swift.accounts.add").replaceFirst("^\\+\\s*", "");
      int plus = 13;
      int total = plus + 7 + c.textWidth(label);
      int sx = a[0] + (a[2] - total) / 2;
      int py = a[1] + (a[3] - plus) / 2;
      Px.chamfer(c, sx, py, plus, plus, 3, survol ? Px.ACCENT : 0xFF232A3A);
      c.fill(sx + 3, py + 6, sx + plus - 3, py + 7, Px.TEXT);
      c.fill(sx + 6, py + 3, sx + 7, py + plus - 3, Px.TEXT);
      c.text(label, sx + plus + 7, a[1] + (a[3] - c.lineHeight()) / 2 + 1, survol ? Px.TEXT : 0xFFB4BDCC, false);
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
            if (Kit.dedans(mouseX, mouseY, this.z.x(), y, this.z.w(), LIGNE_H)) {
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
