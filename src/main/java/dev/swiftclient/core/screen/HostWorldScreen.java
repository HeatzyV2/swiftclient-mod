package dev.swiftclient.core.screen;

import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.social.SocialApi;
import dev.swiftclient.core.ui.Kit;
import dev.swiftclient.core.ui.UiScreen;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HostWorldScreen extends UiScreen {
   private static final String[] MODES = new String[]{"survival", "creative", "adventure", "spectator"};
   private static final String[] DIFFS = new String[]{"peaceful", "easy", "normal", "hard"};

   private static String modeLabel(int i) {
      return Tr.of("swift.host.mode." + MODES[i]);
   }

   private static String diffLabel(int i) {
      return Tr.of("swift.host.diff." + DIFFS[i]);
   }

   private static String[] modeLabels() {
      String[] out = new String[MODES.length];
      for (int i = 0; i < out.length; i++) {
         out[i] = modeLabel(i);
      }

      return out;
   }

   private static String[] diffLabels() {
      String[] out = new String[DIFFS.length];
      for (int i = 0; i < out.length; i++) {
         out[i] = diffLabel(i);
      }

      return out;
   }
   private static final int W = 300;
   private static final int PAD = 14;
   private static final int LIGNE = 26;
   private static final int CTRL_W = 128;
   private static final int CTRL_H = 18;
   private static final int BTN_H = 20;
   private static final int AMI_H = 24;
   private static final int AMIS_MAX = 4;
   private static final int OPT_H = 16;
   private HostWorldScreen.Etat etat = HostWorldScreen.Etat.REGLAGES;
   private int mode = 0;
   private int diff = 2;
   private boolean triche = false;
   private String adresse;
   private String erreur = "";
   private String statut = "";
   private long statutMs;
   private int deroule = -1;
   private volatile List<SocialApi.Friend> amis = List.of();
   private volatile boolean amisCharges;
   private final Set<String> invites = new HashSet<>();

   @Override
   public String title() {
      return Tr.of("swift.host.title");
   }

   @Override
   public void layout(int width, int height) {
      super.layout(width, height);
      String a = Platform.game().hostAddress();
      if (a != null) {
         this.adresse = a;
         this.etat = HostWorldScreen.Etat.HEBERGE;
         this.chargerAmis();
      }
   }

   private void pose(String s) {
      this.statut = s;
      this.statutMs = System.currentTimeMillis();
   }

   private void chargerAmis() {
      if (!this.amisCharges) {
         this.amisCharges = true;
         SocialApi.listFriends().thenAccept(res -> {
            if (!res.ok()) {
               this.pose(Tr.of("swift.host.friends_unavailable", res.error()));
               Platform.game().notify(Tr.of("swift.host.friends_toast"), res.error());
            }

            List<SocialApi.Friend> ok = new ArrayList<>();

            for (SocialApi.Friend f : res.value()) {
               if ("accepted".equals(f.status())) {
                  ok.add(f);
               }
            }

            ok.sort((a, b) -> Boolean.compare(b.online(), a.online()));
            this.amis = ok;
         }).exceptionally(e -> null);
      }
   }

   private String monde() {
      String n = Platform.game().worldName();
      return n != null && !n.isBlank() ? n : Tr.of("swift.host.your_world");
   }

   private boolean horsSolo() {
      return !Platform.game().inSingleplayer() && this.etat != HostWorldScreen.Etat.HEBERGE;
   }

   private int hauteur() {
      if (this.horsSolo()) {
         return 104;
      } else if (this.etat == HostWorldScreen.Etat.HEBERGE) {
         int n = Math.max(1, Math.min(4, this.amis.size()));
         return 116 + n * 27 + 12 + 20 + 14;
      } else {
         return 128 + (this.erreur.isBlank() ? 0 : 14) + 16 + 20 + 14;
      }
   }

   private int fx() {
      return (this.width - 300) / 2;
   }

   private int fy() {
      return Math.max(8, (this.height - this.hauteur()) / 2);
   }

   private int yContenu() {
      return this.fy() + 14 + 12 + 16 + 8;
   }

   private int[] rectCtrl(int i) {
      int y = this.yContenu() + i * 26;
      return new int[]{this.fx() + 300 - 14 - 128, y + 4, 128, 18};
   }

   private int[] rectOption(int menu, int i) {
      int[] r = this.rectCtrl(menu);
      return new int[]{r[0] + 2, r[1] + r[3] + 4 + i * 16, r[2] - 4, 16};
   }

   private int[] rectGauche() {
      int y = this.fy() + this.hauteur() - 14 - 20;
      return new int[]{this.fx() + 14, y, 132, 20};
   }

   private int[] rectDroite() {
      int[] g = this.rectGauche();
      return new int[]{g[0] + g[2] + 8, g[1], g[2], 20};
   }

   private int[] rectCopier() {
      return new int[]{this.fx() + 300 - 14 - 56, this.yContenu() + 6, 48, 18};
   }

   private int yAmis() {
      return this.yContenu() + 30 + 18 + 18;
   }

   private int[] rectInviter(int y) {
      return new int[]{this.fx() + 300 - 14 - 58, y + 4, 52, 16};
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      int x = this.fx();
      int y = this.fy();
      int h = this.hauteur();
      c.card(x, y, 300, h, -233564904, 520093695, 1, 9.0F);
      if (this.horsSolo()) {
         c.text(Tr.of("swift.host.title"), x + 14, y + 14, -1, false);
         c.text(Tr.of("swift.host.no_world.1"), x + 14, y + 14 + 18, -6644317, false);
         c.text(Tr.of("swift.host.no_world.2"), x + 14, y + 14 + 30, -6644317, false);
         int[] r = new int[]{x + 14, y + h - 14 - 20, 272, 20};
         boutonCreux(c, r, Tr.of("swift.host.back"), "back", Kit.dedans(mouseX, mouseY, r));
      } else if (this.etat == HostWorldScreen.Etat.HEBERGE) {
         this.dessinerEnLigne(c, mouseX, mouseY);
      } else {
         c.text(Tr.of("swift.host.configure"), x + 14, y + 14, -1, false);
         c.text(Kit.tronque(c, Tr.of("swift.host.choose_how", this.monde()), 272), x + 14, y + 14 + 16, -10394518, false);
         boolean fige = this.etat == HostWorldScreen.Etat.DEMARRAGE;
         String[] libelles = new String[]{Tr.of("swift.host.game_mode"), Tr.of("swift.host.difficulty"), Tr.of("swift.host.cheats")};

         for (int i = 0; i < 3; i++) {
            int yl = this.yContenu() + i * 26;
            c.text(libelles[i], x + 14, yl + 9, -1, false);
            int[] r = this.rectCtrl(i);
            boolean survol = !fige && this.deroule < 0 && Kit.dedans(mouseX, mouseY, r);
            if (i < 2) {
               selecteur(c, r, i == 0 ? Tr.of("swift.host.mode") : Tr.of("swift.host.difficulty"), i == 0 ? modeLabel(this.mode) : diffLabel(this.diff), this.deroule == i, survol);
            } else {
               caseACocher(c, r, Tr.of("swift.host.allow_cheats"), this.triche, survol);
            }
         }

         if (!this.erreur.isBlank()) {
            c.text(Kit.tronque(c, this.erreur, 272), x + 14, this.yContenu() + 78 + 4, -2067601, false);
         }

         int[] g = this.rectGauche();
         int[] d = this.rectDroite();
         boutonCreux(c, g, Tr.of("swift.host.back"), "chevron_right", this.deroule < 0 && Kit.dedans(mouseX, mouseY, g));
         boutonPlein(c, d, fige ? Tr.of("swift.host.starting") : (this.erreur.isBlank() ? Tr.of("swift.host.next") : Tr.of("swift.host.retry")), !fige && this.deroule < 0 && Kit.dedans(mouseX, mouseY, d));
         if (this.deroule >= 0) {
            String[] opts = this.deroule == 0 ? modeLabels() : diffLabels();
            int choisi = this.deroule == 0 ? this.mode : this.diff;
            int[] r0 = this.rectOption(this.deroule, 0);
            c.card(r0[0] - 2, r0[1] - 2, r0[2] + 4, opts.length * 16 + 4, -14934751, 872415231, 1, 6.0F);

            for (int ix = 0; ix < opts.length; ix++) {
               int[] r = this.rectOption(this.deroule, ix);
               boolean s = Kit.dedans(mouseX, mouseY, r);
               if (s) {
                  c.roundRect(r[0], r[1], r[2], r[3], 4.0F, -14013648);
               }

               c.text(opts[ix], r[0] + 7, r[1] + 4, ix != choisi && !s ? -6644317 : -1, false);
               if (ix == choisi) {
                  c.icon("check", r[0] + r[2] - 14, r[1] + 3, 9, -12868259);
               }
            }
         }
      }
   }

   private void dessinerEnLigne(Canvas c, int mouseX, int mouseY) {
      int x = this.fx();
      int y = this.fy();
      c.text(Tr.of("swift.host.live"), x + 14, y + 14, -1, false);
      c.roundRect(x + 14 + c.textWidth(Tr.of("swift.host.live")) + 6, y + 14 + 2, 5, 5, 2.5F, -12868259);
      c.text(Kit.tronque(c, Tr.of("swift.host.join_with", this.monde()), 272), x + 14, y + 14 + 16, -10394518, false);
      int ya = this.yContenu();
      c.card(x + 14, ya, 272, 30, -15461097, 520093695, 1, 6.0F);
      c.text(Tr.of("swift.host.address"), x + 14 + 9, ya + 5, -10394518, false);
      c.text(Kit.tronque(c, this.adresse, 192), x + 14 + 9, ya + 17, -1, false);
      int[] rc = this.rectCopier();
      boolean copie = !this.statut.isBlank() && this.statut.equals(Tr.of("swift.host.copied")) && System.currentTimeMillis() - this.statutMs < 2000L;
      Kit.bouton(c, rc, copie ? Tr.of("swift.host.copied") : Tr.of("swift.host.copy"), Kit.dedans(mouseX, mouseY, rc));
      c.text(Kit.tronque(c, Tr.of("swift.host.direct_connect"), 272), x + 14, ya + 35, -10394518, false);
      int yA = this.yAmis();
      Kit.section(c, x + 14, yA - 11, Tr.of("swift.host.invite_friends"));
      List<SocialApi.Friend> liste = this.amis;
      if (liste.isEmpty()) {
         c.text(this.amisCharges ? Tr.of("swift.host.no_friends") : Tr.of("swift.common.loading"), x + 14, yA + 8, -10394518, false);
      }

      int yl = yA;

      for (int i = 0; i < Math.min(4, liste.size()); i++) {
         SocialApi.Friend f = liste.get(i);
         boolean survol = Kit.dedans(mouseX, mouseY, x + 14, yl, 272, 24);
         c.card(x + 14, yl, 272, 24, survol ? -14605786 : -15461097, survol ? 872415231 : 520093695, 1, 6.0F);
         c.playerHead(f.uuid(), x + 14 + 6, yl + 6, 12);
         c.text(Kit.tronque(c, f.username(), 162), x + 14 + 24, yl + 8, -1, false);
         c.roundRect(x + 14 + 24 + c.textWidth(Kit.tronque(c, f.username(), 162)) + 5, yl + 12 - 2, 4, 4, 2.0F, f.online() ? -12868259 : -12960962);
         int[] ri = this.rectInviter(yl);
         if (this.invites.contains(f.uuid())) {
            c.centeredText(Tr.of("swift.host.invited"), ri[0] + ri[2] / 2, ri[1] + 4, -10394518, false);
         } else {
            Kit.bouton(c, ri, Tr.of("swift.host.invite"), Kit.dedans(mouseX, mouseY, ri));
         }

         yl += 27;
      }

      int[] g = this.rectGauche();
      int[] d = this.rectDroite();
      boolean sg = Kit.dedans(mouseX, mouseY, g);
      c.card(g[0], g[1], g[2], g[3], sg ? 585134959 : 0, sg ? -2067601 : 872415231, 1, 6.0F);
      c.centeredText(Tr.of("swift.host.stop"), g[0] + g[2] / 2, g[1] + (g[3] - 8) / 2, -2067601, false);
      boutonPlein(c, d, Tr.of("swift.common.done"), Kit.dedans(mouseX, mouseY, d));
      if (!this.statut.isBlank() && !this.statut.equals(Tr.of("swift.host.copied")) && System.currentTimeMillis() - this.statutMs < 3000L) {
         c.centeredText(this.statut, x + 150, this.fy() + this.hauteur() + 6, -6644317, false);
      }
   }

   private static void selecteur(Canvas c, int[] r, String intitule, String valeur, boolean ouvert, boolean survol) {
      c.card(r[0], r[1], r[2], r[3], !survol && !ouvert ? -15461097 : -14605786, ouvert ? -1578515 : (survol ? 872415231 : 520093695), 1, 5.0F);
      c.text(intitule, r[0] + 7, r[1] + (r[3] - 8) / 2, -10394518, false);
      int vx = r[0] + r[2] - 18 - c.textWidth(valeur);
      c.text(valeur, vx, r[1] + (r[3] - 8) / 2, -1, false);
      c.icon(ouvert ? "chevron_up" : "chevron_down", r[0] + r[2] - 14, r[1] + (r[3] - 8) / 2, 8, -6644317);
   }

   private static void caseACocher(Canvas c, int[] r, String libelle, boolean coche, boolean survol) {
      c.card(r[0], r[1], r[2], r[3], survol ? -14605786 : -15461097, survol ? 872415231 : 520093695, 1, 5.0F);
      c.text(libelle, r[0] + 7, r[1] + (r[3] - 8) / 2, -1, false);
      int bx = r[0] + r[2] - 16;
      int by = r[1] + (r[3] - 10) / 2;
      if (coche) {
         c.roundRect(bx, by, 10, 10, 3.0F, -12868259);
         c.icon("check", bx + 1, by + 1, 8, -1);
      } else {
         c.card(bx, by, 10, 10, 0, 872415231, 1, 3.0F);
      }
   }

   private static void boutonCreux(Canvas c, int[] r, String libelle, String icone, boolean survol) {
      c.card(r[0], r[1], r[2], r[3], survol ? -14539993 : 0, survol ? 872415231 : 872415231, 1, 6.0F);
      int tw = c.textWidth(libelle);
      int ix = r[0] + (r[2] - tw - 11) / 2;
      c.icon(icone, ix, r[1] + (r[3] - 7) / 2, 7, survol ? -1 : -6644317);
      c.text(libelle, ix + 11, r[1] + (r[3] - 8) / 2, survol ? -1 : -6644317, false);
   }

   private static void boutonPlein(Canvas c, int[] r, String libelle, boolean survol) {
      c.roundRect(r[0], r[1], r[2], r[3], 6.0F, survol ? -12208022 : -12868259);
      int tw = c.textWidth(libelle);
      int ix = r[0] + (r[2] - tw - 11) / 2;
      c.icon("chevron_right", ix, r[1] + (r[3] - 7) / 2, 7, -1);
      c.text(libelle, ix + 11, r[1] + (r[3] - 8) / 2, -1, false);
   }

   @Override
   public boolean click(double mx, double my, int button) {
      if (button != 0) {
         return false;
      } else if (this.horsSolo()) {
         int[] r = new int[]{this.fx() + 14, this.fy() + this.hauteur() - 14 - 20, 272, 20};
         if (Kit.dedans(mx, my, r)) {
            Platform.game().playClick();
            this.back();
            return true;
         } else {
            return false;
         }
      } else if (this.etat == HostWorldScreen.Etat.HEBERGE) {
         return this.clicEnLigne(mx, my);
      } else if (this.deroule < 0) {
         if (this.etat == HostWorldScreen.Etat.DEMARRAGE) {
            return true;
         } else {
            for (int i = 0; i < 3; i++) {
               if (Kit.dedans(mx, my, this.rectCtrl(i))) {
                  Platform.game().playClick();
                  if (i < 2) {
                     this.deroule = i;
                  } else {
                     this.triche = !this.triche;
                  }

                  return true;
               }
            }

            if (Kit.dedans(mx, my, this.rectGauche())) {
               Platform.game().playClick();
               this.back();
               return true;
            } else if (Kit.dedans(mx, my, this.rectDroite())) {
               Platform.game().playClick();
               this.demarrer();
               return true;
            } else {
               return false;
            }
         }
      } else {
         String[] opts = this.deroule == 0 ? modeLabels() : diffLabels();

         for (int ix = 0; ix < opts.length; ix++) {
            if (Kit.dedans(mx, my, this.rectOption(this.deroule, ix))) {
               if (this.deroule == 0) {
                  this.mode = ix;
               } else {
                  this.diff = ix;
               }

               Platform.game().playClick();
               break;
            }
         }

         this.deroule = -1;
         return true;
      }
   }

   private boolean clicEnLigne(double mx, double my) {
      if (Kit.dedans(mx, my, this.rectCopier())) {
         Platform.game().copyToClipboard(this.adresse);
         Platform.game().playClick();
         this.pose(Tr.of("swift.host.copied"));
         return true;
      } else {
         int y = this.yAmis();
         List<SocialApi.Friend> liste = this.amis;

         for (int i = 0; i < Math.min(4, liste.size()); i++) {
            SocialApi.Friend f = liste.get(i);
            if (Kit.dedans(mx, my, this.rectInviter(y)) && !this.invites.contains(f.uuid())) {
               Platform.game().playClick();
               this.inviter(f);
               return true;
            }

            y += 27;
         }

         if (Kit.dedans(mx, my, this.rectGauche())) {
            Platform.game().playClick();
            this.arreter();
            return true;
         } else if (Kit.dedans(mx, my, this.rectDroite())) {
            Platform.game().playClick();
            this.back();
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public boolean keyPressed(int keyCode) {
      if (keyCode == 256 && this.deroule >= 0) {
         this.deroule = -1;
         return true;
      } else {
         return false;
      }
   }

   private void demarrer() {
      this.etat = HostWorldScreen.Etat.DEMARRAGE;
      this.erreur = "";
      Platform.game().hostWorld(MODES[this.mode], DIFFS[this.diff], this.triche).whenComplete((a, err) -> {
         if (err != null) {
            Throwable cause = err.getCause() != null ? err.getCause() : err;
            this.erreur = Tr.of("swift.host.failed", cause.getMessage() != null ? cause.getMessage() : String.valueOf(cause));
            Platform.game().notify(Tr.of("swift.host.failed_toast"), cause.getMessage() != null ? cause.getMessage() : String.valueOf(cause));
            this.etat = HostWorldScreen.Etat.REGLAGES;
         } else {
            this.adresse = a;
            this.etat = HostWorldScreen.Etat.HEBERGE;
            this.chargerAmis();
         }
      });
   }

   private void arreter() {
      Platform.game().stopHosting();
      this.etat = HostWorldScreen.Etat.REGLAGES;
      this.adresse = null;
      this.invites.clear();
      this.pose(Tr.of("swift.host.stopped"));
   }

   private void inviter(SocialApi.Friend f) {
      this.invites.add(f.uuid());
      SocialApi.sendDM(f.uuid(), "swift-invite://" + this.adresse).thenAccept(res -> {
         if (res.ok()) {
            this.pose(Tr.of("swift.host.invite_sent", f.username()));
         } else {
            this.invites.remove(f.uuid());
            this.pose(Tr.of("swift.host.invite_failed", res.error()));
            Platform.game().notify(Tr.of("swift.host.invite_failed_toast"), res.error());
         }
      });
   }

   private static enum Etat {
      REGLAGES,
      DEMARRAGE,
      HEBERGE;
   }
}
