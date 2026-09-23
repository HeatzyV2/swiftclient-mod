package dev.swiftclient.core.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.swiftclient.core.cosmetics.CosmeticHttp;
import dev.swiftclient.core.cosmetics.CosmeticState;
import dev.swiftclient.core.cosmetics.PetState;
import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.ui.Defilement;
import dev.swiftclient.core.ui.Kit;
import dev.swiftclient.core.ui.UiScreen;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WardrobeScreen extends UiScreen {
   private static final int CARTE_W = 56;
   private static final int CARTE_H = 80;
   private static final int ECART = 8;
   private static final int APERCU_W = 30;
   private static final int APERCU_H = 48;
   private static final int SECTION_H = 18;
   private static final String NONE = "__none__";
   private static final WardrobeScreen.Onglet[] ONGLETS = new WardrobeScreen.Onglet[]{
      new WardrobeScreen.Onglet("capes", "Capes", true)
   };
   private int onglet = 0;
   private volatile List<WardrobeScreen.PetItem> petItems = List.of();
   private volatile String equippedPet;
   private volatile int coins = -1;
   private String pendingBuy;
   private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "swiftclient-wardrobe");
      t.setDaemon(true);
      return t;
   });
   private volatile List<WardrobeScreen.Item> items = List.of();
   private volatile boolean loaded;
   private volatile String selected;
   private volatile String status;
   private volatile boolean statusError;
   private volatile long statusMs;
   private boolean loadStarted;
   private final Defilement defil = new Defilement();
   private String query = "";
   private boolean searchFocused;
   private long curseur;
   private Kit.Zone z;
   private int[] rectAnime = new int[]{0, 0, 0, 0};
   private static final int PET_H = 34;
   private static final int PET_ECART = 6;
   private final Map<String, Integer> largeurs = new HashMap<>();

   public WardrobeScreen() {
      CosmeticState.warmup();
   }

   @Override
   public String title() {
      return "Cosmetics";
   }

   @Override
   public void layout(int width, int height) {
      super.layout(width, height);
      this.z = Kit.zone(width);
      if (!this.loadStarted) {
         this.loadStarted = true;
         IO.execute(this::loadData);
      }
   }

   private int corpsHaut() {
      return 64;
   }

   private int corpsBas() {
      return this.height - 12;
   }

   private int apercuW() {
      return this.z.w() >= 360 ? Math.max(110, Math.min(170, Math.round(this.z.w() * 0.32F))) : 0;
   }

   private int grilleX() {
      return this.apercuW() > 0 ? this.z.x() + this.apercuW() + 14 : this.z.x();
   }

   private int grilleW() {
      return this.z.right() - this.grilleX() - 6;
   }

   private int colonnes() {
      return Math.max(1, (this.grilleW() + 8) / 64);
   }

   private void pose(String message, boolean erreur) {
      this.status = message;
      this.statusError = erreur;
      this.statusMs = System.currentTimeMillis();
   }

   private void loadData() {
      List<WardrobeScreen.Item> out = new ArrayList<>();
      out.add(new WardrobeScreen.Item("__none__", "None", "", null));
      JsonObject own = CosmeticHttp.ownedSelf();
      JsonArray catalog = CosmeticHttp.catalog();
      List<WardrobeScreen.Item> customs = new ArrayList<>();
      if (own != null && own.has("owned")) {
         ArrayList<String> ownedIds = new ArrayList<>();

         for (JsonElement e : own.getAsJsonArray("owned")) {
            ownedIds.add(e.getAsString());
         }

         for (JsonElement el : catalog) {
            if (el.isJsonObject()) {
               JsonObject o = el.getAsJsonObject();
               String id = o.has("id") ? o.get("id").getAsString() : null;
               if (id != null && ownedIds.contains(id)) {
                  customs.add(new WardrobeScreen.Item(id, o.has("name") ? o.get("name").getAsString() : id, "Swift Client capes", null));
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

                  out.add(new WardrobeScreen.Item("mojang:" + url, alias, "Minecraft capes", id));
               }
            }
         }
      }

      String eq = own != null && own.has("equipped") && !own.get("equipped").isJsonNull() ? own.get("equipped").getAsString() : null;
      boolean customOn = eq != null && !eq.equals("none") && customs.stream().anyMatch(i -> eq.equals(i.value));
      this.selected = customOn ? eq : (activeMojangUrl != null ? "mojang:" + activeMojangUrl : null);
      if (own == null && mojang == null) {
         this.pose("Can't reach the server, try again later", true);
      }

      this.items = out;
      Set<String> ownedSet = new HashSet<>();
      if (own != null && own.has("owned")) {
         for (JsonElement e : own.getAsJsonArray("owned")) {
            ownedSet.add(e.getAsString());
         }
      }

      List<WardrobeScreen.PetItem> pl = new ArrayList<>();

      for (JsonElement elxx : catalog) {
         if (elxx.isJsonObject()) {
            JsonObject o = elxx.getAsJsonObject();
            if ("pet".equals(o.has("type") ? o.get("type").getAsString() : "")) {
               String id = o.get("id").getAsString();
               int price = o.has("price") ? o.get("price").getAsInt() : 0;
               pl.add(new WardrobeScreen.PetItem(id, o.has("name") ? o.get("name").getAsString() : id, price, ownedSet.contains(id)));
            }
         }
      }

      this.petItems = pl;
      this.equippedPet = own != null && own.has("equippedPet") && !own.get("equippedPet").isJsonNull() ? own.get("equippedPet").getAsString() : null;
      this.coins = CosmeticHttp.balance();
      this.loaded = true;
   }

   private List<WardrobeScreen.Item> visibleItems() {
      String q = this.query.trim().toLowerCase(Locale.ROOT);
      if (q.isEmpty()) {
         return this.items;
      } else {
         List<WardrobeScreen.Item> out = new ArrayList<>();

         for (WardrobeScreen.Item it : this.items) {
            if (it.value.equals("__none__") || it.label.toLowerCase(Locale.ROOT).contains(q)) {
               out.add(it);
            }
         }

         return out;
      }
   }

   private int parcourir(WardrobeScreen.Visiteur v) {
      int y = 0;
      int col = 0;
      int cols = this.colonnes();
      String section = null;

      for (WardrobeScreen.Item it : this.visibleItems()) {
         if (!it.section.isEmpty() && !it.section.equals(section)) {
            if (col > 0) {
               y += 88;
               col = 0;
            }

            if (section != null || y > 0) {
               y += 4;
            }

            section = it.section;
            if (v.cellule(null, section, this.grilleX(), y)) {
               return y;
            }

            y += 18;
         }

         if (v.cellule(it, null, this.grilleX() + col * 64, y)) {
            return y;
         }

         if (++col >= cols) {
            col = 0;
            y += 88;
         }
      }

      if (col > 0) {
         y += 88;
      }

      return y;
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      this.curseur++;
      this.defil.anime();
      WardrobeScreen.Onglet o = ONGLETS[this.onglet];
      Kit.titre(c, this.z, this.title());
      String sous = "pets".equals(o.id())
         ? (this.coins < 0 ? "" : this.coins + " coins")
         : (this.loaded ? String.valueOf(Math.max(0, this.items.size() - 1)) : "");
      Kit.sousTitre(c, this.z, this.title(), sous);
      if ("capes".equals(o.id())) {
         Kit.recherche(c, Kit.rectRecherche(this.width, this.z), this.query, this.searchFocused, this.curseur, "Search capes...", mouseX, mouseY);
      }

      this.dessinerOnglets(c, mouseX, mouseY);
      this.dessinerStatut(c);
      if (this.apercuW() > 0) {
         this.dessinerApercu(c, mouseX, mouseY, delta);
      }

      int gx = this.grilleX();
      int gw = this.grilleW();
      int cx = gx + gw / 2;
      int haut = this.corpsHaut();
      int bas = this.corpsBas();
      if (!o.pret()) {
         c.centeredText("Coming soon", cx, (haut + bas) / 2 - 6, -6644317, false);
      } else if (!this.loaded) {
         c.centeredText("Loading...", cx, (haut + bas) / 2 - 4, -10394518, false);
      } else if ("pets".equals(o.id())) {
         this.dessinerPets(c, mouseX, mouseY);
      } else {
         int contenu = this.parcourir((i, s, x, y) -> false);
         this.defil.contenu(contenu, bas - haut);
         c.pushScissor(gx - 2, haut, gw + 4, bas - haut);
         int base = haut - this.defil.px();
         this.parcourir((it, section, x, y) -> {
            int sy = base + y;
            if (sy <= bas && sy + 80 >= haut - 18) {
               if (section != null) {
                  Kit.section(c, gx, sy + 4, section);
               } else {
                  this.dessinerCarte(c, it, x, sy, mouseX, mouseY);
               }

               return false;
            } else {
               return false;
            }
         });
         c.popScissor();
         Kit.barre(c, this.z.right() - 2, haut, bas, this.defil.px(), this.defil.maxPx());
      }
   }

   private void dessinerOnglets(Canvas c, int mouseX, int mouseY) {
      // Single Capes section — no grey Capes/Skins chip row
      if (ONGLETS.length <= 1) {
         c.text("Capes", this.z.x(), Kit.chipY() + 4, -1, false);
         c.fill(this.z.x(), Kit.chipY() + 16, this.z.x() + 40, Kit.chipY() + 17, -12877066);
         return;
      }

      int x = this.z.x();
      for (int i = 0; i < ONGLETS.length; i++) {
         WardrobeScreen.Onglet o = ONGLETS[i];
         String lib = o.pret() ? o.libelle() : o.libelle() + " (soon)";
         int w = Kit.chip(c, x, lib, i == this.onglet, !o.pret(), mouseX, mouseY);
         this.largeurs.put(o.id(), w);
         x += w + 6;
      }
   }

   private void dessinerStatut(Canvas c) {
      String s = this.status;
      if (s != null && !s.isBlank()) {
         if (this.statusError || System.currentTimeMillis() - this.statusMs <= 4000L) {
            int w = c.textWidth(s);
            c.text(s, this.z.right() - w, Kit.chipY() + 5, this.statusError ? -2067601 : -6644317, false);
         }
      }
   }

   private void dessinerApercu(Canvas c, int mouseX, int mouseY, float delta) {
      int x = this.z.x();
      int y = this.corpsHaut();
      int w = this.apercuW();
      int h = this.corpsBas() - y;
      c.card(x, y, w, h, 218103807, 520093695, 1, 7.0F);
      int basInfos = 44;
      c.playerModel(x + 4, y + 4, w - 8, h - basInfos - 8, mouseX, mouseY, delta);
      int iy = y + h - basInfos;
      c.fill(x + 10, iy, x + w - 10, iy + 1, 419430399);
      String porte = this.selected == null ? "No cape" : this.libelleDe(this.selected);
      c.text(Kit.tronque(c, porte, w - 20), x + 10, iy + 8, -6644317, false);
      boolean anim = CosmeticState.selfAnimated();
      c.text("Animated", x + 10, iy + 26, -10394518, false);
      int sx = x + w - 10 - 30;
      int sy = iy + 23;
      Kit.interrupteur(c, sx, sy, anim);
      this.rectAnime = new int[]{sx - 4, sy - 4, 38, 23};
   }

   private void dessinerCarte(Canvas c, WardrobeScreen.Item it, int x, int y, int mouseX, int mouseY) {
      boolean choisi = it.value.equals(this.selected) || it.value.equals("__none__") && this.selected == null;
      boolean survol = Kit.dedans(mouseX, mouseY, x, y, 56, 80) && mouseY >= this.corpsHaut() && mouseY <= this.corpsBas();
      Kit.carte(c, x, y, 56, 80, choisi, survol);
      int px = x + 13;
      int py = y + 8;
      Object frame = it.value.equals("__none__") ? null : CosmeticState.currentFrame(it.value);
      if (frame != null) {
         int[] m = CosmeticState.metaFor(it.value);
         int fw = m[2];
         int fh = m[3];
         c.textureRegion(frame, px, py, 30, 48, fw / 64, fh / 32, fw * 10 / 64, fh * 16 / 32, fw, fh);
      } else {
         c.card(px, py, 30, 48, 352321535, 0, 0, 3.0F);
      }

      c.centeredText(Kit.tronque(c, it.label, 48), x + 28, y + 80 - 16, !choisi && !survol ? -6644317 : -1, false);
   }

   private String libelleDe(String value) {
      for (WardrobeScreen.Item i : this.items) {
         if (value.equals(i.value)) {
            return i.label;
         }
      }

      return value;
   }

   private int petY(int i) {
      return this.corpsHaut() + i * 40;
   }

   private int[] rectAction(int y) {
      return new int[]{this.grilleX() + this.grilleW() - 96, y + 9, 88, 16};
   }

   private void dessinerPets(Canvas c, int mouseX, int mouseY) {
      this.dessinerPet(c, 0, "None", this.equippedPet == null ? "Equipped" : "Remove", this.equippedPet == null, mouseX, mouseY);

      for (int i = 0; i < this.petItems.size(); i++) {
         WardrobeScreen.PetItem p = this.petItems.get(i);
         boolean eq = p.id.equals(this.equippedPet);
         String action;
         if (p.owned) {
            action = eq ? "Equipped" : "Equip";
         } else if (p.id.equals(this.pendingBuy)) {
            action = "Confirm - " + p.price;
         } else {
            action = p.price + " coins";
         }

         this.dessinerPet(c, i + 1, p.name, action, eq, mouseX, mouseY);
      }

      if (this.petItems.isEmpty()) {
         c.text("No pet in the catalog yet.", this.grilleX(), this.petY(1) + 6, -10394518, false);
      }
   }

   private void dessinerPet(Canvas c, int i, String nom, String action, boolean equipe, int mouseX, int mouseY) {
      int x = this.grilleX();
      int y = this.petY(i);
      int w = this.grilleW();
      boolean survol = Kit.dedans(mouseX, mouseY, x, y, w, 34);
      Kit.ligne(c, x, y, w, 34, equipe, survol);
      c.text(Kit.tronque(c, nom, w - 120), x + 12, y + 13, !equipe && !survol ? -6644317 : -1, false);
      int[] r = this.rectAction(y);
      if (equipe) {
         c.centeredText(action, r[0] + r[2] / 2, r[1] + 4, -10394518, false);
      } else {
         Kit.bouton(c, r, action, Kit.dedans(mouseX, mouseY, r));
      }
   }

   @Override
   public boolean click(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return false;
      } else if (this.apercuW() > 0 && Kit.dedans(mouseX, mouseY, this.rectAnime)) {
         CosmeticState.setSelfAnimated(!CosmeticState.selfAnimated());
         Platform.game().playClick();
         return true;
      } else {
         if ("capes".equals(ONGLETS[this.onglet].id())) {
            boolean surRecherche = Kit.dedans(mouseX, mouseY, Kit.rectRecherche(this.width, this.z));
            if (surRecherche != this.searchFocused) {
               this.searchFocused = surRecherche;
            }

            if (surRecherche) {
               return true;
            }
         }

         if (mouseY >= Kit.chipY() && mouseY < Kit.chipY() + 18) {
            int x = this.z.x();

            for (int i = 0; i < ONGLETS.length; i++) {
               WardrobeScreen.Onglet o = ONGLETS[i];
               int w = this.largeurOnglet(o);
               if (mouseX >= x && mouseX < x + w) {
                  if (o.pret() && this.onglet != i) {
                     this.onglet = i;
                     this.defil.haut();
                     Platform.game().playClick();
                  }

                  return true;
               }

               x += w + 6;
            }
         }

         if (!ONGLETS[this.onglet].pret() || !this.loaded) {
            return false;
         } else if (mouseY < this.corpsHaut() || mouseY > this.corpsBas()) {
            return false;
         } else if ("pets".equals(ONGLETS[this.onglet].id())) {
            return this.clicPet(mouseX, mouseY);
         } else {
            int base = this.corpsHaut() - this.defil.px();
            WardrobeScreen.Item[] cible = new WardrobeScreen.Item[1];
            this.parcourir((it, section, x, y) -> {
               if (it == null) {
                  return false;
               } else if (Kit.dedans(mouseX, mouseY, x, base + y, 56, 80)) {
                  cible[0] = it;
                  return true;
               } else {
                  return false;
               }
            });
            if (cible[0] != null) {
               this.equip(cible[0]);
               return true;
            } else {
               return false;
            }
         }
      }
   }

   private int largeurOnglet(WardrobeScreen.Onglet o) {
      Integer w = this.largeurs.get(o.id());
      return w == null ? 60 : w;
   }

   private boolean clicPet(double mouseX, double mouseY) {
      int n = this.petItems.size() + 1;

      for (int i = 0; i < n; i++) {
         int y = this.petY(i);
         if (Kit.dedans(mouseX, mouseY, this.grilleX(), y, this.grilleW(), 34)) {
            if (i == 0) {
               this.pendingBuy = null;
               this.equipPet(null);
               return true;
            }

            WardrobeScreen.PetItem p = this.petItems.get(i - 1);
            if (p.owned) {
               this.pendingBuy = null;
               this.equipPet(p.id);
            } else if (p.id.equals(this.pendingBuy)) {
               this.buyPet(p);
            } else {
               this.pendingBuy = p.id;
               Platform.game().playClick();
               this.pose("Click again to buy for " + p.price + " coins", false);
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
      } else {
         this.query = this.query + s;
         this.defil.haut();
         return true;
      }
   }

   @Override
   public boolean keyPressed(int keyCode) {
      if (!this.searchFocused) {
         return false;
      } else if (keyCode == 259 && !this.query.isEmpty()) {
         this.query = this.query.substring(0, this.query.length() - 1);
         this.defil.haut();
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
      return !this.searchFocused;
   }

   @Override
   public boolean scroll(double mouseX, double mouseY, double amount) {
      if (!ONGLETS[this.onglet].pret()) {
         return false;
      } else {
         this.defil.cran(amount, 44.0);
         return true;
      }
   }

   private void equip(WardrobeScreen.Item it) {
      Platform.game().playClick();
      boolean none = it.value.equals("__none__") || it.value.equals(this.selected);
      this.selected = none ? null : it.value;
      this.pose("Applying...", false);
      CosmeticState.applySelf(none ? "" : it.value);
      IO.execute(() -> {
         boolean ok;
         if (none) {
            ok = CosmeticHttp.equipCosmetic("none");
            CosmeticHttp.disableMojangCape();
            CosmeticHttp.declareMojangCape(null);
         } else if (it.extra != null) {
            CosmeticHttp.equipCosmetic("none");
            ok = CosmeticHttp.setMojangCapeActive(it.extra);
            if (ok) {
               CosmeticHttp.declareMojangCape(it.value.substring("mojang:".length()));
            }
         } else {
            ok = CosmeticHttp.equipCosmetic(it.value);
         }

         this.pose(ok ? "Applied" : "Failed - not applied", !ok);
      });
   }

   private void buyPet(WardrobeScreen.PetItem p) {
      Platform.game().playClick();
      this.pendingBuy = null;
      this.pose("Buying...", false);
      IO.execute(() -> {
         boolean ok = CosmeticHttp.buyCosmetic(p.id);
         if (ok) {
            p.owned = true;
            this.coins = CosmeticHttp.balance();
            this.pose("Bought - click to equip", false);
         } else {
            this.pose("Purchase refused (not enough coins?)", true);
         }
      });
   }

   private void equipPet(String id) {
      Platform.game().playClick();
      boolean none = id == null || id.equals(this.equippedPet);
      this.equippedPet = none ? null : id;
      PetState.applySelf(none ? "" : id);
      this.pose("Applying...", false);
      String target = none ? "none" : id;
      IO.execute(() -> {
         boolean ok = CosmeticHttp.equipPet(target);
         this.pose(ok ? "Applied" : "Failed - not applied", !ok);
      });
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

   private record Onglet(String id, String libelle, boolean pret) {
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

   private interface Visiteur {
      boolean cellule(WardrobeScreen.Item var1, String var2, int var3, int var4);
   }
}
