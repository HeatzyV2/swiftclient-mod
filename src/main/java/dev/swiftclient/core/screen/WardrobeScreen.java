package dev.swiftclient.core.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.swiftclient.core.cosmetics.CosmeticHttp;
import dev.swiftclient.core.cosmetics.CosmeticState;
import dev.swiftclient.core.cosmetics.PetState;
import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.net.Backend;
import dev.swiftclient.core.net.Net;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.ui.Defilement;
import dev.swiftclient.core.ui.Kit;
import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.ui.UiScreen;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Cosmetics: a preview of the player on the left (drag to turn), and on the right the capes (Swift and
 * Minecraft ones) as cards with their name, or the companions as rows with a buy / equip button.
 * Same data and backend calls as before; only the screen is new.
 */
public class WardrobeScreen extends UiScreen {
   private static final String NONE = "__none__";
   private static final int PAD = 12;
   private static final int HEAD_H = 30;
   private static final int GAP = 7;
   private static final int CARD_MIN_W = 54;
   private static final int ROW_H = 30;
   private static final int SECTION_H = 16;
   private static final ExecutorService IO = Net.IO;

   private static final int SLOT = 0xFF0B0E15;
   private static final int OK = 0xFF3ECF8E;

   private enum Tab {
      CAPES, PETS
   }

   private Tab tab = Tab.CAPES;
   private volatile List<Item> items = List.of();
   private volatile List<PetItem> petItems = List.of();
   private volatile String equippedPet;
   private volatile int coins = -1;
   private volatile boolean loaded;
   private volatile String selected;
   private volatile String status;
   private volatile boolean statusError;
   private volatile long statusMs;
   private boolean loadStarted;
   private String pendingBuy;
   private final Defilement defil = new Defilement();
   private String query = "";
   private boolean searchFocused;
   private long caret;
   private long born = System.nanoTime();

   // Hit areas, rebuilt every frame
   private int[] animRect = new int[4];
   private final List<int[]> tabRects = new ArrayList<>();

   public WardrobeScreen() {
      CosmeticState.warmup();
   }

   @Override
   public String title() {
      return Tr.of("swift.wardrobe.title");
   }

   @Override
   public void layout(int width, int height) {
      super.layout(width, height);
      if (!this.loadStarted) {
         this.loadStarted = true;
         IO.execute(this::loadData);
      }
   }

   // --- Geometry ---

   private int bodyTop() {
      return HEAD_H + 4;
   }

   private int bodyBottom() {
      return this.height - 8;
   }

   private int previewW() {
      return this.width >= 330 ? Math.max(104, Math.min(150, Math.round(this.width * 0.27F))) : 0;
   }

   private int listX() {
      return this.previewW() > 0 ? PAD + this.previewW() + 10 : PAD;
   }

   private int listW() {
      return this.width - PAD - this.listX() - 8;
   }

   private int columns() {
      return Math.max(1, (this.listW() + GAP) / (CARD_MIN_W + GAP));
   }

   private int cardW() {
      int cols = this.columns();
      return (this.listW() - (cols - 1) * GAP) / cols;
   }

   private int[] searchRect() {
      int w = Math.max(90, Math.min(150, this.width / 3));
      return new int[]{this.width - PAD - w, 6, w, 18};
   }

   /** Cards shrink a little in short windows so a second row is always partly visible. */
   private int cardH() {
      return this.bodyBottom() - this.bodyTop() < 150 ? 64 : 72;
   }

   private boolean hasPets() {
      return !this.petItems.isEmpty();
   }

   // --- Data ---

   private void pose(String message, boolean error) {
      this.status = message;
      this.statusError = error;
      this.statusMs = System.currentTimeMillis();
   }

   /** Status line for the outcome of a backend call, with a toast on failure (seen even if the screen is closed). */
   private boolean report(Backend.Response r, String success) {
      if (r.ok()) {
         this.pose(success, false);
         return true;
      } else {
         this.pose(r.message(), true);
         Platform.game().notify(Tr.of("swift.wardrobe.toast"), r.message());
         return false;
      }
   }

   private void loadData() {
      List<Item> out = new ArrayList<>();
      out.add(new Item(NONE, Tr.of("swift.wardrobe.none"), "", null));
      Backend.Response ownedResponse = CosmeticHttp.ownedSelfResponse();
      JsonObject own = ownedResponse.ok() && ownedResponse.json() != null && ownedResponse.json().isJsonObject() ? ownedResponse.json().getAsJsonObject() : null;
      if (!ownedResponse.ok()) {
         this.pose(ownedResponse.message(), true);
      }

      JsonArray catalog = CosmeticHttp.catalog();
      List<Item> customs = new ArrayList<>();
      Set<String> ownedSet = new HashSet<>();
      if (own != null && own.has("owned")) {
         for (JsonElement e : own.getAsJsonArray("owned")) {
            ownedSet.add(e.getAsString());
         }

         for (JsonElement el : catalog) {
            if (el.isJsonObject()) {
               JsonObject o = el.getAsJsonObject();
               String id = o.has("id") ? o.get("id").getAsString() : null;
               boolean free = !o.has("price") || o.get("price").getAsInt() <= 0;
               if (id != null && (free || ownedSet.contains(id)) && !"pet".equals(o.has("type") ? o.get("type").getAsString() : "")) {
                  customs.add(new Item(id, o.has("name") ? o.get("name").getAsString() : id, Tr.of("swift.wardrobe.section.swift"), null));
               }
            }
         }
      }

      out.addAll(customs);
      JsonArray mojang = CosmeticHttp.mojangCapes();
      String activeMojangUrl = null;
      if (mojang != null) {
         for (JsonElement elx : mojang) {
            if (elx.isJsonObject()) {
               JsonObject o = elx.getAsJsonObject();
               String url = o.has("url") ? o.get("url").getAsString() : null;
               String id = o.has("id") ? o.get("id").getAsString() : null;
               if (url != null && id != null) {
                  String alias = o.has("alias") ? o.get("alias").getAsString() : "Cape";
                  if (o.has("state") && "ACTIVE".equals(o.get("state").getAsString())) {
                     activeMojangUrl = url;
                  }

                  out.add(new Item("mojang:" + url, alias, Tr.of("swift.wardrobe.section.minecraft"), id));
               }
            }
         }
      }

      String eq = own != null && own.has("equipped") && !own.get("equipped").isJsonNull() ? own.get("equipped").getAsString() : null;
      boolean customOn = eq != null && !eq.equals("none") && customs.stream().anyMatch(i -> eq.equals(i.value));
      this.selected = customOn ? eq : (activeMojangUrl != null ? "mojang:" + activeMojangUrl : null);
      if (own == null && mojang == null) {
         this.pose(Tr.of("swift.wardrobe.unreachable"), true);
      }

      this.items = out;
      List<PetItem> pl = new ArrayList<>();
      for (JsonElement el : catalog) {
         if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            if ("pet".equals(o.has("type") ? o.get("type").getAsString() : "")) {
               String id = o.get("id").getAsString();
               int price = o.has("price") ? o.get("price").getAsInt() : 0;
               pl.add(new PetItem(id, o.has("name") ? o.get("name").getAsString() : id, price, ownedSet.contains(id)));
            }
         }
      }

      this.petItems = pl;
      this.equippedPet = own != null && own.has("equippedPet") && !own.get("equippedPet").isJsonNull() ? own.get("equippedPet").getAsString() : null;
      this.coins = CosmeticHttp.balance();
      this.loaded = true;
   }

   private List<Item> visibleItems() {
      String q = this.query.trim().toLowerCase(Locale.ROOT);
      if (q.isEmpty()) {
         return this.items;
      }

      List<Item> out = new ArrayList<>();
      for (Item it : this.items) {
         if (it.value.equals(NONE) || it.label.toLowerCase(Locale.ROOT).contains(q)) {
            out.add(it);
         }
      }

      return out;
   }

   /** Walks the grid in order: sections first, then cards. Return true to stop; the result is the content height. */
   private int walk(Visitor v) {
      int y = 0;
      int col = 0;
      int cols = this.columns();
      int cw = this.cardW();
      String section = null;
      for (Item it : this.visibleItems()) {
         if (!it.section.isEmpty() && !it.section.equals(section)) {
            if (col > 0) {
               y += this.cardH() + GAP;
               col = 0;
            }

            section = it.section;
            if (v.cell(null, section, this.listX(), y, cw)) {
               return y;
            }

            y += SECTION_H;
         }

         if (v.cell(it, null, this.listX() + col * (cw + GAP), y, cw)) {
            return y;
         }

         if (++col >= cols) {
            col = 0;
            y += this.cardH() + GAP;
         }
      }

      if (col > 0) {
         y += this.cardH() + GAP;
      }

      return Math.max(0, y - GAP);
   }

   // --- Drawing ---

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      this.caret++;
      this.defil.anime();
      this.drawHeader(c, mouseX, mouseY);
      if (this.previewW() > 0) {
         this.drawPreview(c, mouseX, mouseY, delta);
      }

      int top = this.bodyTop();
      int bottom = this.bodyBottom();
      int cx = this.listX() + this.listW() / 2;
      if (!this.loaded) {
         this.drawLoading(c, cx, (top + bottom) / 2);
      } else if (this.tab == Tab.PETS) {
         this.drawPets(c, mouseX, mouseY);
      } else {
         this.drawCapes(c, mouseX, mouseY, top, bottom);
      }
   }

   private void drawLoading(Canvas c, int cx, int cy) {
      long t = (System.nanoTime() - this.born) / 1_000_000L;
      for (int i = 0; i < 3; i++) {
         float k = (float)Math.abs(Math.sin(t / 380.0 + i * 0.7));
         c.roundRect(cx - 14 + i * 10, cy - 3 - Math.round(k * 4), 6, 6, 3.0F, Px.ACCENT);
      }

      c.centeredText(Tr.of("swift.common.loading"), cx, cy + 12, Px.FAINT, false);
   }

   private void drawHeader(Canvas c, int mouseX, int mouseY) {
      Px.title(c, Tr.of("swift.wardrobe.title"), PAD, 6);
      int x = PAD + c.textWidth(Tr.of("swift.wardrobe.title").toUpperCase(Locale.ROOT)) + 16;
      this.tabRects.clear();
      Tab[] tabs = this.hasPets() ? Tab.values() : new Tab[]{Tab.CAPES};
      for (Tab t : tabs) {
         String label = (t == Tab.CAPES ? Tr.of("swift.wardrobe.capes") : Tr.of("swift.wardrobe.pets")).toUpperCase(Locale.ROOT);
         int w = c.textWidth(label) + 18;
         int[] r = {x, 7, w, 16};
         boolean on = t == this.tab;
         boolean over = Kit.dedans(mouseX, mouseY, r);
         c.card(r[0], r[1] + (on ? 0 : (over ? -1 : 1)), w, r[3], on ? Px.ACCENT : (over ? 0xFF182033 : 0xFF11151F), 0, 0, 3.0F);
         c.centeredText(label, x + w / 2, r[1] + (on ? 0 : (over ? -1 : 1)) + 4, on || over ? -1 : Px.DIM, on);
         this.tabRects.add(new int[]{r[0], r[1], r[2], r[3], t.ordinal()});
         x += w + 5;
      }

      int[] sr = this.searchRect();
      int rightLimit = sr[0];
      if (this.tab == Tab.CAPES) {
         Kit.recherche(c, sr, this.query, this.searchFocused, this.caret, Tr.of("swift.wardrobe.search"), mouseX, mouseY);
      } else {
         rightLimit = this.width - PAD;
      }

      if (this.coins >= 0 && this.tab == Tab.PETS) {
         String s = String.valueOf(this.coins);
         int w = c.textWidth(s) + 28;
         int px = rightLimit - w - 8;
         c.card(px, 6, w, 18, 0xFF11151F, 0xFF2A3350, 1, 9.0F);
         c.icon("coins", px + 6, 10, 10, 0xFFF5C451);
         c.text(s, px + 20, 11, -1, false);
         rightLimit = px;
      }

      String st = this.status;
      if (st != null && !st.isBlank() && (this.statusError || System.currentTimeMillis() - this.statusMs <= 4000L)) {
         int free = rightLimit - x - 12;
         if (free > 40) {
            String t = Kit.tronque(c, st, free);
            c.text(t, rightLimit - 8 - c.textWidth(t), 11, this.statusError ? Px.DANGER : Px.DIM, false);
         }
      }

      c.fill(PAD, HEAD_H, this.width - PAD, HEAD_H + 1, 0x1AFFFFFF);
   }

   private void drawPreview(Canvas c, int mouseX, int mouseY, float delta) {
      int x = PAD;
      int y = this.bodyTop();
      int w = this.previewW();
      int h = this.bodyBottom() - y;
      c.card(x, y, w, h, 0xFF0D1119, 0xFF1B2438, 1, 6.0F);
      int footer = h >= 128 ? 46 : 26;
      // The 3D model keeps to its own box: the canvas turns these coordinates into screen ones
      c.pushScissor(x + 1, y + 1, w - 2, h - footer - 1);
      c.playerModel(x + 4, y + 4, w - 8, h - footer - 8, mouseX, mouseY, delta);
      c.popScissor();
      c.icon("hand", x + w - 15, y + 5, 9, 0x55FFFFFF);
      int fy = y + h - footer;
      c.fill(x + 8, fy, x + w - 8, fy + 1, 0x1AFFFFFF);
      String worn = this.selected == null ? Tr.of("swift.wardrobe.no_cape") : this.labelOf(this.selected);
      c.icon("cape", x + 8, fy + 7, 9, Px.DIM);
      c.text(Kit.tronque(c, worn, w - 36), x + 21, fy + 8, -1, false);
      if (footer > 30) {
         boolean anim = CosmeticState.selfAnimated();
         c.text(Tr.of("swift.wardrobe.animated"), x + 8, fy + 28, Px.FAINT, false);
         int sx = x + w - 8 - 26;
         int sy = fy + 26;
         Px.toggle(c, sx, sy, anim);
         this.animRect = new int[]{x + 4, fy + 20, w - 8, 22};
      } else {
         this.animRect = new int[]{0, 0, 0, 0};
      }
   }

   private String labelOf(String value) {
      for (Item i : this.items) {
         if (value.equals(i.value)) {
            return i.label;
         }
      }

      return value;
   }

   private void drawCapes(Canvas c, int mouseX, int mouseY, int top, int bottom) {
      int content = this.walk((i, s, x, y, w) -> false);
      this.defil.contenu(content, bottom - top);
      int gx = this.listX();
      int gw = this.listW();
      c.pushScissor(gx - 3, top, gw + 6, bottom - top);
      int base = top - this.defil.px();
      this.walk((it, section, x, y, w) -> {
         int sy = base + y;
         if (sy <= bottom && sy + this.cardH() >= top - SECTION_H) {
            if (section != null) {
               Kit.section(c, gx, sy + 3, section);
            } else {
               this.drawCard(c, it, x, sy, w, mouseX, mouseY, top, bottom);
            }
         }

         return false;
      });
      c.popScissor();
      Kit.barre(c, this.width - PAD - 3, top, bottom, this.defil.px(), this.defil.maxPx());
      if (this.visibleItems().size() <= 1 && !this.query.isBlank()) {
         c.centeredText(Tr.of("swift.wardrobe.search_none"), gx + gw / 2, top + this.cardH() + 24, Px.FAINT, false);
      }
   }

   private void drawCard(Canvas c, Item it, int x, int y, int w, int mouseX, int mouseY, int top, int bottom) {
      boolean chosen = it.value.equals(this.selected) || it.value.equals(NONE) && this.selected == null;
      boolean hover = Kit.dedans(mouseX, mouseY, x, y, w, this.cardH()) && mouseY >= top && mouseY <= bottom;
      int lift = hover && !chosen ? 1 : 0;
      c.card(x, y - lift, w, this.cardH(), hover ? 0xFF182033 : 0xFF11151F, chosen ? Px.ACCENT : (hover ? 0xFF3A4766 : 0), 1, 5.0F);
      int sx = x + 5;
      int sy = y - lift + 5;
      int sw = w - 10;
      int sh = this.cardH() - 26;
      c.card(sx, sy, sw, sh, SLOT, 0, 0, 3.0F);
      Object frame = it.value.equals(NONE) ? null : CosmeticState.currentFrame(it.value);
      if (frame != null) {
         int[] m = CosmeticState.metaFor(it.value);
         int fw = m[2];
         int fh = m[3];
         int ph = sh - 8;
         int pw = ph * 10 / 16;
         c.textureRegion(frame, sx + (sw - pw) / 2, sy + 4, pw, ph, fw / 64, fh / 32, fw * 10 / 64, fh * 16 / 32, fw, fh);
      } else if (it.value.equals(NONE)) {
         c.icon("close", sx + (sw - 16) / 2, sy + (sh - 16) / 2, 16, 0xFF3A4152);
      } else {
         c.icon("cape", sx + (sw - 16) / 2, sy + (sh - 16) / 2, 16, 0xFF3A4152);
      }

      c.centeredText(Kit.tronque(c, it.label, w - 8), x + w / 2, y - lift + this.cardH() - 15, chosen || hover ? -1 : Px.DIM, false);
      if (chosen) {
         c.roundRect(x + w - 15, y - lift + 3, 12, 12, 6.0F, Px.ACCENT);
         c.icon("check", x + w - 13, y - lift + 5, 8, -1);
      }
   }

   private int petY(int i) {
      return this.bodyTop() + i * (ROW_H + 5) - this.defil.px();
   }

   private void drawPets(Canvas c, int mouseX, int mouseY) {
      int top = this.bodyTop();
      int bottom = this.bodyBottom();
      int n = this.petItems.size() + 1;
      this.defil.contenu(n * (ROW_H + 5) - 5, bottom - top);
      c.pushScissor(this.listX() - 3, top, this.listW() + 6, bottom - top);
      this.drawPet(c, 0, Tr.of("swift.wardrobe.none"), null, this.equippedPet == null, true, 0, mouseX, mouseY);
      for (int i = 0; i < this.petItems.size(); i++) {
         PetItem p = this.petItems.get(i);
         this.drawPet(c, i + 1, p.name, p, p.id.equals(this.equippedPet), p.owned, p.price, mouseX, mouseY);
      }

      c.popScissor();
      Kit.barre(c, this.width - PAD - 3, top, bottom, this.defil.px(), this.defil.maxPx());
   }

   private int[] actionRect(int y) {
      return new int[]{this.listX() + this.listW() - 92, y + (ROW_H - 18) / 2, 84, 18};
   }

   private void drawPet(Canvas c, int i, String name, PetItem p, boolean equipped, boolean owned, int price, int mouseX, int mouseY) {
      int x = this.listX();
      int y = this.petY(i);
      int w = this.listW();
      boolean hover = Kit.dedans(mouseX, mouseY, x, y, w, ROW_H) && mouseY >= this.bodyTop() && mouseY <= this.bodyBottom();
      c.card(x, y, w, ROW_H, hover ? 0xFF182033 : 0xFF11151F, equipped ? Px.ACCENT : (hover ? 0xFF3A4766 : 0), 1, 5.0F);
      c.card(x + 5, y + 5, ROW_H - 10, ROW_H - 10, SLOT, 0, 0, 3.0F);
      c.icon(p == null ? "close" : "pet", x + 8, y + 8, ROW_H - 16, p == null ? 0xFF3A4152 : (owned ? 0xFFE6ECF7 : Px.DIM));
      int[] r = this.actionRect(y);
      int nameMax = r[0] - (x + ROW_H + 4) - 6;
      c.text(Kit.tronque(c, name, nameMax), x + ROW_H + 4, y + (ROW_H - 8) / 2, equipped || hover ? -1 : Px.DIM, false);
      if (equipped) {
         c.icon("check", r[0] + r[2] - 22, y + (ROW_H - 9) / 2, 9, OK);
         c.text(Tr.of("swift.wardrobe.equipped"), r[0] + r[2] - 26 - c.textWidth(Tr.of("swift.wardrobe.equipped")), y + (ROW_H - 8) / 2, OK, false);
      } else {
         String action;
         boolean primary = false;
         if (p == null) {
            action = Tr.of("swift.wardrobe.remove");
         } else if (p.owned) {
            action = Tr.of("swift.wardrobe.equip");
            primary = true;
         } else if (p.id.equals(this.pendingBuy)) {
            action = Tr.of("swift.wardrobe.confirm", p.price);
            primary = true;
         } else {
            action = Tr.of("swift.wardrobe.coins", p.price);
         }

         Px.button(c, r[0], r[1], r[2], r[3], Kit.tronque(c, action, r[2] - 8), Kit.dedans(mouseX, mouseY, r), primary);
      }
   }

   // --- Input ---

   @Override
   public boolean click(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      }

      if (Kit.dedans(mouseX, mouseY, this.animRect) && this.previewW() > 0) {
         CosmeticState.setSelfAnimated(!CosmeticState.selfAnimated());
         Platform.game().playClick();
         return true;
      }

      for (int[] t : this.tabRects) {
         if (Kit.dedans(mouseX, mouseY, t)) {
            Tab next = Tab.values()[t[4]];
            if (next != this.tab) {
               this.tab = next;
               this.defil.haut();
               this.searchFocused = false;
               Platform.game().playClick();
            }

            return true;
         }
      }

      if (this.tab == Tab.CAPES) {
         boolean onSearch = Kit.dedans(mouseX, mouseY, this.searchRect());
         this.searchFocused = onSearch;
         if (onSearch) {
            return true;
         }
      }

      if (!this.loaded || mouseY < this.bodyTop() || mouseY > this.bodyBottom()) {
         return false;
      }

      if (this.tab == Tab.PETS) {
         return this.clickPet(mouseX, mouseY);
      }

      int base = this.bodyTop() - this.defil.px();
      Item[] target = new Item[1];
      this.walk((it, section, x, y, w) -> {
         if (it != null && Kit.dedans(mouseX, mouseY, x, base + y, w, this.cardH())) {
            target[0] = it;
            return true;
         }

         return false;
      });
      if (target[0] != null) {
         this.equip(target[0]);
         return true;
      }

      return false;
   }

   private boolean clickPet(double mouseX, double mouseY) {
      int n = this.petItems.size() + 1;
      for (int i = 0; i < n; i++) {
         int y = this.petY(i);
         if (Kit.dedans(mouseX, mouseY, this.listX(), y, this.listW(), ROW_H)) {
            if (i == 0) {
               this.pendingBuy = null;
               this.equipPet(null);
               return true;
            }

            PetItem p = this.petItems.get(i - 1);
            if (p.owned) {
               this.pendingBuy = null;
               this.equipPet(p.id);
            } else if (p.id.equals(this.pendingBuy)) {
               this.buyPet(p);
            } else {
               this.pendingBuy = p.id;
               Platform.game().playClick();
               this.pose(Tr.of("swift.wardrobe.click_again", p.price), false);
            }

            return true;
         }
      }

      return false;
   }

   @Override
   public boolean charTyped(String s) {
      if (!this.searchFocused) {
         return false;
      }

      this.query = this.query + s;
      this.defil.haut();
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode) {
      if (!this.searchFocused) {
         return false;
      } else if (keyCode == KEY_BACKSPACE && !this.query.isEmpty()) {
         this.query = this.query.substring(0, this.query.length() - 1);
         this.defil.haut();
         return true;
      } else if (keyCode == KEY_ESCAPE) {
         this.query = "";
         this.searchFocused = false;
         return true;
      } else if (keyCode == KEY_ENTER) {
         this.searchFocused = false;
         return true;
      }

      return false;
   }

   @Override
   public boolean closeOnEscape() {
      return !this.searchFocused;
   }

   @Override
   public boolean scroll(double mouseX, double mouseY, double amount) {
      this.defil.cran(amount, this.cardH() / 2.0);
      return true;
   }

   // --- Actions (unchanged behaviour) ---

   private void equip(Item it) {
      Platform.game().playClick();
      boolean none = it.value.equals(NONE) || it.value.equals(this.selected);
      this.selected = none ? null : it.value;
      this.pose(Tr.of("swift.wardrobe.applying"), false);
      CosmeticState.applySelf(none ? "" : it.value);
      IO.execute(() -> {
         if (none) {
            Backend.Response r = CosmeticHttp.equipCosmetic("none");
            CosmeticHttp.disableMojangCape();
            CosmeticHttp.declareMojangCape(null);
            this.report(r, Tr.of("swift.wardrobe.applied"));
         } else if (it.extra != null) {
            CosmeticHttp.equipCosmetic("none");
            if (CosmeticHttp.setMojangCapeActive(it.extra)) {
               this.report(CosmeticHttp.declareMojangCape(it.value.substring("mojang:".length())), Tr.of("swift.wardrobe.applied"));
            } else {
               this.pose(Tr.of("swift.wardrobe.mojang_refused"), true);
               Platform.game().notify(Tr.of("swift.wardrobe.toast"), Tr.of("swift.wardrobe.mojang_refused"));
            }
         } else {
            this.report(CosmeticHttp.equipCosmetic(it.value), Tr.of("swift.wardrobe.applied"));
         }
      });
   }

   private void buyPet(PetItem p) {
      Platform.game().playClick();
      this.pendingBuy = null;
      this.pose(Tr.of("swift.wardrobe.buying"), false);
      IO.execute(() -> {
         if (this.report(CosmeticHttp.buyCosmetic(p.id), Tr.of("swift.wardrobe.bought"))) {
            p.owned = true;
            this.coins = CosmeticHttp.balance();
         }
      });
   }

   private void equipPet(String id) {
      Platform.game().playClick();
      boolean none = id == null || id.equals(this.equippedPet);
      this.equippedPet = none ? null : id;
      PetState.applySelf(none ? "" : id);
      this.pose(Tr.of("swift.wardrobe.applying"), false);
      String target = none ? "none" : id;
      IO.execute(() -> this.report(CosmeticHttp.equipPet(target), Tr.of("swift.wardrobe.applied")));
   }

   private static final class Item {
      final String value;
      final String label;
      final String section;
      final String extra;

      Item(String value, String label, String section, String extra) {
         this.value = value;
         this.label = label;
         this.section = section;
         this.extra = extra;
      }
   }

   private static final class PetItem {
      final String id;
      final String name;
      final int price;
      boolean owned;

      PetItem(String id, String name, int price, boolean owned) {
         this.id = id;
         this.name = name;
         this.price = price;
         this.owned = owned;
      }
   }

   private interface Visitor {
      boolean cell(Item item, String section, int x, int y, int w);
   }
}
