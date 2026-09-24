package dev.swiftclient.core.screen;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.mods.Profiles;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.ui.Kit;
import dev.swiftclient.core.ui.UiScreen;
import java.util.List;
import net.minecraft.client.resources.language.I18n;

/**
 * Profiles — Swift DA: left name rail + right detail pane.
 * No icon slabs / bordered hero cards.
 */
public class ProfilesScreen extends UiScreen {
   private static final int ACCENT = -12877066;
   private static final int RAIL_W = 148;
   private static final int ROW_H = 28;
   private static final int NOM_MAX = 24;
   private List<String> noms = List.of();
   private String actif = "";
   private String focus = "";
   private String statut = "";
   private long statutMs;
   private boolean saisie;
   private String nom = "";
   private long curseur;

   @Override
   public String title() {
      return I18n.get("swift.menu.profiles");
   }

   @Override
   public void layout(int width, int height) {
      super.layout(width, height);
      this.rafraichir();
      if (this.focus.isBlank() || !this.noms.contains(this.focus)) {
         this.focus = this.actif.isBlank() && !this.noms.isEmpty() ? this.noms.get(0) : this.actif;
      }
   }

   private void rafraichir() {
      this.noms = Profiles.noms();
      this.actif = Profiles.actif();
   }

   private int pad() {
      return 16;
   }

   private int railX() {
      return this.pad();
   }

   private int detailX() {
      return this.railX() + RAIL_W + 18;
   }

   private int detailW() {
      return Math.max(120, this.width - this.detailX() - this.pad());
   }

   private int listTop() {
      return 40;
   }

   private int[] rowRect(int i) {
      return new int[]{this.railX(), this.listTop() + i * ROW_H, RAIL_W, ROW_H - 2};
   }

   private int[] rectAjout() {
      return new int[]{this.railX(), this.listTop() + this.noms.size() * ROW_H + 6, RAIL_W, 22};
   }

   private int[] rectActiver() {
      return new int[]{this.detailX(), 118, Math.min(120, this.detailW()), 24};
   }

   private int[] rectPartager() {
      return new int[]{this.detailX(), 152, Math.min(120, this.detailW()), 24};
   }

   private int[] rectImporter() {
      int[] a = this.rectPartager();
      return new int[]{a[0] + a[2] + 8, a[1], Math.min(120, this.detailW() - a[2] - 8), 24};
   }

   private void bouton(Canvas c, int[] r, String label, int mouseX, int mouseY) {
      boolean over = Kit.dedans(mouseX, mouseY, r);
      c.card(r[0], r[1], r[2], r[3], over ? -1441125824 : -1728053248, over ? ACCENT : 419430399, 1, 5.0F);
      c.centeredText(Kit.tronque(c, label, r[2] - 8), r[0] + r[2] / 2, r[1] + (r[3] - 8) / 2, over ? -1 : -6644317, false);
   }

   private int[] rectSupprimer() {
      int[] a = this.rectActiver();
      return new int[]{a[0] + a[2] + 8, a[1], Math.min(100, this.detailW() - a[2] - 8), 24};
   }

   private void pose(String s) {
      this.statut = s;
      this.statutMs = System.currentTimeMillis();
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      this.curseur++;
      this.rafraichir();
      if (this.focus.isBlank() || !this.noms.contains(this.focus)) {
         this.focus = this.actif;
      }

      String titre = this.title();
      c.text(titre, this.pad(), 14, -1, false);
      c.text(String.valueOf(this.noms.size()), this.pad() + c.textWidth(titre) + 8, 14, -10394518, false);
      c.fill(this.pad(), 28, this.pad() + 96, 29, 1715176182);

      // Soft vertical rule between rail and detail
      int ruleX = this.railX() + RAIL_W + 8;
      c.fill(ruleX, this.listTop() - 4, ruleX + 1, this.height - 20, 352321535);

      for (int i = 0; i < this.noms.size(); i++) {
         this.dessinerRail(c, this.noms.get(i), i, mouseX, mouseY);
      }

      int[] add = this.rectAjout();
      if (this.saisie) {
         this.dessinerSaisie(c, add);
      } else {
         boolean over = Kit.dedans(mouseX, mouseY, add);
         c.text(I18n.get("swift.profiles.new_short"), add[0] + 2, add[1] + 6, over ? ACCENT : -6644317, false);
      }

      this.dessinerDetail(c, mouseX, mouseY);
   }

   private void dessinerRail(Canvas c, String n, int i, int mouseX, int mouseY) {
      int[] r = this.rowRect(i);
      boolean choisi = n.equals(this.focus);
      boolean actif = n.equals(this.actif);
      boolean survol = Kit.dedans(mouseX, mouseY, r);
      if (choisi) {
         c.fill(r[0], r[1] + 4, r[0] + 2, r[1] + r[3] - 4, ACCENT);
      } else if (survol) {
         c.fill(r[0], r[1] + 4, r[0] + 2, r[1] + r[3] - 4, 872415231);
      }

      int col = choisi || survol ? -1 : -6644317;
      String label = Kit.tronque(c, n, RAIL_W - 18);
      c.text(label, r[0] + 10, r[1] + (r[3] - 8) / 2, col, false);
      if (actif) {
         c.text("●", r[0] + RAIL_W - 12, r[1] + (r[3] - 8) / 2, ACCENT, false);
      }
   }

   private void dessinerDetail(Canvas c, int mouseX, int mouseY) {
      if (this.focus == null || this.focus.isBlank()) {
         return;
      }

      int x = this.detailX();
      int w = this.detailW();
      boolean estActif = this.focus.equals(this.actif);
      int nb = Profiles.modulesActifs(this.focus);

      c.text(Kit.tronque(c, this.focus, w), x, 44, -1, false);
      String etat = estActif
         ? I18n.get("swift.profiles.status_active")
         : I18n.get("swift.profiles.status_idle");
      c.text(etat, x, 60, estActif ? ACCENT : -10394518, false);
      c.text(I18n.get("swift.profiles.mods_n", nb), x, 78, -6644317, false);
      c.text(Kit.tronque(c, I18n.get("swift.profiles.hint"), w), x, 96, -10394518, false);

      if (!estActif) {
         int[] act = this.rectActiver();
         boolean overA = Kit.dedans(mouseX, mouseY, act);
         c.card(act[0], act[1], act[2], act[3], overA ? -1441125824 : -1728053248, overA ? ACCENT : 419430399, 1, 5.0F);
         c.centeredText(I18n.get("swift.profiles.activate"), act[0] + act[2] / 2, act[1] + (act[3] - 8) / 2, overA ? -1 : -6644317, false);

         if (this.noms.size() > 1) {
            int[] del = this.rectSupprimer();
            boolean overD = Kit.dedans(mouseX, mouseY, del);
            c.card(del[0], del[1], del[2], del[3], overD ? -1441125824 : -1728053248, overD ? -2067601 : 419430399, 1, 5.0F);
            c.centeredText(I18n.get("swift.profiles.delete"), del[0] + del[2] / 2, del[1] + (del[3] - 8) / 2, overD ? -2067601 : -6644317, false);
         }
      } else {
         c.text(I18n.get("swift.profiles.using"), x, 122, -10394518, false);
      }

      this.bouton(c, this.rectPartager(), I18n.get("swift.profiles.share"), mouseX, mouseY);
      this.bouton(c, this.rectImporter(), I18n.get("swift.profiles.import"), mouseX, mouseY);

      if (!this.statut.isBlank() && System.currentTimeMillis() - this.statutMs < 4000L) {
         c.text(Kit.tronque(c, this.statut, w), x, this.height - 28, -6644317, false);
      }
   }

   private void dessinerSaisie(Canvas c, int[] a) {
      c.fill(a[0], a[1] + a[3] - 1, a[0] + a[2], a[1] + a[3], ACCENT);
      boolean vide = this.nom.isEmpty();
      String ph = I18n.get("swift.profiles.name_ph_short");
      c.text(vide ? ph : Kit.tronque(c, this.nom, a[2] - 4), a[0] + 2, a[1] + 6, vide ? -10394518 : -1, false);
      if (this.curseur / 20L % 2L == 0L) {
         c.text("_", a[0] + 2 + (vide ? 0 : Math.min(c.textWidth(this.nom), a[2] - 8)), a[1] + 6, -1, false);
      }
   }

   @Override
   public boolean click(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      }

      if (this.saisie) {
         if (!Kit.dedans(mouseX, mouseY, this.rectAjout())) {
            this.saisie = false;
         }
         return true;
      }

      int[] add = this.rectAjout();
      if (Kit.dedans(mouseX, mouseY, add)) {
         this.saisie = true;
         this.nom = "";
         Platform.game().playClick();
         return true;
      }

      for (int i = 0; i < this.noms.size(); i++) {
         if (Kit.dedans(mouseX, mouseY, this.rowRect(i))) {
            this.focus = this.noms.get(i);
            Platform.game().playClick();
            return true;
         }
      }

      if (Kit.dedans(mouseX, mouseY, this.rectPartager())) {
         Platform.game().playClick();
         Platform.game().copyToClipboard(Profiles.codePartage(this.focus));
         this.pose(I18n.get("swift.profiles.code_copied", this.focus));
         return true;
      }

      if (Kit.dedans(mouseX, mouseY, this.rectImporter())) {
         Platform.game().playClick();

         try {
            String nom = Profiles.importerCode(Platform.game().readClipboard());
            this.noms = Profiles.noms();
            this.focus = nom;
            this.pose(I18n.get("swift.profiles.imported", nom));
         } catch (IllegalArgumentException e) {
            this.pose(e.getMessage());
         }

         return true;
      }

      if (!this.focus.equals(this.actif) && Kit.dedans(mouseX, mouseY, this.rectActiver())) {
         Platform.game().playClick();
         Profiles.activer(this.focus);
         this.pose(I18n.get("swift.profiles.switched", this.focus));
         return true;
      }

      if (!this.focus.equals(this.actif) && this.noms.size() > 1 && Kit.dedans(mouseX, mouseY, this.rectSupprimer())) {
         String n = this.focus;
         Platform.game().playClick();
         if (Profiles.supprimer(n)) {
            this.pose(I18n.get("swift.profiles.deleted", n));
            this.focus = Profiles.actif();
         }
         return true;
      }

      return false;
   }

   @Override
   public boolean charTyped(String s) {
      if (!this.saisie) {
         return false;
      }
      boolean espace = s.isBlank();
      if (this.nom.length() < NOM_MAX && (!espace || !this.nom.isEmpty())) {
         this.nom = this.nom + s;
      }
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode) {
      if (!this.saisie) {
         return false;
      }
      if (keyCode == 259) {
         if (!this.nom.isEmpty()) {
            this.nom = this.nom.substring(0, this.nom.length() - 1);
         }
         return true;
      }
      if (keyCode == 256) {
         this.saisie = false;
         return true;
      }
      if (keyCode == 257) {
         String n = this.nom.trim();
         if (n.isEmpty()) {
            this.saisie = false;
            return true;
         }
         if (Profiles.creer(n)) {
            this.focus = n;
            this.pose(I18n.get("swift.profiles.created", n));
            this.saisie = false;
         } else {
            this.pose(I18n.get("swift.profiles.exists", n));
         }
         Platform.game().playClick();
         return true;
      }
      return false;
   }
}
