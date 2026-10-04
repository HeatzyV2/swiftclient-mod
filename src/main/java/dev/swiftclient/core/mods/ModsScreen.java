package dev.swiftclient.core.mods;

import dev.swiftclient.core.account.AccountEntry;
import dev.swiftclient.core.account.AccountManager;
import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.screen.AccountScreen;
import dev.swiftclient.core.screen.HostWorldScreen;
import dev.swiftclient.core.screen.HudEditorScreen;
import dev.swiftclient.core.screen.LanguageScreen;
import dev.swiftclient.core.screen.ProfilesScreen;
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
   private static final float PANEL_R = 10.0F;
   // Slabs: dark rounded blocks with a hairline border (Swift take on the Lunar layout)
   private static final int SLAB = 0xF20E1116; // panel
   private static final int SLAB_BORDER = 0x1AFFFFFF;
   private static final int TILE = 0xFF171A21; // module card, a step above the panel
   private static final int TILE_HOV = 0xFF1C2029;
   private static final int BTN = 0xFF22262E; // options button
   private static final int BTN_HOV = 0xFF2B303A;
   private static final int OFF_BG = 0xFF1F2229; // disabled bar
   private static final int OFF_TEXT = 0xFF8A93A0;
   private static final int ACCENT_HOV = 0xFF5B9BFF;
   private static final int RAIL = 0xFF0B0D12;
   private static final int ACCENT_SOFT = 0x333B82F6; // accent @ 20%
   private static final float CARD_R = 8.0F;
   private static final float PILL_R = 6.0F;
   private static final int S_MODS = 0;
   private static final int S_HUD = 1;
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
      new ModsScreen.Nav(3, "swift.menu.cosmetics", "shirt"),
      new ModsScreen.Nav(5, "swift.menu.language", "globe"),
      ModsScreen.Nav.header("swift.menu.section.account"),
      new ModsScreen.Nav(8, "swift.menu.friends", "friends"),
      new ModsScreen.Nav(4, "swift.menu.account", "users")
   };
   private final List<ModsScreen.NavHit> navHits = new ArrayList<>();
   private final Defilement grille = new Defilement();
   private Module settingsFor;
   private String settingsGroup;
   private ModuleSetting draggingSetting;
   private ModuleSetting listeningKey;
   private ModuleSetting editingText;
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
   private static final int CARD_MIN_W = 76;
   private static final int TILE_H = 57;
   private static final int TILE_GAP = 6;
   private static final int GAP = 10;
   private static final int CARD_H = 64;
   private boolean titresVisibles = true;
   private static final int ROW_PAD = 9;
   private static final int ROW_GAP = 5;
   private static final int DESC_LH = 9;

   @Override
   public String title() {
      return Tr.of("swift.menu.mods");
   }

   private int embX() {
      return this.contentX();
   }

   private int embY() {
      return this.panelY() + TOP_H;
   }

   private int embW() {
      return this.contentW();
   }

   private int embH() {
      return this.panelH() - TOP_H - BAR_H;
   }

   private void setEmbedded(UiScreen s, int section) {
      this.contentChanged();
      this.embedded = s;
      this.currentSection = section;
      this.settingsFor = null;
      if (s != null) {
         s.bindHost(next -> this.setEmbedded(next, this.currentSection), this::embedBack);
         s.layout(this.embW(), this.embH());
      }
   }

   private void embedBack() {
      this.contentChanged();
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

   /** No sidebar any more: navigation is the hotbar along the bottom. */
   private int sideW() {
      return 0;
   }

   private static final int BAR_H = 48;
   private static final int TOP_H = 34;

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
      return this.panelY() + this.panelH() - 8 - BAR_H;
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

   private int columns() {
      return Math.max(1, Math.min(7, (this.gridW() + TILE_GAP) / (CARD_MIN_W + TILE_GAP)));
   }

   private int cardW() {
      int cols = this.columns();
      return (this.gridW() - (cols - 1) * TILE_GAP) / cols;
   }

   /** Card i: x, y, w, h (y already scrolled). */
   private int[] cardRect(int i) {
      int cols = this.columns();
      int w = this.cardW();
      int col = i % cols;
      int row = i / cols;
      return new int[]{this.gridLeft() + col * (w + TILE_GAP), this.gridTop() + row * (TILE_H + TILE_GAP) - this.grille.px(), w, TILE_H};
   }

   private static int[] gearRect(int[] card) {
      return new int[]{card[0] + card[2] - 5 - 14, card[1] + card[3] - 5 - 14, 14, 14};
   }

   private static int[] toggleRect(int[] card) {
      return new int[]{card[0] + 5, card[1] + card[3] - 5 - 14, card[2] - 10 - 16, 14};
   }

   private int[] closeRect() {
      return new int[]{this.panelX() + this.panelW() - 28, this.panelY() + 8, 18, 18};
   }

   private List<Module> filtered() {
      String q = this.query.trim().toLowerCase(Locale.ROOT);
      List<Module> out = new ArrayList<>();

      for (Module m : ModuleManager.modules()) {
         if ((this.category == null || this.category.equals(m.category)) && (q.isEmpty() || m.displayName().toLowerCase(Locale.ROOT).contains(q))) {
            out.add(m);
         }
      }

      return out;
   }

   private List<String> categories() {
      List<String> out = new ArrayList<>();

      for (Module m : ModuleManager.modules()) {
         if (!out.contains(m.category)) {
            out.add(m.category);
         }
      }

      // Fixed chip order; categories outside the list keep their place after it.
      out.sort((a, b) -> Integer.compare(rank(a), rank(b)));
      return out;
   }

   private static int rank(String category) {
      int i = ModuleManager.CATEGORIES.indexOf(category);
      return i < 0 ? ModuleManager.CATEGORIES.size() : i;
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
      int rows = (n + this.columns() - 1) / this.columns();
      int content = Math.max(0, rows * (TILE_H + TILE_GAP) - TILE_GAP);
      this.grille.contenu(content, this.gridBottom() - this.gridTop());
   }

   // --- Motion: everything eases toward its target at a fixed rate, whatever the frame rate ---

   private final java.util.Map<String, Float> anim = new java.util.HashMap<>();
   private long lastFrame;
   private float frameDt = 1.0F / 60.0F;
   private final long openedAt = System.nanoTime();
   private long contentChangedAt = System.nanoTime();
   private float chipHiX = -1.0F;
   private float chipHiW;
   private float navHiY = -1.0F;

   /** Moves the value `key` toward `target` (exponential ease, `rate` per second) and returns it. */
   private float ease(String key, float target, float rate) {
      Float v = this.anim.get(key);
      float cur = v == null ? target : v;
      cur += (target - cur) * (1.0F - (float)Math.exp(-rate * this.frameDt));
      if (Math.abs(target - cur) < 0.002F) {
         cur = target;
      }

      this.anim.put(key, cur);
      return cur;
   }

   private static float approach(float cur, float target, float rate, float dt) {
      if (cur < -0.5F) {
         return target;
      }

      float n = cur + (target - cur) * (1.0F - (float)Math.exp(-rate * dt));
      return Math.abs(target - n) < 0.3F ? target : n;
   }

   /** 0 -> 1 over `ms` since `since`, ease-out cubic. */
   private static float intro(long since, float ms) {
      float t = Math.min(1.0F, (System.nanoTime() - since) / 1.0E6F / ms);
      float u = 1.0F - t;
      return 1.0F - u * u * u;
   }

   private void contentChanged() {
      this.contentChangedAt = System.nanoTime();
   }

   private static int lerpArgb(int a, int b, float t) {
      t = t < 0.0F ? 0.0F : Math.min(t, 1.0F);
      int aa = a >>> 24, ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
      int ba = b >>> 24, br = b >> 16 & 0xFF, bg = b >> 8 & 0xFF, bb = b & 0xFF;
      return Math.round(aa + (ba - aa) * t) << 24 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
   }

   private static int withAlpha(int argb, float k) {
      return Math.round((argb >>> 24) * Math.max(0.0F, Math.min(1.0F, k))) << 24 | argb & 0xFFFFFF;
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      long now = System.nanoTime();
      this.frameDt = this.lastFrame == 0L ? 1.0F / 60.0F : Math.min(0.1F, (now - this.lastFrame) / 1.0E9F);
      this.lastFrame = now;
      this.caret++;
      this.recomputeScroll();
      this.grille.anime();
      this.reglages.anime();
      float open = intro(this.openedAt, 240.0F);
      c.fill(0, 0, this.width, this.height, withAlpha(OVERLAY, open));
      int px = this.panelX();
      int py = this.panelY();
      int pw = this.panelW();
      int ph = this.panelH();
      boolean scaled = open < 1.0F;
      if (scaled) {
         // Scale about the panel centre: pushScale moves the origin there, so move it back before drawing
         c.pushScale(px + pw / 2, py + ph / 2, 0.96F + 0.04F * open);
         c.pushTranslate(-(px + pw / 2), -(py + ph / 2));
      }

      // Swift shell: one dark slab, the rail is an inset slab on its left
      c.card(px, py, pw, ph, 0xF2090C12, 0xFF222B42, 1, 6.0F);
      this.drawTopBar(c);
      this.drawHotbar(c, mouseX, mouseY);
      int slide = Math.round((1.0F - intro(this.contentChangedAt, 200.0F)) * 6.0F);
      if (slide != 0) {
         c.pushTranslate(slide, 0);
      }

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
               int[] r = this.cardRect(i);
               if (r[1] + r[3] >= this.gridTop() && r[1] <= this.gridBottom()) {
                  this.drawModCard(c, mods.get(i), r, mouseX, mouseY);
               }
            }

            c.popScissor();
            if (mods.isEmpty()) {
               c.centeredText(Tr.of("swift.mods.none"), this.contentX() + this.contentW() / 2, this.gridTop() + 40, FAINT, false);
            }

            this.drawScrollbar(c, this.grille, this.gridTop(), this.gridBottom());
         }
      }

      if (slide != 0) {
         c.popTranslate();
      }

      if (scaled) {
         c.popTranslate();
         c.popScale();
      }
   }

   private void drawClose(Canvas c, int[] xr, boolean hovered) {
      dev.swiftclient.core.ui.Px.button(c, xr[0], xr[1], xr[2], xr[3], "X", hovered, false);
   }

   private void drawScrollbar(Canvas c, Defilement d, int top, int bottom) {
      if (d.maxPx() > 0) {
         int vh = bottom - top;
         int barH = Math.max(24, vh * vh / (vh + d.maxPx()));
         int barY = top + (vh - barH) * d.px() / d.maxPx();
         int x = this.panelX() + this.panelW() - 9;
         c.fill(x, top, x + 5, bottom, 0x66000000);
         c.fill(x, barY, x + 5, barY + barH, 0xFF000000);
         c.fill(x + 1, barY + 1, x + 4, barY + barH - 1, ACCENT);
      }
   }

   private int[] backRect() {
      return new int[]{this.panelX() + 168, this.panelY() + 8, 18, 18};
   }

   private boolean canGoBack() {
      return this.settingsFor != null;
   }

   private void drawHeader(Canvas c, int mouseX, int mouseY) {
      if (this.canGoBack()) {
         int[] b = this.backRect();
         boolean over = in(mouseX, mouseY, b);
         dev.swiftclient.core.ui.Px.button(c, b[0], b[1], b[2], b[3], "<", over, false);
         int tx = b[0] + b[2] + 8;
         c.text(trunc(c, this.settingsFor.displayName().toUpperCase(Locale.ROOT), Math.max(30, this.searchX() - tx - 8)), tx, this.panelY() + 13, TEXT, true);
      }
      this.drawSearch(c, mouseX, mouseY);
   }

   private int searchW() {
      return Math.max(110, Math.min(200, this.contentW() / 3));
   }

   private int searchX() {
      return this.closeRect()[0] - 10 - this.searchW();
   }

   private int[] searchRect() {
      return new int[]{this.searchX(), this.panelY() + 8, this.searchW(), 18};
   }

   private void drawSearch(Canvas c, int mouseX, int mouseY) {
      int[] r = this.searchRect();
      boolean over = in(mouseX, mouseY, r);
      c.card(r[0], r[1], r[2], r[3], 0xFF0C0F16, this.searchFocused ? ACCENT : (over ? 0xFF3A4766 : 0), 1, 2.0F);
      c.icon("zoom", r[0] + 5, r[1] + (r[3] - 9) / 2, 9, FAINT);
      int tx = r[0] + 18;
      int maxW = r[2] - 24;
      String shown = this.query.isEmpty() && !this.searchFocused ? Tr.of("swift.mods.search") : this.query;
      int col = this.query.isEmpty() && !this.searchFocused ? FAINT : TEXT;
      c.text(trunc(c, shown, maxW), tx, r[1] + (r[3] - c.lineHeight()) / 2 + 1, col, false);
      if (this.searchFocused && this.caret / 20L % 2L == 0L) {
         c.fill(tx + Math.min(c.textWidth(this.query), maxW) + 1, r[1] + 4, tx + Math.min(c.textWidth(this.query), maxW) + 3, r[1] + r[3] - 4, TEXT);
      }
   }

   private int accountCardY() {
      return this.panelY() + this.panelH() - BAR_H + 6;
   }

   private void buildNav() {
      this.navHits.clear();
      int y = this.panelY() + 56;
      int dispo = this.accountCardY() - 32 - y;
      int items = 0;
      int headers = 0;

      for (ModsScreen.Nav n : NAV) {
         if (n.isHeader()) {
            headers++;
         } else {
            items++;
         }
      }

      int itemH = 17;
      int headH = 14;
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

   private final List<int[]> hotHits = new ArrayList<>();
   private final long t0 = System.nanoTime();

   /** Top bar: Zip, the wordmark and its speed streak. The search, back and close controls sit on the same row. */
   private void drawTopBar(Canvas c) {
      int x = this.panelX() + 12;
      int y = this.panelY() + 7;
      long t = (System.nanoTime() - this.t0) / 1_000_000L;
      dev.swiftclient.core.ui.Px.zip(c, x, y - 1, 1, false, t);
      String swift = "SWIFT";
      String client = "CLIENT";
      try {
         swift = Platform.game().translate("swift.brand.swift").toUpperCase(Locale.ROOT);
         client = Platform.game().translate("swift.brand.client").toUpperCase(Locale.ROOT);
      } catch (Throwable ignored) {
      }
      c.text(swift, x + 24, y + 3, ACCENT, true);
      c.text(client, x + 24 + c.textWidth(swift) + 5, y + 3, TEXT, true);
      dev.swiftclient.core.ui.Px.streak(c, x + 24, y + 15);
      c.fill(this.panelX() + 4, this.panelY() + TOP_H, this.panelX() + this.panelW() - 4, this.panelY() + TOP_H + 2, 0xFF000000);
   }

   /** The hotbar: one slot per section, white frame on the selected one, number keys 1–8 jump to it. */
   private void drawHotbar(Canvas c, int mouseX, int mouseY) {
      this.hotHits.clear();
      int by = this.panelY() + this.panelH() - BAR_H;
      c.fill(this.panelX() + 4, by - 2, this.panelX() + this.panelW() - 4, by, 0xFF000000);
      int n = 0;
      for (ModsScreen.Nav nv : NAV) {
         if (!nv.isHeader()) {
            n++;
         }
      }
      int gap = 3;
      int left = this.panelX() + 12 + this.accountW() + 10;
      int right = this.editHudRect()[0] - 10;
      int zone = Math.max(60, right - left);
      int slot = Math.max(18, Math.min(30, (zone - (n - 1) * gap) / n));
      int total = n * slot + (n - 1) * gap;
      int x = left + Math.max(0, (zone - total) / 2);
      int y = by + (BAR_H - slot) / 2 + 1;
      int idx = 0;
      String hoverLabel = null;
      int hoverX = 0;
      for (ModsScreen.Nav nv : NAV) {
         if (nv.isHeader()) {
            continue;
         }
         idx++;
         boolean actif = nv.section() == this.currentSection;
         boolean over = mouseX >= x && mouseX < x + slot && mouseY >= y && mouseY < y + slot;
         float hov = this.ease("slot:" + nv.section(), over || actif ? 1.0F : 0.0F, 30.0F);
         int dy = actif ? -3 : (over ? -2 : 0);
         c.card(x, y + dy, slot, slot, actif ? 0xFF1D2535 : (over ? 0xFF182033 : 0xFF11151F), actif ? 0xFFFFFFFF : 0, actif ? 1 : 0, 3.0F);
         if (nv.icon() != null) {
            c.icon(nv.icon(), x + (slot - 14) / 2, y + dy + (slot - 14) / 2, 14, actif ? 0xFFFFFFFF : (over ? 0xFFE6ECF7 : 0xFFA9B4C8));
         }
         c.text(String.valueOf(idx), x + slot - 7, y + dy + slot - 10, actif ? TEXT : FAINT, true);
         this.hotHits.add(new int[]{x, y - 3, slot, slot + 3, nv.section()});
         if (over) {
            try {
               hoverLabel = Platform.game().translate(nv.label());
            } catch (Throwable t) {
               hoverLabel = nv.label();
            }
            hoverX = x + slot / 2;
         }
         x += slot + gap;
      }

      if (hoverLabel != null) {
         int tw = c.textWidth(hoverLabel) + 10;
         int tx = Math.max(this.panelX() + 6, Math.min(hoverX - tw / 2, this.panelX() + this.panelW() - tw - 6));
         c.card(tx, by - 24, tw, 14, 0xFF000000, 0xFF3A4766, 1, 2.0F);
         c.text(hoverLabel, tx + 5, by - 20, TEXT, false);
      }

      int[] hud = this.editHudRect();
      boolean hudOver = in(mouseX, mouseY, hud);
      dev.swiftclient.core.ui.Px.button(c, hud[0], hud[1], hud[2], hud[3], trunc(c, this.panelW() < 520 ? "EDIT HUD" : Tr.of("swift.menu.edit_hud").toUpperCase(Locale.ROOT), hud[2] - 8), hudOver, true);
      this.drawAccountCard(c, this.panelX(), this.panelW(), mouseX, mouseY);
   }

   private int editW() {
      return this.panelW() < 520 ? 80 : 118;
   }

   private int accountW() {
      return this.panelW() < 520 ? 70 : 140;
   }

   private int[] editHudRect() {
      return new int[]{this.panelX() + this.panelW() - 12 - this.editW(), this.panelY() + this.panelH() - BAR_H + 10, this.editW(), 22};
   }

   private void drawAccountCard(Canvas c, int x, int w, int mouseX, int mouseY) {
      int cy = this.accountCardY();
      Optional<AccountEntry> acc = AccountManager.get().getActive();
      String uname = acc.<String>map(a -> a.getUsername()).orElseGet(() -> {
         try {
            return Platform.game().translate("swift.not_signed_in");
         } catch (Throwable t) {
            return Tr.of("swift.mods.not_signed_in");
         }
      });
      String uuid = acc.<String>map(a -> a.getUuid().toString()).orElse("");
      int bx = x + 12;
      int bw = this.accountW();
      boolean over = mouseX >= bx && mouseX < bx + bw && mouseY >= cy && mouseY < cy + 34;
      c.card(bx, cy + 2, bw, 28, over ? 0xFF182033 : 0xFF11151F, over ? 0xFFFFFFFF : 0, over ? 1 : 0, 3.0F);
      int hs = 20;
      int hx = bx + 5;
      int hy = cy + 2 + (28 - hs) / 2;
      if (!uuid.isEmpty()) {
         c.fill(hx - 1, hy - 1, hx + hs + 1, hy + hs + 1, 0xFF000000);
         c.playerHead(uuid, hx, hy, hs);
      } else {
         c.fill(hx, hy, hx + hs, hy + hs, 0xFF222836);
      }
      int tx = hx + hs + 6;
      int tw = bx + bw - 4 - tx;
      c.text(trunc(c, uname, tw), tx, cy + 7, TEXT, true);
      if (bw >= 120) {
         c.text(trunc(c, loaderLine(), tw), tx, cy + 18, FAINT, false);
      }
   }

   private static String loaderLine() {
      String v = "";

      try {
         v = Platform.game().gameVersion();
      } catch (Throwable ignored) {
      }

      return v != null && !v.isBlank() ? "Fabric " + v : "Swift Client";
   }

   private int chipsY() {
      return this.panelY() + 34 + 3;
   }

   private void drawCategoryTabs(Canvas c, int mouseX, int mouseY) {
      this.tabHits.clear();
      List<String> labels = new ArrayList<>();
      List<String> values = new ArrayList<>();
      labels.add(Tr.of("swift.mods.all"));
      values.add(null);
      for (String cat : this.categories()) {
         labels.add(Module.categoryLabel(cat));
         values.add(cat);
      }

      // Tighter chips when they do not fit: tracked capitals, then plain capitals, then less padding
      this.chipPad = 12;
      this.chipTracked = true;
      if (this.chipsWidth(c, labels) > this.gridW()) {
         this.chipTracked = false;
      }
      if (this.chipsWidth(c, labels) > this.gridW()) {
         this.chipPad = 6;
      }

      int y = this.chipsY();
      int h = 16;
      int x = this.gridLeft();
      int[] xs = new int[labels.size()];
      int[] ws = new int[labels.size()];
      int active = 0;
      for (int i = 0; i < labels.size(); i++) {
         String caps = labels.get(i).toUpperCase(Locale.ROOT);
         xs[i] = x;
         ws[i] = (this.chipTracked ? spacedWidth(c, caps) : c.textWidth(caps)) + this.chipPad;
         x += ws[i] + 5;
         String v = values.get(i);
         if (v == null ? this.category == null : v.equals(this.category)) {
            active = i;
         }
      }

      c.pushScissor(this.gridLeft(), y - 3, this.gridW(), h + 8);
      for (int i = 0; i < labels.size(); i++) {
         boolean actif = i == active;
         boolean over = in(mouseX, mouseY, new int[]{xs[i], y, ws[i], h});
         float hov = this.ease("chip:" + i, over ? 1.0F : 0.0F, 30.0F);
         int dy = actif ? 0 : (over ? -1 : 1);
         c.card(xs[i], y + dy, ws[i], h, actif ? ACCENT : (over ? 0xFF182033 : 0xFF11151F), 0, 0, 3.0F);
         this.tabHits.add(new ModsScreen.TabHit(xs[i], ws[i], values.get(i)));
         String caps = labels.get(i).toUpperCase(Locale.ROOT);
         int col = actif || over ? TEXT : DIM;
         if (this.chipTracked) {
            spacedCentered(c, caps, xs[i] + ws[i] / 2, y + dy + (h - 8) / 2 + 1, col, ws[i]);
         } else {
            c.centeredText(caps, xs[i] + ws[i] / 2, y + dy + (h - 8) / 2 + 1, col, false);
         }
      }

      c.popScissor();
      this.chipPad = 12;
      this.chipTracked = true;
   }

   private int chipPad = 12;
   private boolean chipTracked = true;

   private int chipsWidth(Canvas c, List<String> labels) {
      int w = 0;
      for (String l : labels) {
         String caps = l.toUpperCase(Locale.ROOT);
         w += (this.chipTracked ? spacedWidth(c, caps) : c.textWidth(caps)) + this.chipPad + 5;
      }
      return w - 5;
   }

   private int chip(Canvas c, String label, String value, int x, int y, int mouseX, int mouseY, boolean active) {
      String caps = label.toUpperCase(Locale.ROOT);
      int w = (this.chipTracked ? spacedWidth(c, caps) : c.textWidth(caps)) + this.chipPad;
      int h = 16;
      this.tabHits.add(new ModsScreen.TabHit(x, w, value));
      boolean over = in(mouseX, mouseY, new int[]{x, y, w, h});
      int dy = active ? 0 : (over ? -1 : 1);
      c.card(x, y + dy, w, h, active ? ACCENT : (over ? 0xFF182033 : 0xFF11151F), 0, 0, 3.0F);
      int col = active || over ? TEXT : DIM;
      if (this.chipTracked) {
         spacedCentered(c, caps, x + w / 2, y + dy + (h - 8) / 2 + 1, col, w);
      } else {
         c.centeredText(caps, x + w / 2, y + dy + (h - 8) / 2 + 1, col, false);
      }
      return x + w + 5;
   }

   private static boolean lockedM(Module m) {
      return false;
   }

   private void drawModCard(Canvas c, Module m, int[] r, int mouseX, int mouseY) {
      boolean on = m.isEnabled();
      boolean inGrid = mouseY >= this.gridTop() && mouseY <= this.gridBottom();
      boolean hover = inGrid && in(mouseX, mouseY, r);
      float hov = this.ease("card:" + m.id, hover ? 1.0F : 0.0F, 30.0F);
      float onT = this.ease("on:" + m.id, on ? 1.0F : 0.0F, 30.0F);
      int lift = hover ? 2 : 0;
      int y = r[1] - lift;
      // A lit module has an accent frame and a lamp; an idle one is a dark block.
      c.card(r[0], y, r[2], r[3], hover ? 0xFF182033 : 0xFF11151F, on ? ACCENT : (hover ? 0xFF3A4766 : 0), 1, 4.0F);
      dev.swiftclient.core.ui.Px.lamp(c, r[0] + 6, y + 6, on);

      int is = 16;
      c.icon(m.icon == null ? "mods" : m.icon, r[0] + (r[2] - is) / 2, y + 6, is, on ? 0xFFFFFFFF : (hover ? 0xFFE6ECF7 : 0xFF98A3B8));
      c.centeredText(trunc(c, m.displayName(), r[2] - 8), r[0] + r[2] / 2, y + 25, on || hover ? TEXT : DIM, false);

      // ON / OFF key: sinks and lights up when the module is on
      int[] t = toggleRect(r);
      t[1] -= lift;
      boolean tOver = inGrid && in(mouseX, mouseY, t);
      c.card(t[0], t[1], t[2], t[3], on ? (tOver ? 0xFF6FA8FF : ACCENT) : (tOver ? 0xFF2A3350 : 0xFF1E2433), 0, 0, 2.0F);
      String label = on ? Tr.of("swift.mods.enabled") : Tr.of("swift.mods.disabled");
      c.centeredText(trunc(c, label.toUpperCase(Locale.ROOT), t[2] - 4), t[0] + t[2] / 2, t[1] + (t[3] - c.lineHeight()) / 2 + 1, on ? TEXT : 0xFF8A93A0, on);

      // Settings gear
      int[] g = gearRect(r);
      g[1] -= lift;
      boolean has = m.hasSettings();
      boolean gOver = has && inGrid && in(mouseX, mouseY, g);
      c.card(g[0], g[1], g[2], g[3], gOver ? ACCENT : 0xFF1E2433, 0, 0, 2.0F);
      c.icon("gear", g[0] + (g[2] - 8) / 2, g[1] + (g[3] - 8) / 2, 8, has ? (gOver ? TEXT : DIM) : 0xFF3A4152);
   }

   private static final int LETTER_SPACING = 1;

   private static int spacedWidth(Canvas c, String s) {
      int w = 0;
      for (int i = 0; i < s.length(); i++) {
         w += c.textWidth(String.valueOf(s.charAt(i)));
      }

      return w + Math.max(0, s.length() - 1) * LETTER_SPACING;
   }

   /** Capitals with a little tracking, like the Lunar slabs; plain text when it would not fit. */
   private static void spacedCentered(Canvas c, String s, int cx, int y, int argb, int maxW) {
      if (spacedWidth(c, s) > maxW) {
         c.centeredText(trunc(c, s, maxW), cx, y, argb, false);
         return;
      }

      int x = cx - spacedWidth(c, s) / 2;
      for (int i = 0; i < s.length(); i++) {
         String ch = String.valueOf(s.charAt(i));
         c.text(ch, x, y, argb, false);
         x += c.textWidth(ch) + LETTER_SPACING;
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
         case TEXT -> 130;
         default -> 150;
      };
   }

   private int[] setSwitchRect(ModsScreen.SetRow r) {
      return new int[]{this.gridRight() - 40, this.mid(r, 12), 26, 12};
   }

   private int[] setSliderTrack(ModsScreen.SetRow r) {
      return new int[]{this.gridRight() - 118, this.mid(r, 8), 96, 8};
   }

   private int[] setSwatchRect(ModsScreen.SetRow r) {
      return new int[]{this.gridRight() - 44, this.mid(r, 13), 34, 13};
   }

   private int[] setTextRect(ModsScreen.SetRow r) {
      return new int[]{this.gridRight() - 126, this.mid(r, 15), 118, 15};
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
         List<String> desc = wrap(c, s.displayDescription(), dispo, 2);
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
            x = this.chip(c, ModuleSetting.groupLabel(g), g, x, y, mouseX, mouseY, g.equals(this.settingsGroup));
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
            c.card(sx, r.y(), sw, rh, 0xFF11151F, 0, 0, 3.0F);
            c.fill(sx + 2, r.y() + 3, sx + 4, r.y() + rh - 3, ACCENT);
            c.text(trunc(c, s.displayName(), sw - 24 - this.ctrlW(s)), sx + 12, r.y() + 9, TEXT, true);

            for (int i = 0; i < r.desc().size(); i++) {
               c.text(r.desc().get(i), sx + 12, r.y() + 9 + 10 + i * 9, -10394518, false);
            }

            this.drawControl(c, r, mouseX, mouseY);
         }
      }

      c.popScissor();
      if (this.setRows.isEmpty()) {
         c.centeredText(Tr.of("swift.mods.no_setting"), this.contentX() + this.contentW() / 2, top + 40, -10394518, false);
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
            String lbl = listening ? Tr.of("swift.mods.press_key") : s.keyName();
            c.centeredText(trunc(c, lbl, b[2] - 8), b[0] + b[2] / 2, b[1] + (b[3] - 8) / 2, -1, false);
            break;
         }
         case TEXT: {
            int[] b = this.setTextRect(r);
            boolean editing = this.editingText == s;
            boolean over = in(mouseX, mouseY, b);
            c.card(b[0], b[1], b[2], b[3], -15263718, editing ? ACCENT : (over ? ACCENT_DIM : 872415231), 1, 5.0F);
            String v = s.textValue();
            String shown = v.isEmpty() && !editing ? Tr.of("swift.mods.text_empty") : v;
            // Keep the end of the text (where the caret is) visible
            while (shown.length() > 1 && c.textWidth(shown) > b[2] - 14) {
               shown = shown.substring(1);
            }

            c.text(shown, b[0] + 6, b[1] + (b[3] - 8) / 2, v.isEmpty() && !editing ? FAINT : TEXT, false);
            if (editing && this.caret / 20L % 2L == 0L) {
               int cx = b[0] + 6 + c.textWidth(shown) + 1;
               c.fill(cx, b[1] + 3, cx + 1, b[1] + b[3] - 3, TEXT);
            }
            break;
         }
         default:
            int[] tr = this.setSliderTrack(r);
            dev.swiftclient.core.ui.Px.segBar(c, tr[0], tr[1], tr[2], tr[3], (float)s.fraction(), ACCENT, 16);
            int fw = (int)(tr[2] * s.fraction());
            c.fill(tr[0] + fw - 3, tr[1] - 3, tr[0] + fw + 3, tr[1] + tr[3] + 3, 0xFF000000);
            c.fill(tr[0] + fw - 2, tr[1] - 2, tr[0] + fw + 2, tr[1] + tr[3] + 2, TEXT);
            String v = fmt(s.value()) + s.unit;
            c.text(v, tr[0] - 8 - c.textWidth(v), tr[1] + (tr[3] - 8) / 2 - 1, -6644317, false);
      }
   }

   private void drawSwitch(Canvas c, int x, int y, int w, int h, boolean on) {
      dev.swiftclient.core.ui.Px.toggle(c, x, y, on);
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
      c.text(this.pickerFor.displayName(), p[0] + 12, p[1] + 9, -1, false);
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
         this.editingText = null;
         int px = this.panelX();
         int py = this.panelY();
         int pw = this.panelW();
         int ph = this.panelH();
         if (!(mx < px) && !(mx > px + pw) && !(my < py) && !(my > py + ph)) {
            for (int[] h : this.hotHits) {
               if (mx >= h[0] && mx < h[0] + h[2] && my >= h[1] && my < h[1] + h[3]) {
                  this.onNav(h[4]);
                  return true;
               }
            }

            if (in((int)mx, (int)my, this.editHudRect())) {
               this.onNav(1);
               return true;
            }

            int cy = this.accountCardY();
            int abw = this.accountW();
            if (mx >= this.panelX() + 12 && mx < this.panelX() + 12 + abw && my >= cy && my < cy + 34) {
               this.onNav(4);
               return true;
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
                  if (my >= this.chipsY() && my < this.chipsY() + 15) {
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
                           case TEXT:
                              if (in((int)mx, (int)my, this.setTextRect(r))) {
                                 this.editingText = s;
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
                  if (my >= this.chipsY() && my < this.chipsY() + 15) {
                     for (ModsScreen.TabHit txx : this.tabHits) {
                        if (mx >= txx.x() && mx < txx.x() + txx.w()) {
                           if (!java.util.Objects.equals(this.category, txx.value())) {
                              this.contentChanged();
                           }

                           this.category = txx.value();
                           this.grille.haut();
                           return true;
                        }
                     }
                  }

                  if (!(my < this.gridTop()) && !(my > this.gridBottom())) {
                     List<Module> mods = this.filtered();

                     for (int i = 0; i < mods.size(); i++) {
                        int[] r = this.cardRect(i);
                        if (in((int)mx, (int)my, r)) {
                           Module m = mods.get(i);
                           if (m.hasSettings() && in((int)mx, (int)my, gearRect(r))) {
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
      this.contentChanged();
      this.settingsFor = m;
      List<String> g = groupsOf(m);
      this.settingsGroup = g.size() > 1 ? g.get(0) : null;
      this.reglages.haut();
   }

   private void closeSettings() {
      this.contentChanged();
      this.settingsFor = null;
      this.settingsGroup = null;
      this.draggingSetting = null;
      this.listeningKey = null;
      this.editingText = null;
      this.reglages.haut();
   }

   private void onNav(int id) {
      if (id == 0) {
         this.setEmbedded(null, 0);
      } else if (id == 1) {
         this.open(new HudEditorScreen());
      } else if (id == 3) {
         this.setEmbedded(new WardrobeScreen(), 3);
      } else if (id == 5) {
         this.setEmbedded(new LanguageScreen(), 5);
      } else if (id == 7) {
         this.setEmbedded(new HostWorldScreen(), 7);
      } else if (id == 6) {
         this.setEmbedded(new ProfilesScreen(), 6);
      } else if (id == 8) {
         this.setEmbedded(new dev.swiftclient.core.screen.FriendsScreen(), 8);
      } else if (id == 4) {
         this.setEmbedded(new AccountScreen(), 4);
      }
   }

   @Override
   public boolean charTyped(String s) {
      if (this.embedded != null) {
         return this.embedded.charTyped(s);
      } else if (this.editingText != null) {
         this.editingText.setText(this.editingText.textValue() + s);
         return true;
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
      } else if (this.editingText != null) {
         if (keyCode == 259) {
            String v = this.editingText.textValue();
            if (!v.isEmpty()) {
               this.editingText.setText(v.substring(0, v.length() - 1));
            }
         } else if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
            this.editingText = null;
         }

         return true;
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
         if (keyCode >= 49 && keyCode <= 56 && this.settingsFor == null) {
            int want = keyCode - 49;
            if (want < this.hotHits.size()) {
               this.onNav(this.hotHits.get(want)[4]);
               return true;
            }
         }

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
      return this.embedded == null && this.settingsFor == null && this.pickerFor == null && this.listeningKey == null && this.editingText == null && !this.searchFocused;
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
         this.grille.cran(amount, TILE_H / 2.0);
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
