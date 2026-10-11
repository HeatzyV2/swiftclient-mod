package dev.swiftclient.core.screen;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.social.SocialApi;
import dev.swiftclient.core.ui.Defilement;
import dev.swiftclient.core.ui.Kit;
import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.ui.UiScreen;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Host the current singleplayer world for friends. Three states: settings (game mode, difficulty, cheats),
 * starting, and live (address to share, friends to invite). Wide windows get a side card, narrow ones stack.
 * Behaviour and backend calls are those of the previous screen.
 */
public class HostWorldScreen extends UiScreen {
   private static final String[] MODES = new String[]{"survival", "creative", "adventure", "spectator"};
   private static final String[] DIFFS = new String[]{"peaceful", "easy", "normal", "hard"};
   private static final String[] MODE_ICONS = new String[]{"sword", "sparkles", "map", "eye"};
   private static final int PAD = 12;
   private static final int HEAD_H = 30;
   private static final int ROW_H = 24;
   private static final int FRIEND_GAP = 4;
   private static final int OK = 0xFF3ECF8E;

   private Etat etat = Etat.REGLAGES;
   private int mode = 0;
   private int diff = 2;
   private boolean triche = false;
   private String adresse;
   private String erreur = "";
   private String statut = "";
   private long statutMs;
   private volatile List<SocialApi.Friend> amis = List.of();
   private volatile boolean amisCharges;
   private final Set<String> invites = new HashSet<>();
   private final Defilement defil = new Defilement();
   private final long born = System.nanoTime();

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
         this.etat = Etat.HEBERGE;
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
      return !Platform.game().inSingleplayer() && this.etat != Etat.HEBERGE;
   }

   // --- Geometry ---

   private boolean wide() {
      return this.width >= 420;
   }

   private int top() {
      return HEAD_H + 6;
   }

   private int bottom() {
      return this.height - 8;
   }

   private int leftW() {
      return this.wide() ? Math.min(176, Math.round(this.width * 0.36F)) : this.width - 2 * PAD;
   }

   private int rightX() {
      return this.wide() ? PAD + this.leftW() + 10 : PAD;
   }

   private int rightW() {
      return this.width - PAD - this.rightX();
   }

   private int[] btnBack() {
      int w = Math.min(92, this.rightW() / 3);
      return new int[]{this.rightX(), this.bottom() - 22, w, 22};
   }

   private int[] btnMain() {
      int[] b = this.btnBack();
      int w = Math.min(150, this.rightW() - b[2] - 8);
      return new int[]{this.rightX() + this.rightW() - w, b[1], w, 22};
   }

   // Settings

   private int rowsY() {
      return this.wide() ? this.top() : this.top() + 14;
   }

   private int labelW() {
      return Math.max(60, Math.min(92, this.rightW() / 4));
   }

   private int ctrlX() {
      return this.rightX() + this.labelW();
   }

   private int ctrlW() {
      return this.rightW() - this.labelW();
   }

   private int rowY(int i) {
      return this.rowsY() + i * 30;
   }

   private int[] chip(int row, int i, int n) {
      int gap = 4;
      int w = (this.ctrlW() - (n - 1) * gap) / n;
      return new int[]{this.ctrlX() + i * (w + gap), this.rowY(row) + 1, w, 22};
   }

   private int[] cheatsRect() {
      return new int[]{this.ctrlX(), this.rowY(2) + 1, this.ctrlW(), 22};
   }

   // Live

   private int[] copyRect() {
      if (this.wide()) {
         return new int[]{PAD + 8, this.top() + 78, this.leftW() - 16, 20};
      }

      int w = 70;
      return new int[]{PAD + this.leftW() - 8 - w, this.top() + 11, w, 20};
   }

   private int[] stopRect() {
      return this.wide()
         ? new int[]{PAD + 8, this.bottom() - 28, this.leftW() - 16, 20}
         : new int[]{PAD, this.bottom() - 22, Math.min(120, this.width / 3), 22};
   }

   private int[] doneRect() {
      return this.wide()
         ? new int[]{this.rightX() + this.rightW() - 100, this.bottom() - 22, 100, 22}
         : new int[]{this.width - PAD - 100, this.bottom() - 22, 100, 22};
   }

   private int friendsTop() {
      return this.wide() ? this.top() + 16 : this.top() + 62 - 6;
   }

   private int friendsBottom() {
      return this.bottom() - 28;
   }

   private int[] inviteRect(int y) {
      return new int[]{this.rightX() + this.rightW() - 8 - 56, y + 4, 56, 16};
   }

   // --- Drawing ---

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      this.defil.anime();
      this.drawHeader(c);
      if (this.horsSolo()) {
         this.drawNoWorld(c, mouseX, mouseY);
      } else if (this.etat == Etat.HEBERGE) {
         this.drawLive(c, mouseX, mouseY);
      } else {
         this.drawSettings(c, mouseX, mouseY);
      }

      if (!this.statut.isBlank() && !this.statut.equals(Tr.of("swift.host.copied")) && System.currentTimeMillis() - this.statutMs < 3500L) {
         String t = Kit.tronque(c, this.statut, this.width - 2 * PAD - 120);
         c.text(t, this.width - PAD - c.textWidth(t), 11, Px.DIM, false);
      }
   }

   private void drawHeader(Canvas c) {
      Px.title(c, this.title(), PAD, 6);
      if (this.etat == Etat.HEBERGE || this.etat == Etat.DEMARRAGE) {
         boolean live = this.etat == Etat.HEBERGE;
         String badge = (live ? Tr.of("swift.host.live_badge") : Tr.of("swift.host.starting")).toUpperCase(Locale.ROOT);
         int x = PAD + c.textWidth(this.title().toUpperCase(Locale.ROOT)) + 16;
         int w = c.textWidth(badge) + 22;
         c.card(x, 7, w, 16, live ? 0x223ECF8E : 0x22F5B544, live ? 0x663ECF8E : 0x66F5B544, 1, 8.0F);
         float pulse = 0.55F + 0.45F * (float)Math.abs(Math.sin((System.nanoTime() - this.born) / 4.0e8));
         int dot = live ? OK : 0xFFF5B544;
         c.roundRect(x + 7, 12, 6, 6, 3.0F, (Math.round(255 * pulse) << 24) | (dot & 0xFFFFFF));
         c.text(badge, x + 17, 11, live ? OK : 0xFFF5B544, false);
      }

      c.fill(PAD, HEAD_H, this.width - PAD, HEAD_H + 1, 0x1AFFFFFF);
   }

   private void drawNoWorld(Canvas c, int mouseX, int mouseY) {
      int cx = this.width / 2;
      int cy = (this.top() + this.bottom()) / 2 - 10;
      c.card(cx - 22, cy - 40, 44, 44, 0xFF11151F, 0xFF2A3350, 1, 8.0F);
      c.icon("host", cx - 11, cy - 29, 22, Px.DIM);
      c.centeredText(Tr.of("swift.host.no_world.1"), cx, cy + 12, Px.DIM, false);
      c.centeredText(Tr.of("swift.host.no_world.2"), cx, cy + 24, Px.DIM, false);
      int[] r = {cx - 50, cy + 42, 100, 22};
      Px.button(c, r[0], r[1], r[2], r[3], Tr.of("swift.host.back"), Kit.dedans(mouseX, mouseY, r), true);
   }

   private void sideCard(Canvas c, int x, int y, int w, int h) {
      c.card(x, y, w, h, 0xFF0D1119, 0xFF1B2438, 1, 6.0F);
   }

   private void drawSettings(Canvas c, int mouseX, int mouseY) {
      boolean frozen = this.etat == Etat.DEMARRAGE;
      if (this.wide()) {
         int x = PAD;
         int y = this.top();
         int w = this.leftW();
         int h = this.bottom() - y;
         this.sideCard(c, x, y, w, h);
         c.card(x + 10, y + 10, 30, 30, Px.ACCENT & 0x00FFFFFF | 0x33000000, Px.ACCENT, 1, 6.0F);
         c.icon("host", x + 16, y + 16, 18, -1);
         c.text(Kit.tronque(c, this.monde(), w - 20), x + 10, y + 48, -1, true);
         int ty = y + 62;
         for (String line : wrap(c, Tr.of("swift.host.choose_how", this.monde()), w - 20, 4)) {
            c.text(line, x + 10, ty, Px.DIM, false);
            ty += 10;
         }

         // Who can join: said once, at the bottom of the card
         int ny = y + h - 10 - 10 * Math.min(3, wrap(c, Tr.of("swift.host.note"), w - 34, 3).size());
         c.icon("shield", x + 10, ny, 10, Px.FAINT);
         for (String line : wrap(c, Tr.of("swift.host.note"), w - 34, 3)) {
            c.text(line, x + 24, ny + 1, Px.FAINT, false);
            ny += 10;
         }
      } else {
         c.text(Kit.tronque(c, Tr.of("swift.host.choose_how", this.monde()), this.width - 2 * PAD), PAD, this.top(), Px.DIM, false);
      }

      String[] labels = {Tr.of("swift.host.game_mode"), Tr.of("swift.host.difficulty"), Tr.of("swift.host.cheats")};
      for (int i = 0; i < 3; i++) {
         c.text(Kit.tronque(c, labels[i], this.labelW() - 8), this.rightX(), this.rowY(i) + 8, i == 2 ? Px.DIM : -1, false);
      }

      for (int i = 0; i < MODES.length; i++) {
         int[] r = this.chip(0, i, MODES.length);
         this.drawChip(c, r, Tr.of("swift.host.mode." + MODES[i]), MODE_ICONS[i], this.mode == i, !frozen && Kit.dedans(mouseX, mouseY, r), frozen);
      }

      for (int i = 0; i < DIFFS.length; i++) {
         int[] r = this.chip(1, i, DIFFS.length);
         this.drawChip(c, r, Tr.of("swift.host.diff." + DIFFS[i]), null, this.diff == i, !frozen && Kit.dedans(mouseX, mouseY, r), frozen);
      }

      int[] cr = this.cheatsRect();
      boolean over = !frozen && Kit.dedans(mouseX, mouseY, cr);
      c.card(cr[0], cr[1], cr[2], cr[3], over ? 0xFF182033 : 0xFF11151F, over ? 0xFF3A4766 : 0, 1, 4.0F);
      c.text(Kit.tronque(c, Tr.of("swift.host.allow_cheats"), cr[2] - 48), cr[0] + 8, cr[1] + 7, this.triche ? -1 : Px.DIM, false);
      Px.toggle(c, cr[0] + cr[2] - 34, cr[1] + 5, this.triche);

      if (!this.erreur.isBlank()) {
         int ey = this.rowY(2) + 30;
         int lines = 0;
         for (String line : wrap(c, this.erreur, this.rightW(), 2)) {
            c.text(line, this.rightX(), ey + lines * 10, Px.DANGER, false);
            lines++;
         }
      }

      int[] back = this.btnBack();
      int[] main = this.btnMain();
      Px.button(c, back[0], back[1], back[2], back[3], Tr.of("swift.host.back"), !frozen && Kit.dedans(mouseX, mouseY, back), false);
      String go = frozen ? Tr.of("swift.host.starting") : (this.erreur.isBlank() ? Tr.of("swift.host.start") : Tr.of("swift.host.retry"));
      Px.button(c, main[0], main[1], main[2], main[3], Kit.tronque(c, go, main[2] - 10), !frozen && Kit.dedans(mouseX, mouseY, main), !frozen);
   }

   private void drawChip(Canvas c, int[] r, String label, String icon, boolean on, boolean over, boolean frozen) {
      int dy = on ? 0 : (over ? -1 : 0);
      c.card(r[0], r[1] + dy, r[2], r[3], on ? Px.ACCENT : (over ? 0xFF182033 : 0xFF11151F), on ? 0 : (over ? 0xFF3A4766 : 0), 1, 4.0F);
      int tw = c.textWidth(label);
      boolean showIcon = icon != null && r[2] >= tw + 30;
      int total = tw + (showIcon ? 13 : 0);
      int x = r[0] + (r[2] - total) / 2;
      if (showIcon) {
         c.icon(icon, x, r[1] + dy + 6, 10, on ? -1 : Px.DIM);
         x += 13;
      }

      c.text(Kit.tronque(c, label, r[2] - 6), Math.max(r[0] + 3, x), r[1] + dy + 7, on || over ? -1 : (frozen ? Px.FAINT : Px.DIM), false);
   }

   private void drawLive(Canvas c, int mouseX, int mouseY) {
      int[] copy = this.copyRect();
      int[] stop = this.stopRect();
      int[] done = this.doneRect();
      boolean copied = !this.statut.isBlank() && this.statut.equals(Tr.of("swift.host.copied")) && System.currentTimeMillis() - this.statutMs < 2000L;
      String addr = this.adresse == null ? "" : this.adresse;
      if (this.wide()) {
         int x = PAD;
         int y = this.top();
         int w = this.leftW();
         int h = this.bottom() - y;
         this.sideCard(c, x, y, w, h);
         c.text(Kit.tronque(c, Tr.of("swift.host.live"), w - 20), x + 10, y + 10, -1, true);
         c.text(Kit.tronque(c, Tr.of("swift.host.join_with", this.monde()), w - 20), x + 10, y + 22, Px.FAINT, false);
         c.card(x + 8, y + 38, w - 16, 34, 0xFF080B11, 0xFF222B42, 1, 4.0F);
         c.text(Tr.of("swift.host.address"), x + 15, y + 43, Px.FAINT, false);
         c.text(Kit.tronque(c, addr, w - 30), x + 15, y + 56, Px.ACCENT_HI, false);
         Px.button(c, copy[0], copy[1], copy[2], copy[3], copied ? Tr.of("swift.host.copied") : Tr.of("swift.host.copy"), Kit.dedans(mouseX, mouseY, copy), !copied);
         int ty = y + 104;
         for (String line : wrap(c, Tr.of("swift.host.direct_connect"), w - 20, 3)) {
            c.text(line, x + 10, ty, Px.FAINT, false);
            ty += 10;
         }
      } else {
         int w = this.leftW();
         this.sideCard(c, PAD, this.top(), w, 42);
         c.text(Tr.of("swift.host.address"), PAD + 10, this.top() + 8, Px.FAINT, false);
         c.text(Kit.tronque(c, addr, w - 100), PAD + 10, this.top() + 22, Px.ACCENT_HI, false);
         Px.button(c, copy[0], copy[1], copy[2], copy[3], copied ? Tr.of("swift.host.copied") : Tr.of("swift.host.copy"), Kit.dedans(mouseX, mouseY, copy), !copied);
      }

      Kit.section(c, this.rightX(), this.friendsTop() - 13, Tr.of("swift.host.invite_friends"));
      this.drawFriends(c, mouseX, mouseY);

      boolean sOver = Kit.dedans(mouseX, mouseY, stop);
      c.card(stop[0], stop[1], stop[2], stop[3], sOver ? 0x33F2555A : 0, sOver ? Px.DANGER : 0xFF4A2A30, 1, 6.0F);
      c.centeredText(Kit.tronque(c, Tr.of("swift.host.stop"), stop[2] - 8), stop[0] + stop[2] / 2, stop[1] + (stop[3] - 8) / 2, Px.DANGER, false);
      Px.button(c, done[0], done[1], done[2], done[3], Tr.of("swift.common.done"), Kit.dedans(mouseX, mouseY, done), true);
   }

   private void drawFriends(Canvas c, int mouseX, int mouseY) {
      int top = this.friendsTop();
      int bottom = this.friendsBottom();
      List<SocialApi.Friend> liste = this.amis;
      int x = this.rightX();
      int w = this.rightW();
      if (liste.isEmpty()) {
         c.text(this.amisCharges ? Tr.of("swift.host.no_friends") : Tr.of("swift.common.loading"), x, top + 6, Px.FAINT, false);
         return;
      }

      this.defil.contenu(liste.size() * (ROW_H + FRIEND_GAP) - FRIEND_GAP, bottom - top);
      c.pushScissor(x - 2, top, w + 4, bottom - top);
      int y = top - this.defil.px();
      for (SocialApi.Friend f : liste) {
         if (y + ROW_H >= top && y <= bottom) {
            boolean over = Kit.dedans(mouseX, mouseY, x, y, w, ROW_H) && mouseY >= top && mouseY <= bottom;
            c.card(x, y, w, ROW_H, over ? 0xFF182033 : 0xFF11151F, over ? 0xFF3A4766 : 0, 1, 4.0F);
            c.fill(x + 5, y + 5, x + 19, y + 19, 0xFF000000);
            c.playerHead(f.uuid(), x + 6, y + 6, 12);
            String name = Kit.tronque(c, f.username(), w - 130);
            c.text(name, x + 26, y + 8, -1, false);
            c.roundRect(x + 26 + c.textWidth(name) + 6, y + 10, 5, 5, 2.5F, f.online() ? OK : 0xFF3A4152);
            int[] ri = this.inviteRect(y);
            if (this.invites.contains(f.id())) {
               c.icon("check", ri[0] + 6, ri[1] + 4, 8, OK);
               c.text(Tr.of("swift.host.invited"), ri[0] + 17, ri[1] + 4, Px.FAINT, false);
            } else {
               Px.button(c, ri[0], ri[1], ri[2], ri[3], Tr.of("swift.host.invite"), Kit.dedans(mouseX, mouseY, ri), false);
            }
         }

         y += ROW_H + FRIEND_GAP;
      }

      c.popScissor();
      Kit.barre(c, this.width - PAD + 2, top, bottom, this.defil.px(), this.defil.maxPx());
   }

   private static List<String> wrap(Canvas c, String text, int maxW, int maxLines) {
      List<String> out = new ArrayList<>();
      StringBuilder line = new StringBuilder();
      for (String word : text.split(" ")) {
         String next = line.length() == 0 ? word : line + " " + word;
         if (c.textWidth(next) > maxW && line.length() > 0) {
            out.add(line.toString());
            line = new StringBuilder(word);
         } else {
            line = new StringBuilder(next);
         }
      }

      if (line.length() > 0) {
         out.add(line.toString());
      }

      if (out.size() > maxLines) {
         List<String> cut = new ArrayList<>(out.subList(0, maxLines));
         cut.set(maxLines - 1, Kit.tronque(c, cut.get(maxLines - 1) + "…", maxW));
         return cut;
      }

      return out;
   }

   // --- Input ---

   @Override
   public boolean click(double mx, double my, int button) {
      if (button != 0) {
         return false;
      }

      if (this.horsSolo()) {
         int cx = this.width / 2;
         int cy = (this.top() + this.bottom()) / 2 - 10;
         if (Kit.dedans(mx, my, cx - 50, cy + 42, 100, 22)) {
            Platform.game().playClick();
            this.back();
            return true;
         }

         return false;
      }

      if (this.etat == Etat.HEBERGE) {
         return this.clickLive(mx, my);
      }

      if (this.etat == Etat.DEMARRAGE) {
         return true;
      }

      for (int i = 0; i < MODES.length; i++) {
         if (Kit.dedans(mx, my, this.chip(0, i, MODES.length))) {
            this.mode = i;
            Platform.game().playClick();
            return true;
         }
      }

      for (int i = 0; i < DIFFS.length; i++) {
         if (Kit.dedans(mx, my, this.chip(1, i, DIFFS.length))) {
            this.diff = i;
            Platform.game().playClick();
            return true;
         }
      }

      if (Kit.dedans(mx, my, this.cheatsRect())) {
         this.triche = !this.triche;
         Platform.game().playClick();
         return true;
      }

      if (Kit.dedans(mx, my, this.btnBack())) {
         Platform.game().playClick();
         this.back();
         return true;
      }

      if (Kit.dedans(mx, my, this.btnMain())) {
         Platform.game().playClick();
         this.demarrer();
         return true;
      }

      return false;
   }

   private boolean clickLive(double mx, double my) {
      if (Kit.dedans(mx, my, this.copyRect())) {
         Platform.game().copyToClipboard(this.adresse);
         Platform.game().playClick();
         this.pose(Tr.of("swift.host.copied"));
         return true;
      }

      if (my >= this.friendsTop() && my <= this.friendsBottom()) {
         int y = this.friendsTop() - this.defil.px();
         for (SocialApi.Friend f : this.amis) {
            if (Kit.dedans(mx, my, this.inviteRect(y)) && !this.invites.contains(f.id())) {
               Platform.game().playClick();
               this.inviter(f);
               return true;
            }

            y += ROW_H + FRIEND_GAP;
         }
      }

      if (Kit.dedans(mx, my, this.stopRect())) {
         Platform.game().playClick();
         this.arreter();
         return true;
      }

      if (Kit.dedans(mx, my, this.doneRect())) {
         Platform.game().playClick();
         this.back();
         return true;
      }

      return false;
   }

   @Override
   public boolean scroll(double mx, double my, double amount) {
      if (this.etat == Etat.HEBERGE) {
         this.defil.cran(amount, ROW_H + FRIEND_GAP);
         return true;
      }

      return false;
   }

   private void demarrer() {
      this.etat = Etat.DEMARRAGE;
      this.erreur = "";
      Platform.game().hostWorld(MODES[this.mode], DIFFS[this.diff], this.triche).whenComplete((a, err) -> {
         if (err != null) {
            Throwable cause = err.getCause() != null ? err.getCause() : err;
            this.erreur = Tr.of("swift.host.failed", cause.getMessage() != null ? cause.getMessage() : String.valueOf(cause));
            Platform.game().notify(Tr.of("swift.host.failed_toast"), cause.getMessage() != null ? cause.getMessage() : String.valueOf(cause));
            this.etat = Etat.REGLAGES;
         } else {
            this.adresse = a;
            this.etat = Etat.HEBERGE;
            this.chargerAmis();
         }
      });
   }

   private void arreter() {
      Platform.game().stopHosting();
      this.etat = Etat.REGLAGES;
      this.adresse = null;
      this.invites.clear();
      this.pose(Tr.of("swift.host.stopped"));
   }

   private void inviter(SocialApi.Friend f) {
      this.invites.add(f.id());
      SocialApi.sendDM(f, "swift-invite://" + this.adresse).thenAccept(res -> {
         if (res.ok()) {
            this.pose(Tr.of("swift.host.invite_sent", f.username()));
         } else {
            this.invites.remove(f.id());
            this.pose(Tr.of("swift.host.invite_failed", res.error()));
            Platform.game().notify(Tr.of("swift.host.invite_failed_toast"), res.error());
         }
      });
   }

   private enum Etat {
      REGLAGES,
      DEMARRAGE,
      HEBERGE
   }
}
