package dev.swiftclient.core.mods;

import dev.swiftclient.core.account.AccountEntry;
import dev.swiftclient.core.account.AccountManager;
import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.screen.AccountScreen;
import dev.swiftclient.core.screen.HostWorldScreen;
import dev.swiftclient.core.screen.HudEditorScreen;
import dev.swiftclient.core.screen.LanguageScreen;
import dev.swiftclient.core.screen.ProfilesScreen;
import dev.swiftclient.core.screen.ThemeScreen;
import dev.swiftclient.core.screen.WardrobeScreen;
import dev.swiftclient.core.ui.Defilement;
import dev.swiftclient.core.ui.ScreenRequest;
import dev.swiftclient.core.ui.UiScreen;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class ModsScreen extends UiScreen {
   private static final int OVERLAY = -1442840576; // deep wash, not grey fog
   private static final int PANEL_BG = -670035950; // unused — grey glass killed
   private static final int RAIL_INK = -251592680; // near-black blue ink 0xF00A0E18
   private static final int SIDE_BG = 352321535;
   private static final int LINE = 419430399;
   private static final int CARD_BG = -15263718;
   private static final int CARD_HOV = -14605786;
   private static final int ROW_BG = -15461097;
   private static final int ACCENT = -12877066; // #3B82F6
   private static final int ACCENT_DIM = 1715176182; // accent @ ~40%
   private static final int ON = -12868259;
   private static final int OFF_TRACK = -12960962;
   private static final int TEXT = -1;
   private static final int DIM = -6644317;
   private static final int FAINT = -10394518;
   private static final int NAV_HOVER = 352321535;
   private static final int NAV_ACTIVE = 587202559;
   private static final int GOLD = -12877066;
   private static final float PANEL_R = 0.0F; // no rounded grey LC slab
   private static final float CARD_R = 8.0F;
   private static final float PILL_R = 6.0F;
   private static final int S_MODS = 0;
   private static final int S_HUD = 1;
   private static final int S_THEMES = 2;
   private static final int S_COSMETICS = 3;
   private static final int S_ACCOUNT = 4;
   private static final int S_LANG = 5;
   private static final int S_PROFILES = 6;
   private static final int S_HOST = 7;
   private static final ModsScreen.Nav[] NAV = new ModsScreen.Nav[]{
      ModsScreen.Nav.header("swift.menu.section.mods"),
      new ModsScreen.Nav(0, "swift.menu.mods", "mods"),
      new ModsScreen.Nav(6, "swift.menu.profiles", "profiles"),
      new ModsScreen.Nav(1, "swift.menu.hud", "hud"),
      new ModsScreen.Nav(7, "swift.menu.host", "host"),
      ModsScreen.Nav.header("swift.menu.section.personalization"),
      new ModsScreen.Nav(2, "swift.menu.themes", "palette"),
      new ModsScreen.Nav(3, "swift.menu.cosmetics", "shirt"),
      new ModsScreen.Nav(5, "swift.menu.language", "globe"),
      ModsScreen.Nav.header("swift.menu.section.account"),
      new ModsScreen.Nav(4, "swift.menu.account", "users")
   };
   private final List<ModsScreen.NavHit> navHits = new ArrayList<>();
   private final Defilement grille = new Defilement();
   private Module settingsFor;
   private String settingsGroup;
   private ModuleSetting draggingSetting;
   private ModuleSetting listeningKey;
   private final Defilement reglages = new Defilement();
   private ModuleSetting pickerFor;
   private float pkH;
   private float pkS;
   private float pkV;
   private int pkA = 255;
   private int pkDrag = 0;
   private String category = null;
   private String query = "";
   private boolean searchFocused;
   private long caret;
   private int currentSection = 0;
   private final List<ModsScreen.TabHit> tabHits = new ArrayList<>();
   private final List<ModsScreen.SetRow> setRows = new ArrayList<>();
   private UiScreen embedded;
   private static final float RATIO = 2.0F;
   private static final int HEADER_H = 34;
   private static final int CHIPS_H = 26;
   private static final int MOD_ROW_H = 30;
   private static final int MOD_ROW_GAP = 2;
   private static final int GAP = 10;
   private static final int CARD_H = 64;
   private boolean titresVisibles = true;
   private static final int ROW_PAD = 9;
   private static final int ROW_GAP = 5;
   private static final int DESC_LH = 9;

   @Override
   public String title() {
      return "Mods";
   }

   private int embX() {
      return this.contentX();
   }

   private int embY() {
      return this.panelY();
   }

   private int embW() {
      return this.contentW();
   }

   private int embH() {
      return this.panelH();
   }

   private void setEmbedded(UiScreen s, int section) {
      this.embedded = s;
      this.currentSection = section;
      this.settingsFor = null;
      if (s != null) {
         s.bindHost(next -> this.setEmbedded(next, this.currentSection), this::embedBack);
         s.layout(this.embW(), this.embH());
      }
   }

   private void embedBack() {
      this.embedded = null;
      this.currentSection = 0;
   }

   @Override
   public void layout(int width, int height) {
      super.layout(width, height);
      if (this.embedded != null) {
         this.embedded.layout(this.embW(), this.embH());
      }
   }

   private int panelW() {
      return Math.max(420, this.width - 48);
   }

   private int panelH() {
      return Math.max(240, this.height - 40);
   }

   private int panelX() {
      return (this.width - this.panelW()) / 2;
   }

   private int panelY() {
      return (this.height - this.panelH()) / 2;
   }

   private int sideW() {
      return Math.max(168, Math.min(210, Math.round(this.panelW() * 0.24F)));
   }

   private int sideX() {
      return this.panelX();
   }

   private int contentX() {
      return this.panelX() + this.sideW();
   }

   private int contentW() {
      return this.panelW() - this.sideW();
   }

   private int gridTop() {
      return this.panelY() + 34 + 26;
   }

   private int gridBottom() {
      return this.panelY() + this.panelH() - 10;
   }

   private int gridLeft() {
      return this.contentX() + 14;
   }

   private int gridRight() {
      return this.panelX() + this.panelW() - 14;
   }

   private int gridW() {
      return this.gridRight() - this.gridLeft();
   }

   private int rowStride() {
      return MOD_ROW_H + MOD_ROW_GAP;
   }

   private int rowY(int i) {
      return this.gridTop() + i * this.rowStride() - this.grille.px();
   }

   private int[] rowRect(int i) {
      return new int[]{this.gridLeft(), this.rowY(i), this.gridW(), MOD_ROW_H};
   }

   private int[] closeRect() {
      return new int[]{this.panelX() + this.panelW() - 26, this.panelY() + 11, 15, 15};
   }

   private List<Module> filtered() {
      String q = this.query.trim().toLowerCase(Locale.ROOT);
      List<Module> out = new ArrayList<>();

      for (Module m : ModuleManager.modules()) {
         if (m.implemented() && (this.category == null || this.category.equals(m.category)) && (q.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(q))) {
            out.add(m);
         }
      }

      return out;
   }

   private List<String> categories() {
      List<String> out = new ArrayList<>();

      for (Module m : ModuleManager.modules()) {
         if (m.implemented() && !out.contains(m.category)) {
            out.add(m.category);
         }
      }

      return out;
   }

   private static List<String> groupsOf(Module m) {
      Set<String> seen = new LinkedHashSet<>();

      for (ModuleSetting s : m.settings()) {
         seen.add(s.group());
      }

      return new ArrayList<>(seen);
   }

   private List<ModuleSetting> visibleSettings() {
      List<ModuleSetting> out = new ArrayList<>();

      for (ModuleSetting s : this.settingsFor.settings()) {
         if (this.settingsGroup == null || this.settingsGroup.equals(s.group())) {
            out.add(s);
         }
      }

      return out;
   }

   private void recomputeScroll() {
      int n = this.filtered().size();
      int content = Math.max(0, n * this.rowStride() - MOD_ROW_GAP);
      this.grille.contenu(content, this.gridBottom() - this.gridTop());
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      this.caret++;
      this.recomputeScroll();
      this.grille.anime();
      this.reglages.anime();
      // Dim world — no grey fog overlay
      c.fill(0, 0, this.width, this.height, OVERLAY);
      int px = this.panelX();
      int py = this.panelY();
      int pw = this.panelW();
      int ph = this.panelH();
      // Swift shell: ink rail only — content floats, no LC grey glass slab
      c.fill(px, py, px + this.sideW(), py + ph, RAIL_INK);
      c.fill(px, py, px + 2, py + ph, ACCENT);
      // hairline between rail and content
      c.fill(this.contentX(), py + 12, this.contentX() + 1, py + ph - 12, ACCENT_DIM);

      this.drawSidebar(c, mouseX, mouseY);
      int[] xr = this.closeRect();
      boolean xh = in(mouseX, mouseY, xr);
      if (this.embedded != null) {
         c.pushScissor(this.embX(), this.embY(), this.embW(), this.embH());
         c.pushTranslate(this.embX(), this.embY());
         this.embedded.draw(c, mouseX - this.embX(), mouseY - this.embY(), delta);
         c.popTranslate();
         c.popScissor();
         this.drawClose(c, xr, xh);
      } else {
         this.drawHeader(c, mouseX, mouseY);
         this.drawClose(c, xr, xh);
         if (this.settingsFor != null) {
            this.drawSettings(c, mouseX, mouseY);
         } else {
            this.drawCategoryTabs(c, mouseX, mouseY);
            c.pushScissor(this.contentX() + 4, this.gridTop(), this.contentW() - 8, this.gridBottom() - this.gridTop());
            List<Module> mods = this.filtered();

            for (int i = 0; i < mods.size(); i++) {
               int[] r = this.rowRect(i);
               if (r[1] + r[3] >= this.gridTop() && r[1] <= this.gridBottom()) {
                  this.drawModRow(c, mods.get(i), r[0], r[1], r[2], mouseX, mouseY);
               }
            }

            c.popScissor();
            if (mods.isEmpty()) {
               c.centeredText("No module", this.contentX() + this.contentW() / 2, this.gridTop() + 40, FAINT, false);
            }

            this.drawScrollbar(c, this.grille, this.gridTop(), this.gridBottom());
         }
      }
   }

   private void drawClose(Canvas c, int[] xr, boolean hovered) {
      c.text("✕", xr[0], xr[1] + 2, hovered ? ACCENT : DIM, false);
   }

   private void drawScrollbar(Canvas c, Defilement d, int top, int bottom) {
      if (d.maxPx() > 0) {
         int vh = bottom - top;
         int barH = Math.max(24, vh * vh / (vh + d.maxPx()));
         int barY = top + (vh - barH) * d.px() / d.maxPx();
         c.fill(this.panelX() + this.panelW() - 4, barY, this.panelX() + this.panelW() - 2, barY + barH, ACCENT_DIM);
      }
   }

   private int[] backRect() {
      return new int[]{this.contentX() + 14, this.panelY() + 12, 12, 14};
   }

   private boolean canGoBack() {
      return this.settingsFor != null;
   }

   private void drawHeader(Canvas c, int mouseX, int mouseY) {
      int tx = this.contentX() + 18;
      if (this.canGoBack()) {
         int[] b = this.backRect();
         boolean over = in(mouseX, mouseY, b);
         c.text("‹", b[0], b[1] + 1, over ? ACCENT : DIM, false);
         tx = b[0] + b[2] + 6;
      }

      String titre = this.settingsFor != null ? this.settingsFor.name : "Mods";
      c.text(titre, tx, this.panelY() + 14, TEXT, false);
      c.fill(tx, this.panelY() + 28, tx + Math.min(72, c.textWidth(titre)), this.panelY() + 29, ACCENT);
      this.drawSearch(c, mouseX, mouseY);
   }

   private int searchW() {
      return Math.max(110, Math.min(200, this.contentW() / 3));
   }

   private int searchX() {
      return this.closeRect()[0] - 14 - this.searchW();
   }

   private int[] searchRect() {
      return new int[]{this.searchX(), this.panelY() + 10, this.searchW(), 18};
   }

   private void drawSearch(Canvas c, int mouseX, int mouseY) {
      int[] r = this.searchRect();
      boolean over = in(mouseX, mouseY, r);
      // Underline field — no grey LC search pill
      int line = this.searchFocused ? ACCENT : (over ? ACCENT_DIM : FAINT);
      c.fill(r[0], r[1] + r[3] - 1, r[0] + r[2], r[1] + r[3], line);
      int tx = r[0] + 2;
      int maxW = r[2] - 4;
      String shown = this.query.isEmpty() && !this.searchFocused ? "Search…" : this.query;
      int col = this.query.isEmpty() && !this.searchFocused ? FAINT : TEXT;
      c.text(trunc(c, shown, maxW), tx, r[1] + (r[3] - c.lineHeight()) / 2, col, false);
      if (this.searchFocused && this.caret / 20L % 2L == 0L) {
         c.text("_", tx + Math.min(c.textWidth(this.query), maxW), r[1] + (r[3] - c.lineHeight()) / 2, TEXT, false);
      }
   }

   private int accountCardY() {
      return this.panelY() + this.panelH() - 42;
   }

   private void buildNav() {
      this.navHits.clear();
      int y = this.panelY() + 56;
      int dispo = this.accountCardY() - 8 - y;
      int items = 0;
      int headers = 0;

      for (ModsScreen.Nav n : NAV) {
         if (n.isHeader()) {
            headers++;
         } else {
            items++;
         }
      }

      int itemH = 21;
      int headH = 15;
      int besoin = items * itemH + headers * headH;
      if (besoin > dispo && besoin > 0) {
         float k = (float)dispo / besoin;
         itemH = Math.max(15, Math.round(itemH * k));
         headH = Math.max(10, Math.round(headH * k));
         besoin = items * itemH + headers * headH;
      }

      this.titresVisibles = besoin <= dispo;
      if (!this.titresVisibles) {
         itemH = Math.max(13, Math.min(21, dispo / Math.max(1, items)));
      }

      for (ModsScreen.Nav nx : NAV) {
         if (!nx.isHeader() || this.titresVisibles) {
            int h = nx.isHeader() ? headH : itemH;
            this.navHits.add(new ModsScreen.NavHit(nx, y, h));
            y += h;
         }
      }
   }

   private void drawSidebar(Canvas c, int mouseX, int mouseY) {
      int x = this.sideX();
      int y = this.panelY();
      int w = this.sideW();
      // Wordmark — title-rail DA (no grey Cadre wash)
      c.logo(x + 18, y + 18, 22);
      try {
         String swift = Platform.game().translate("swift.brand.swift");
         String client = Platform.game().translate("swift.brand.client");
         c.text(swift, x + 46, y + 22, ACCENT, false);
         c.text(client, x + 46 + c.textWidth(swift) + 5, y + 22, TEXT, false);
      } catch (Throwable t) {
         c.text("SWIFT", x + 46, y + 22, ACCENT, false);
      }

      this.buildNav();

      for (ModsScreen.NavHit hit : this.navHits) {
         ModsScreen.Nav n = hit.nav();
         String label;
         try {
            label = Platform.game().translate(n.label());
         } catch (Throwable t) {
            label = n.label();
         }

         if (n.isHeader()) {
            c.text(label.toUpperCase(Locale.ROOT), x + 18, hit.y() + hit.h() - 10, FAINT, false);
         } else {
            boolean actif = n.section() == this.currentSection;
            boolean over = mouseX >= x + 10 && mouseX < x + w - 8 && mouseY >= hit.y() && mouseY < hit.y() + hit.h() - 2;
            int ih = hit.h() - 2;
            if (actif) {
               c.fill(x + 10, hit.y() + 4, x + 12, hit.y() + ih - 4, ACCENT);
            } else if (over) {
               c.fill(x + 10, hit.y() + 4, x + 12, hit.y() + ih - 4, ACCENT_DIM);
            }

            int col = actif || over ? TEXT : DIM;
            c.text(trunc(c, label, w - 40), x + 22, hit.y() + (ih - 8) / 2, col, false);
            if (over || actif) {
               c.text("›", x + w - 22, hit.y() + (ih - 8) / 2, ACCENT, false);
            }
         }
      }

      this.drawAccountCard(c, x, w, mouseX, mouseY);
   }

   private void drawAccountCard(Canvas c, int x, int w, int mouseX, int mouseY) {
      int cy = this.accountCardY();
      int chH = 32;
      // Text-only footer — no grey LC account pill
      c.fill(x + 18, cy - 10, x + w - 14, cy - 9, ACCENT_DIM);
      Optional<AccountEntry> acc = AccountManager.get().getActive();
      String uname = acc.<String>map(a -> a.getUsername()).orElseGet(() -> {
         try {
            return Platform.game().translate("swift.not_signed_in");
         } catch (Throwable t) {
            return "Not signed in";
         }
      });
      String uuid = acc.<String>map(a -> a.getUuid().toString()).orElse("");
      boolean over = mouseX >= x + 10 && mouseX < x + w - 8 && mouseY >= cy && mouseY < cy + chH;
      int hs = 18;
      int hx = x + 18;
      int hy = cy + (chH - hs) / 2;
      if (!uuid.isEmpty()) {
         c.playerHead(uuid, hx, hy, hs);
      } else {
         c.fill(hx, hy, hx + hs, hy + hs, ACCENT_DIM);
      }

      int tx = hx + hs + 8;
      int tw = x + w - 14 - tx;
      c.text(trunc(c, uname, tw), tx, cy + 7, over ? TEXT : DIM, false);
      c.text(trunc(c, loaderLine(), tw), tx, cy + 18, FAINT, false);
   }

   private static String loaderLine() {
      String v = "";

      try {
         v = Platform.game().gameVersion();
      } catch (Throwable var2) {
      }

      return v != null && !v.isBlank() ? "Fabric " + v : "Swift Client";
   }

   private int chipsY() {
      return this.panelY() + 34 + 3;
   }

   private void drawCategoryTabs(Canvas c, int mouseX, int mouseY) {
      this.tabHits.clear();
      int x = this.gridLeft();
      int y = this.chipsY();
      x = this.chip(c, "All", null, x, y, mouseX, mouseY, this.category == null);

      for (String cat : this.categories()) {
         x = this.chip(c, cat, cat, x, y, mouseX, mouseY, cat.equals(this.category));
      }
   }

   private int chip(Canvas c, String label, String value, int x, int y, int mouseX, int mouseY, boolean active) {
      int w = c.textWidth(label) + 4;
      int h = 18;
      this.tabHits.add(new ModsScreen.TabHit(x, w + 8, value));
      boolean over = in(mouseX, mouseY, new int[]{x, y, w + 8, h});
      int col = active ? ACCENT : (over ? -1 : -6644317);
      c.text(label, x + 2, y + (h - 8) / 2, col, false);
      if (active) {
         c.fill(x + 2, y + h - 2, x + 2 + w, y + h - 1, ACCENT);
      }
      return x + w + 14;
   }

   private static boolean lockedM(Module m) {
      return false;
   }

   /** Swift DA: compact text row. */
   private void drawModRow(Canvas c, Module m, int x, int y, int w, int mouseX, int mouseY) {
      boolean on = m.isEnabled();
      boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + MOD_ROW_H
         && mouseY >= this.gridTop() && mouseY <= this.gridBottom();
      if (hover) {
         c.fill(x, y + MOD_ROW_H - 1, x + w, y + MOD_ROW_H, ACCENT_DIM);
      }
      if (on) {
         c.fill(x, y + 6, x + 2, y + MOD_ROW_H - 6, ACCENT);
      } else if (hover) {
         c.fill(x, y + 6, x + 2, y + MOD_ROW_H - 6, 872415231);
      }

      int tx = x + 12;
      int nameCol = on || hover ? -1 : -6644317;
      String name = trunc(c, m.name, w - 80);
      c.text(name, tx, y + 7, nameCol, false);

      String etat = on ? "ON" : "OFF";
      int etatCol = on ? ACCENT : -10394518;
      int etatW = c.textWidth(etat);
      int gearW = m.hasSettings() ? 28 : 0;
      c.text(etat, x + w - gearW - etatW - 8, y + 7, etatCol, false);
      if (m.hasSettings()) {
         boolean gh = mouseX >= x + w - 24 && mouseX < x + w && mouseY >= y && mouseY < y + MOD_ROW_H;
         c.text("›", x + w - 16, y + 7, gh ? ACCENT : -10394518, false);
      }
   }

   private int rowH(List<String> desc) {
      return 26 + (desc.isEmpty() ? 0 : 2 + desc.size() * 9);
   }

   private int ctrlW(ModuleSetting s) {
      return switch (s.type) {
         case TOGGLE -> 44;
         case COLOR -> 92;
         case ACTION, CYCLE, KEY -> 100;
         default -> 150;
      };
   }

   private int[] setSwitchRect(ModsScreen.SetRow r) {
      return new int[]{this.gridRight() - 42, this.mid(r, 15), 30, 15};
   }

   private int[] setSliderTrack(ModsScreen.SetRow r) {
      return new int[]{this.gridRight() - 118, this.mid(r, 6), 96, 6};
   }

   private int[] setSwatchRect(ModsScreen.SetRow r) {
      return new int[]{this.gridRight() - 44, this.mid(r, 13), 34, 13};
   }

   private int[] setActionRect(ModsScreen.SetRow r) {
      return new int[]{this.gridRight() - 96, this.mid(r, 15), 88, 15};
   }

   private int mid(ModsScreen.SetRow r, int h) {
      return r.y() + (r.h() - 5 - h) / 2;
   }

   private void buildRows(Canvas c) {
      this.setRows.clear();
      List<ModuleSetting> settings = this.visibleSettings();
      int y = this.settingsTop();

      for (ModuleSetting s : settings) {
         int dispo = this.gridW() - 24 - this.ctrlW(s);
         List<String> desc = wrap(c, s.description(), dispo, 2);
         int h = this.rowH(desc) + 5;
         this.setRows.add(new ModsScreen.SetRow(s, y, h, desc));
         y += h;
      }

      this.reglages.contenu(Math.max(0, y - this.settingsTop() - 5), this.gridBottom() - this.settingsTop());
      int off = this.reglages.px();
      if (off != 0) {
         for (int i = 0; i < this.setRows.size(); i++) {
            ModsScreen.SetRow r = this.setRows.get(i);
            this.setRows.set(i, new ModsScreen.SetRow(r.s(), r.y() - off, r.h(), r.desc()));
         }
      }
   }

   private int settingsTop() {
      return groupsOf(this.settingsFor).size() > 1 ? this.gridTop() : this.panelY() + 34 + 4;
   }

   private void drawSettings(Canvas c, int mouseX, int mouseY) {
      if (this.draggingSetting != null) {
         this.updateDrag(mouseX);
      }

      List<String> groups = groupsOf(this.settingsFor);
      this.tabHits.clear();
      if (groups.size() > 1) {
         int x = this.gridLeft();
         int y = this.chipsY();

         for (String g : groups) {
            x = this.chip(c, g, g, x, y, mouseX, mouseY, g.equals(this.settingsGroup));
         }
      }

      this.buildRows(c);
      int sx = this.gridLeft();
      int sw = this.gridW();
      int top = this.settingsTop();
      int bottom = this.gridBottom();
      c.pushScissor(this.contentX() + 4, top, this.contentW() - 8, bottom - top);

      for (ModsScreen.SetRow r : this.setRows) {
         if (r.y() + r.h() >= top && r.y() <= bottom) {
            ModuleSetting s = r.s();
            int rh = r.h() - 5;
            c.card(sx, r.y(), sw, rh, -15461097, 520093695, 1, 8.0F);
            c.text(trunc(c, s.name, sw - 24 - this.ctrlW(s)), sx + 12, r.y() + 9, -1446929, false);

            for (int i = 0; i < r.desc().size(); i++) {
               c.text(r.desc().get(i), sx + 12, r.y() + 9 + 10 + i * 9, -10394518, false);
            }

            this.drawControl(c, r, mouseX, mouseY);
         }
      }

      c.popScissor();
      if (this.setRows.isEmpty()) {
         c.centeredText("No setting", this.contentX() + this.contentW() / 2, top + 40, -10394518, false);
      }

      this.drawScrollbar(c, this.reglages, top, bottom);
      if (this.pickerFor != null) {
         this.drawColorPicker(c, mouseX, mouseY);
      }
   }

   private void drawControl(Canvas c, ModsScreen.SetRow r, int mouseX, int mouseY) {
      ModuleSetting s = r.s();
      switch (s.type) {
         case TOGGLE:
            int[] t = this.setSwitchRect(r);
            this.drawSwitch(c, t[0], t[1], t[2], t[3], s.boolValue());
            break;
         case COLOR:
            int[] sw = this.setSwatchRect(r);
            c.card(sw[0], sw[1], sw[2], sw[3], -12960962, 872415231, 1, 3.0F);
            c.roundRect(sw[0] + 1, sw[1] + 1, sw[2] - 2, sw[3] - 2, 2.5F, s.colorValue());
            String hex = hex6(s.colorValue());
            c.text(hex, sw[0] - 8 - c.textWidth(hex), sw[1] + (sw[3] - 8) / 2, -6644317, false);
            break;
         case ACTION: {
            int[] b = this.setActionRect(r);
            boolean over = in(mouseX, mouseY, b);
            c.card(b[0], b[1], b[2], b[3], over ? -13882063 : -14671580, 872415231, 1, 5.0F);
            c.centeredText(trunc(c, s.actionLabel(), b[2] - 10), b[0] + b[2] / 2, b[1] + (b[3] - 8) / 2, -1, false);
            break;
         }
         case CYCLE: {
            int[] b = this.setActionRect(r);
            boolean over = in(mouseX, mouseY, b);
            c.card(b[0], b[1], b[2], b[3], over ? -13882063 : -14671580, 872415231, 1, 5.0F);
            c.centeredText("‹ " + trunc(c, s.cycleLabel(), b[2] - 26) + " ›", b[0] + b[2] / 2, b[1] + (b[3] - 8) / 2, -1, false);
            break;
         }
         case KEY: {
            int[] b = this.setActionRect(r);
            boolean listening = this.listeningKey == s;
            boolean over = in(mouseX, mouseY, b);
            c.card(b[0], b[1], b[2], b[3], listening ? -12868259 : (over ? -13882063 : -14671580), 872415231, 1, 5.0F);
            String lbl = listening ? "Press a key..." : s.keyName();
            c.centeredText(trunc(c, lbl, b[2] - 8), b[0] + b[2] / 2, b[1] + (b[3] - 8) / 2, -1, false);
            break;
         }
         default:
            int[] tr = this.setSliderTrack(r);
            c.roundRect(tr[0], tr[1], tr[2], tr[3], 3.0F, -12960962);
            int fw = (int)(tr[2] * s.fraction());
            c.roundRect(tr[0], tr[1], Math.max(fw, 1), tr[3], 3.0F, -12877066);
            c.roundRect(tr[0] + fw - 4, tr[1] - 3, 8, tr[3] + 6, 4.0F, -1);
            String v = fmt(s.value()) + s.unit;
            c.text(v, tr[0] - 8 - c.textWidth(v), tr[1] + (tr[3] - 8) / 2 - 1, -6644317, false);
      }
   }

   private void drawSwitch(Canvas c, int x, int y, int w, int h, boolean on) {
      c.roundRect(x, y, w, h, h / 2.0F, on ? -12868259 : -12960962);
      int kd = h - 4;
      int kx = on ? x + w - kd - 2 : x + 2;
      c.roundRect(kx, y + 2, kd, kd, kd / 2.0F, -1);
   }

   private static List<String> wrap(Canvas c, String s, int maxW, int maxLignes) {
      List<String> out = new ArrayList<>();
      if (s != null && !s.isBlank() && maxW >= 30) {
         StringBuilder ligne = new StringBuilder();

         for (String mot : s.split(" ")) {
            String essai = ligne.length() == 0 ? mot : ligne + " " + mot;
            if (c.textWidth(essai) <= maxW) {
               ligne.setLength(0);
               ligne.append(essai);
            } else {
               if (ligne.length() > 0) {
                  out.add(ligne.toString());
               }

               ligne.setLength(0);
               ligne.append(mot);
               if (out.size() == maxLignes) {
                  break;
               }
            }
         }

         if (out.size() < maxLignes && ligne.length() > 0) {
            out.add(ligne.toString());
         }

         if (out.size() > maxLignes) {
            out = new ArrayList<>(out.subList(0, maxLignes));
         }

         if (!out.isEmpty()) {
            int i = out.size() - 1;
            out.set(i, trunc(c, out.get(i), maxW));
         }

         return out;
      } else {
         return out;
      }
   }

   private int[] pickerRect() {
      int pw = 188;
      int ph = 150;
      return new int[]{this.contentX() + (this.contentW() - pw) / 2, this.panelY() + (this.panelH() - ph) / 2, pw, ph};
   }

   private int[] svRect() {
      int[] p = this.pickerRect();
      return new int[]{p[0] + 12, p[1] + 28, p[2] - 46, 84};
   }

   private int[] hueRect() {
      int[] sv = this.svRect();
      return new int[]{sv[0] + sv[2] + 8, sv[1], 12, sv[3]};
   }

   private int[] alphaRect() {
      int[] sv = this.svRect();
      return new int[]{sv[0], sv[1] + sv[3] + 9, sv[2] + 20, 8};
   }

   private void drawColorPicker(Canvas c, int mouseX, int mouseY) {
      if (this.pkDrag != 0) {
         this.updatePicker(mouseX, mouseY);
      }

      int[] p = this.pickerRect();
      c.fill(0, 0, this.width, this.height, 1711276032);
      c.card(p[0], p[1], p[2], p[3], -15461354, 872415231, 1, 8.0F);
      c.text(this.pickerFor.name, p[0] + 12, p[1] + 9, -1, false);
      c.text("Esc", p[0] + p[2] - 8 - c.textWidth("Esc"), p[1] + 9, -10394518, false);
      int hue = hsv(this.pkH, 1.0F, 1.0F, 255);
      int[] sv = this.svRect();

      for (int i = 0; i < sv[2]; i++) {
         int top = lerpRgb(-1, hue, (float)i / sv[2]);
         c.gradientV(sv[0] + i, sv[1], 1, sv[3], top, -16777216);
      }

      int cxp = sv[0] + Math.round(this.pkS * sv[2]);
      int cyp = sv[1] + Math.round((1.0F - this.pkV) * sv[3]);
      c.card(cxp - 4, cyp - 4, 8, 8, 0, -872415232, 1, 4.0F);
      c.card(cxp - 3, cyp - 3, 6, 6, 0, -1, 1, 3.0F);
      int[] hr = this.hueRect();
      int[] wheel = new int[]{-65536, -256, -16711936, -16711681, -16776961, -65281, -65536};
      int seg = hr[3] / 6;

      for (int k = 0; k < 6; k++) {
         c.gradientV(hr[0], hr[1] + k * seg, hr[2], k == 5 ? hr[3] - 5 * seg : seg, wheel[k], wheel[k + 1]);
      }

      int hy = hr[1] + Math.round(this.pkH * hr[3]);
      c.roundRect(hr[0] - 2, hy - 1, hr[2] + 4, 2, 1.0F, -1);
      int[] ar = this.alphaRect();
      int rgb = hsv(this.pkH, this.pkS, this.pkV, 255) & 16777215;
      c.roundRect(ar[0], ar[1], ar[2], ar[3], ar[3] / 2.0F, -9802898);

      for (int i = 0; i < ar[2]; i++) {
         int a = Math.round((float)i / ar[2] * 255.0F);
         c.fill(ar[0] + i, ar[1], ar[0] + i + 1, ar[1] + ar[3], a << 24 | rgb);
      }

      int ax = ar[0] + Math.round(this.pkA / 255.0F * ar[2]);
      c.roundRect(ax - 1, ar[1] - 2, 2, ar[3] + 4, 1.0F, -1);
      int argb = hsv(this.pkH, this.pkS, this.pkV, this.pkA);
      int pyv = ar[1] + ar[3] + 10;
      c.card(ar[0], pyv, 34, 14, -12960962, 872415231, 1, 3.0F);
      c.roundRect(ar[0] + 1, pyv + 1, 32, 12, 2.5F, argb);
      c.text("#" + String.format(Locale.ROOT, "%08X", argb), ar[0] + 42, pyv + 3, -6644317, false);
   }

   @Override
   public boolean click(double mx, double my, int button) {
      if (button != 0) {
         return false;
      } else if (this.pickerFor != null) {
         this.handlePickerClick((int)mx, (int)my);
         return true;
      } else if (in((int)mx, (int)my, this.closeRect())) {
         this.back();
         return true;
      } else {
         int px = this.panelX();
         int py = this.panelY();
         int pw = this.panelW();
         int ph = this.panelH();
         if (!(mx < px) && !(mx > px + pw) && !(my < py) && !(my > py + ph)) {
            int sx = this.sideX();
            int sw = this.sideW();
            boolean dansSide = mx >= sx + 8 && mx < sx + sw - 8;
            if (dansSide) {
               for (ModsScreen.NavHit hit : this.navHits) {
                  if (!hit.nav().isHeader() && my >= hit.y() && my < hit.y() + hit.h() - 2) {
                     this.onNav(hit.nav().section());
                     return true;
                  }
               }

               int cy = this.accountCardY();
               if (my >= cy && my < cy + 32) {
                  this.onNav(4);
                  return true;
               }
            }

            if (this.embedded != null) {
               if (mx >= this.embX() && mx < this.embX() + this.embW() && my >= this.embY() && my < this.embY() + this.embH()) {
                  this.embedded.click(mx - this.embX(), my - this.embY(), button);
               }

               return true;
            } else if (this.canGoBack() && in((int)mx, (int)my, this.backRect())) {
               this.closeSettings();
               return true;
            } else {
               boolean onSearch = in((int)mx, (int)my, this.searchRect());
               if (onSearch != this.searchFocused) {
                  this.searchFocused = onSearch;
               }

               if (onSearch) {
                  return true;
               } else if (this.settingsFor != null) {
                  if (my >= this.chipsY() && my < this.chipsY() + 18) {
                     for (ModsScreen.TabHit t : this.tabHits) {
                        if (mx >= t.x() && mx < t.x() + t.w()) {
                           this.settingsGroup = t.value();
                           this.reglages.haut();
                           return true;
                        }
                     }
                  }

                  for (ModsScreen.SetRow r : this.setRows) {
                     if (!(my < r.y()) && !(my >= r.y() + r.h())) {
                        ModuleSetting s = r.s();
                        switch (s.type) {
                           case TOGGLE:
                              int[] tx = this.setSwitchRect(r);
                              if (near(mx, my, tx, 6)) {
                                 s.toggle();
                                 return true;
                              }
                              break;
                           case COLOR:
                              int[] sr = this.setSwatchRect(r);
                              if (near(mx, my, sr, 4)) {
                                 this.openPicker(s);
                                 return true;
                              }
                              break;
                           case ACTION:
                              if (in((int)mx, (int)my, this.setActionRect(r))) {
                                 s.run();
                                 UiScreen req = ScreenRequest.consume();
                                 if (req != null) {
                                    this.open(req);
                                 }

                                 return true;
                              }
                              break;
                           case CYCLE:
                              if (in((int)mx, (int)my, this.setActionRect(r))) {
                                 s.cycleNext();
                                 return true;
                              }
                              break;
                           case KEY:
                              if (in((int)mx, (int)my, this.setActionRect(r))) {
                                 this.listeningKey = s;
                                 return true;
                              }
                              break;
                           default:
                              int[] tr = this.setSliderTrack(r);
                              if (near(mx, my, tr, 6)) {
                                 this.draggingSetting = s;
                                 this.updateDrag((int)mx);
                                 return true;
                              }
                        }
                     }
                  }

                  return true;
               } else {
                  if (my >= this.chipsY() && my < this.chipsY() + 18) {
                     for (ModsScreen.TabHit txx : this.tabHits) {
                        if (mx >= txx.x() && mx < txx.x() + txx.w()) {
                           this.category = txx.value();
                           this.grille.haut();
                           return true;
                        }
                     }
                  }

                  if (!(my < this.gridTop()) && !(my > this.gridBottom())) {
                     List<Module> mods = this.filtered();

                     for (int i = 0; i < mods.size(); i++) {
                        int[] r = this.rowRect(i);
                        if (!(mx < r[0]) && !(mx >= r[0] + r[2]) && !(my < r[1]) && !(my >= r[1] + r[3])) {
                           Module m = mods.get(i);
                           if (m.hasSettings() && mx >= r[0] + r[2] - 28) {
                              this.openSettings(m);
                              return true;
                           }

                           if (!lockedM(m)) {
                              m.toggle();
                           }

                           return true;
                        }
                     }

                     return true;
                  } else {
                     return true;
                  }
               }
            }
         } else {
            this.back();
            return true;
         }
      }
   }

   private void openSettings(Module m) {
      this.settingsFor = m;
      List<String> g = groupsOf(m);
      this.settingsGroup = g.size() > 1 ? g.get(0) : null;
      this.reglages.haut();
   }

   private void closeSettings() {
      this.settingsFor = null;
      this.settingsGroup = null;
      this.draggingSetting = null;
      this.listeningKey = null;
      this.reglages.haut();
   }

   private void onNav(int id) {
      if (id == 0) {
         this.setEmbedded(null, 0);
      } else if (id == 1) {
         this.open(new HudEditorScreen());
      } else if (id == 2) {
         this.setEmbedded(new ThemeScreen(), 2);
      } else if (id == 3) {
         this.setEmbedded(new WardrobeScreen(), 3);
      } else if (id == 5) {
         this.setEmbedded(new LanguageScreen(), 5);
      } else if (id == 7) {
         this.setEmbedded(new HostWorldScreen(), 7);
      } else if (id == 6) {
         this.setEmbedded(new ProfilesScreen(), 6);
      } else if (id == 4) {
         this.setEmbedded(new AccountScreen(), 4);
      }
   }

   @Override
   public boolean charTyped(String s) {
      if (this.embedded != null) {
         return this.embedded.charTyped(s);
      } else if (!this.searchFocused) {
         return false;
      } else {
         this.query = this.query + s;
         this.grille.haut();
         return true;
      }
   }

   @Override
   public boolean keyPressed(int keyCode) {
      if (this.embedded != null) {
         if (this.embedded.keyPressed(keyCode)) {
            return true;
         } else if (keyCode == 256) {
            this.embedBack();
            return true;
         } else {
            return false;
         }
      } else if (this.listeningKey != null) {
         if (keyCode != 256) {
            this.listeningKey.setKey(keyCode);
         }

         this.listeningKey = null;
         return true;
      } else if (keyCode == 256 && this.pickerFor != null) {
         this.pickerFor = null;
         this.pkDrag = 0;
         return true;
      } else if (keyCode == 256 && this.settingsFor != null) {
         this.closeSettings();
         return true;
      } else if (!this.searchFocused) {
         return false;
      } else if (keyCode == 259 && !this.query.isEmpty()) {
         this.query = this.query.substring(0, this.query.length() - 1);
         this.grille.haut();
         return true;
      } else if (keyCode == 256) {
         this.query = "";
         this.searchFocused = false;
         return true;
      } else if (keyCode == 257) {
         this.searchFocused = false;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean closeOnEscape() {
      return this.embedded == null && this.settingsFor == null && this.pickerFor == null && this.listeningKey == null && !this.searchFocused;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button) {
      if (this.pickerFor != null) {
         if (this.pkDrag != 0) {
            this.updatePicker((int)mouseX, (int)mouseY);
         }

         return true;
      } else if (this.embedded != null) {
         return this.embedded.mouseDragged(mouseX - this.embX(), mouseY - this.embY(), button);
      } else if (this.draggingSetting != null) {
         this.updateDrag((int)mouseX);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.pickerFor != null) {
         this.pkDrag = 0;
         return true;
      } else if (this.embedded != null) {
         return this.embedded.mouseReleased(mouseX - this.embX(), mouseY - this.embY(), button);
      } else if (this.draggingSetting != null) {
         this.draggingSetting = null;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean scroll(double mouseX, double mouseY, double amount) {
      if (this.embedded != null) {
         return this.embedded.scroll(mouseX - this.embX(), mouseY - this.embY(), amount);
      } else if (this.pickerFor != null) {
         return true;
      } else if (this.settingsFor != null) {
         this.reglages.cran(amount, 26.0);
         return true;
      } else {
         this.grille.cran(amount, 26.0);
         return true;
      }
   }

   private void updateDrag(int mx) {
      for (ModsScreen.SetRow r : this.setRows) {
         if (r.s() == this.draggingSetting) {
            int[] tr = this.setSliderTrack(r);
            this.draggingSetting.setFraction((double)(mx - tr[0]) / tr[2]);
            return;
         }
      }
   }

   private void openPicker(ModuleSetting s) {
      this.pickerFor = s;
      this.pkDrag = 0;
      this.loadHsv(s.colorValue());
   }

   private void handlePickerClick(int mx, int my) {
      int[] p = this.pickerRect();
      if (mx < p[0] || mx > p[0] + p[2] || my < p[1] || my > p[1] + p[3]) {
         this.pickerFor = null;
      } else if (in(mx, my, this.svRect())) {
         this.pkDrag = 1;
         this.updatePicker(mx, my);
      } else if (in(mx, my, this.hueRect())) {
         this.pkDrag = 2;
         this.updatePicker(mx, my);
      } else {
         int[] ar = this.alphaRect();
         if (mx >= ar[0] && mx <= ar[0] + ar[2] && my >= ar[1] - 4 && my <= ar[1] + ar[3] + 4) {
            this.pkDrag = 3;
            this.updatePicker(mx, my);
         }
      }
   }

   private void updatePicker(int mx, int my) {
      switch (this.pkDrag) {
         case 1:
            int[] sv = this.svRect();
            this.pkS = clamp01((float)(mx - sv[0]) / sv[2]);
            this.pkV = clamp01(1.0F - (float)(my - sv[1]) / sv[3]);
            break;
         case 2:
            int[] hr = this.hueRect();
            this.pkH = clamp01((float)(my - hr[1]) / hr[3]);
            break;
         case 3:
            int[] ar = this.alphaRect();
            this.pkA = Math.round(clamp01((float)(mx - ar[0]) / ar[2]) * 255.0F);
            break;
         default:
            return;
      }

      if (this.pickerFor != null) {
         this.pickerFor.setColor(hsv(this.pkH, this.pkS, this.pkV, this.pkA));
      }
   }

   private void loadHsv(int argb) {
      this.pkA = argb >>> 24 & 0xFF;
      float r = (argb >> 16 & 0xFF) / 255.0F;
      float g = (argb >> 8 & 0xFF) / 255.0F;
      float b = (argb & 0xFF) / 255.0F;
      float max = Math.max(r, Math.max(g, b));
      float min = Math.min(r, Math.min(g, b));
      float d = max - min;
      this.pkV = max;
      this.pkS = max == 0.0F ? 0.0F : d / max;
      float h = 0.0F;
      if (d != 0.0F) {
         if (max == r) {
            h = (g - b) / d % 6.0F;
         } else if (max == g) {
            h = (b - r) / d + 2.0F;
         } else {
            h = (r - g) / d + 4.0F;
         }

         h /= 6.0F;
         if (h < 0.0F) {
            h++;
         }
      }

      this.pkH = h;
   }

   private static int hsv(float h, float s, float v, int a) {
      float i = (float)Math.floor(h * 6.0F);
      float f = h * 6.0F - i;
      float p = v * (1.0F - s);
      float q = v * (1.0F - f * s);
      float t = v * (1.0F - (1.0F - f) * s);
      float r;
      float g;
      float b;
      switch ((int)i % 6) {
         case 0:
            r = v;
            g = t;
            b = p;
            break;
         case 1:
            r = q;
            g = v;
            b = p;
            break;
         case 2:
            r = p;
            g = v;
            b = t;
            break;
         case 3:
            r = p;
            g = q;
            b = v;
            break;
         case 4:
            r = t;
            g = p;
            b = v;
            break;
         default:
            r = v;
            g = p;
            b = q;
      }

      return a << 24 | (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
   }

   private static int lerpRgb(int a, int b, float t) {
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      return 0xFF000000 | (int)(ar + (br - ar) * t) << 16 | (int)(ag + (bg - ag) * t) << 8 | (int)(ab + (bb - ab) * t);
   }

   private static float clamp01(float f) {
      return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
   }

   private static String hex6(int argb) {
      return String.format(Locale.ROOT, "#%06X", argb & 16777215);
   }

   private static boolean in(int mx, int my, int[] r) {
      return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
   }

   private static boolean near(double mx, double my, int[] r, int marge) {
      return mx >= r[0] - marge && mx <= r[0] + r[2] + marge && my >= r[1] - marge && my <= r[1] + r[3] + marge;
   }

   private static String trunc(Canvas c, String s, int maxW) {
      if (c.textWidth(s) <= maxW) {
         return s;
      } else {
         while (s.length() > 1 && c.textWidth(s + "…") > maxW) {
            s = s.substring(0, s.length() - 1);
         }

         return s + "…";
      }
   }

   private static String fmt(double v) {
      return v == Math.floor(v) ? Integer.toString((int)v) : String.format(Locale.ROOT, "%.1f", v);
   }

   private record Nav(int section, String label, String icon) {
      static ModsScreen.Nav header(String label) {
         return new ModsScreen.Nav(-1, label, null);
      }

      boolean isHeader() {
         return this.section < 0;
      }
   }

   private record NavHit(ModsScreen.Nav nav, int y, int h) {
   }

   private record SetRow(ModuleSetting s, int y, int h, List<String> desc) {
   }

   private record TabHit(int x, int w, String value) {
   }
}
