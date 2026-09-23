package dev.swiftclient.core.screen;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.ui.UiScreen;
import net.minecraft.client.resources.language.I18n;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public final class HudEditorScreen extends UiScreen {
   private static final int OVERLAY = -1442116334;
   private static final int BAR_BG = -1726998496;
   private static final int LINE = 419430399;
   private static final int CARD_BG = -15263718;
   private static final int CARD_HOV = -14605786;
   private static final int TEXT = -1;
   private static final int DIM = -6644317;
   private static final int FAINT = -10394518;
   private static final int ON = -12877066;
   private static final int SELECT = -12877066;
   private static final int WARN = -1531075;
   private static final int BAR_H = 24;
   private static final int BAR_Y = 8;
   private static final int GRID = 8;
   private static final int SNAP = 5;
   private static final int HANDLE = 6;
   private static final int K_RIGHT = 262;
   private static final int K_LEFT = 263;
   private static final int K_DOWN = 264;
   private static final int K_UP = 265;
   private static final int K_G = 71;
   private static final int K_S = 83;
   private static final int K_R = 82;
   private static final int K_Y = 89;
   private HudElement selected;
   private HudElement dragging;
   private HudElement resizing;
   private int grabDX;
   private int grabDY;
   private int resizeStartW;
   private float resizeStartScale;
   private boolean snap = true;
   private boolean grid = false;
   private boolean guides = true;
   private int guideX = Integer.MIN_VALUE;
   private int guideY = Integer.MIN_VALUE;
   private final Map<String, int[]> boxes = new HashMap<>();
   private final List<HudEditorScreen.Btn> btns = new ArrayList<>();
   private List<HudEditorScreen.Btn> menu;
   private int menuX;
   private int menuY;
   private int menuW;
   private HudElement pendingMenu;
   private int pendingX;
   private int pendingY;

   @Override
   public String title() {
      return I18n.get("swift.menu.hud");
   }

   private int[] size(Canvas c, HudElement e, HudData d, boolean live) {
      float s = HudManager.scale(e.id) * HudManager.facteurEcran();
      int[] raw;
      if (!live && !e.drawsWithoutData()) {
         raw = e.previewSize(c);
      } else {
         raw = new int[]{e.width(c, d), e.height(c, d)};
      }

      if (raw[0] <= 0 || raw[1] <= 0) {
         raw = e.previewSize(c);
      }

      return new int[]{Math.round(raw[0] * s), Math.round(raw[1] * s)};
   }

   private boolean overlaps(int[] a, int[] b) {
      return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
   }

   private boolean overChrome(double mx, double my) {
      for (HudEditorScreen.Btn b : this.btns) {
         if (b.hit(mx, my)) {
            return true;
         }
      }

      return this.menu != null && mx >= this.menuX && mx < this.menuX + this.menuW && my >= this.menuY && my < this.menuY + this.menu.size() * 14 + 6;
   }

   private boolean overHandle(double mx, double my) {
      if (this.selected == null) {
         return false;
      } else {
         int[] b = this.boxes.get(this.selected.id);
         if (b == null) {
            return false;
         } else {
            int hx = b[0] + b[2] - 3;
            int hy = b[1] + b[3] - 3;
            return mx >= hx && mx < hx + 6 + 2 && my >= hy && my < hy + 6 + 2;
         }
      }
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      c.fill(0, 0, this.width, this.height, -1442116334);
      if (this.grid) {
         this.drawGrid(c);
      }

      HudData d = HudManager.lastData();
      boolean live = d != null && d.inWorld();
      this.boxes.clear();
      Map<String, int[]> sizes = new HashMap<>();

      for (HudElement e : HudManager.elements()) {
         boolean on = ModuleManager.active(e.moduleId());
         sizes.put(e.id, this.size(c, e, d, live && on));
      }

      HudManager.layoutDefaults(this.width, this.height, id -> sizes.get(id)[1]);

      for (HudElement e : HudManager.elements()) {
         int[] wh = sizes.get(e.id);
         int[] xy = HudManager.resolve(e.id, wh[0], wh[1], this.width, this.height);
         this.boxes.put(e.id, new int[]{xy[0], xy[1], wh[0], wh[1]});
      }

      int[] selBox = this.selected != null ? this.boxes.get(this.selected.id) : null;

      for (HudElement e : HudManager.elements()) {
         boolean on = ModuleManager.active(e.moduleId());
         int[] b = this.boxes.get(e.id);
         float s = HudManager.scale(e.id) * HudManager.facteurEcran();
         boolean hover = !this.overChrome(mouseX, mouseY) && mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3];
         boolean clash = selBox != null && this.selected != e && this.overlaps(selBox, b);
         c.pushScale(b[0], b[1], s);
         if ((!live || !on) && !e.drawsWithoutData()) {
            int bw = Math.round(b[2] / s);
            int bh = Math.round(b[3] / s);
            c.card(0, 0, bw, bh, on ? -1728053248 : 1711276032, 587202559, 1, 4.0F);
            int yy = 4;

            for (String str : e.previewLines()) {
               c.text(str, 6, yy, on ? -1 : -8748401, true);
               yy += c.lineHeight() + 2;
            }
         } else {
            e.draw(c, d, 0, 0);
         }

         c.popScale();
         if (e == this.selected) {
            this.brackets(c, b, SELECT);
         } else if (clash) {
            this.outline(c, b, WARN);
         } else if (hover) {
            this.outline(c, b, on ? 1442840575 : 1439456091);
         }

         if (hover || e == this.selected) {
            String tag = e.label + (on ? "" : "  ·  off");
            int tagW = c.textWidth(tag);
            int tagX = Math.max(2, Math.min(b[0], this.width - tagW - 2));
            c.text(tag, tagX, Math.max(2, b[1] - 10), e == this.selected ? SELECT : DIM, true);
         }
      }

      if (selBox != null) {
         this.handle(c, selBox, this.overHandle(mouseX, mouseY) || this.resizing != null);
         if (this.dragging != null || this.resizing != null) {
            this.readout(c, selBox);
         }
      }

      if (this.dragging != null && this.guides) {
         if (this.guideX != Integer.MIN_VALUE) {
            c.fill(this.guideX, 0, this.guideX + 1, this.height, SELECT);
         }

         if (this.guideY != Integer.MIN_VALUE) {
            c.fill(0, this.guideY, this.width, this.guideY + 1, SELECT);
         }
      }

      this.drawBar(c, mouseX, mouseY);
      if (this.pendingMenu != null) {
         this.openMenu(this.pendingMenu, this.pendingX, this.pendingY, c);
         this.pendingMenu = null;
      }

      this.drawMenu(c, mouseX, mouseY);
      String hint = this.selected == null
         ? I18n.get("swift.hud.hint_idle")
         : I18n.get("swift.hud.hint_sel");
      // Offset hints so they sit clear of the left rail
      int hintCx = (this.width + 118) / 2;
      if (c.textWidth(hint) <= this.width - 130) {
         c.centeredText(hint, hintCx, this.height - 14, FAINT, false);
      }

      if (!live) {
         c.centeredText(I18n.get("swift.hud.preview"), hintCx, this.height - 26, FAINT, false);
      }
   }

   private void outline(Canvas c, int[] b, int argb) {
      c.card(b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, 0, argb, 1, 5.0F);
   }

   private void brackets(Canvas c, int[] b, int argb) {
      int x = b[0] - 1;
      int y = b[1] - 1;
      int w = b[2] + 2;
      int h = b[3] + 2;
      int len = Math.max(3, Math.min(8, Math.min(w, h) / 3));
      c.fill(x, y, x + len, y + 1, argb);
      c.fill(x, y, x + 1, y + len, argb);
      c.fill(x + w - len, y, x + w, y + 1, argb);
      c.fill(x + w - 1, y, x + w, y + len, argb);
      c.fill(x, y + h - 1, x + len, y + h, argb);
      c.fill(x, y + h - len, x + 1, y + h, argb);
      c.fill(x + w - len, y + h - 1, x + w, y + h, argb);
      c.fill(x + w - 1, y + h - len, x + w, y + h, argb);
   }

   private void handle(Canvas c, int[] b, boolean hot) {
      int hx = b[0] + b[2] - 3;
      int hy = b[1] + b[3] - 3;
      c.card(hx, hy, 6, 6, hot ? SELECT : -16052974, SELECT, 1, 2.0F);
   }

   private void readout(Canvas c, int[] b) {
      String s = String.format(Locale.ROOT, "%d, %d  ·  %d%%", b[0], b[1], Math.round(HudManager.scale(this.selected.id) * 100.0F));
      int w = c.textWidth(s) + 8;
      int h = c.lineHeight() + 4;
      int x = Math.max(2, Math.min(b[0], this.width - w - 2));
      int y = b[1] - h - 12 >= 32 ? b[1] - h - 12 : b[1] + b[3] + 3;
      c.card(x, y, w, h, -436207616, SELECT, 1, 4.0F);
      c.text(s, x + 4, y + 2, -1, false);
   }

   private void drawGrid(Canvas c) {
      for (int x = 8; x < this.width; x += 8) {
         c.fill(x, 0, x + 1, this.height, 218103807);
      }

      for (int y = 8; y < this.height; y += 8) {
         c.fill(0, y, this.width, y + 1, 218103807);
      }

      c.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 587202559);
      c.fill(0, this.height / 2, this.width, this.height / 2 + 1, 587202559);
   }

   private void drawBar(Canvas c, int mouseX, int mouseY) {
      this.btns.clear();
      // Left text rail — matches Swift title/sidebar DA (not LightClient top pill bar)
      int railW = 108;
      int x0 = 10;
      int y0 = 12;
      c.card(x0, y0, railW, this.height - 24, BAR_BG, 352321535, 1, 10.0F);
      c.fill(x0 + 1, y0 + 10, x0 + 3, y0 + 42, SELECT);

      String titre = I18n.get("swift.hud.bar_title");
      c.text(titre, x0 + 12, y0 + 14, SELECT, false);
      c.text(I18n.get("swift.hud.bar_sub"), x0 + 12, y0 + 28, FAINT, false);

      int row = y0 + 48;
      int rowH = 22;
      String snapL = I18n.get("swift.hud.snap");
      String gridL = I18n.get("swift.hud.grid");
      String guidesL = I18n.get("swift.hud.guides");
      String resetL = I18n.get("swift.hud.reset_all");
      this.btns.add(new HudEditorScreen.Btn(x0 + 8, row, railW - 16, rowH, snapL, this.snap, () -> this.snap = !this.snap));
      row += rowH + 4;
      this.btns.add(new HudEditorScreen.Btn(x0 + 8, row, railW - 16, rowH, gridL, this.grid, () -> this.grid = !this.grid));
      row += rowH + 4;
      this.btns.add(new HudEditorScreen.Btn(x0 + 8, row, railW - 16, rowH, guidesL, this.guides, () -> this.guides = !this.guides));
      row += rowH + 14;

      String glob = String.format(Locale.ROOT, "×%.1f", HudManager.hudScale());
      c.text(I18n.get("swift.hud.scale"), x0 + 12, row, FAINT, false);
      row += 14;
      this.btns.add(new HudEditorScreen.Btn(x0 + 8, row, 28, rowH, "−", false, () -> HudManager.addHudScale(-0.25F)));
      c.centeredText(glob, x0 + railW / 2, row + 6, TEXT, false);
      this.btns.add(new HudEditorScreen.Btn(x0 + railW - 36, row, 28, rowH, "+", false, () -> HudManager.addHudScale(0.25F)));
      row += rowH + 16;
      this.btns.add(new HudEditorScreen.Btn(x0 + 8, row, railW - 16, rowH, resetL, false, HudManager::resetAll));
      this.paint(c, this.btns, mouseX, mouseY);
   }

   private void paint(Canvas c, List<HudEditorScreen.Btn> list, int mouseX, int mouseY) {
      for (HudEditorScreen.Btn b : list) {
         boolean hov = b.hit(mouseX, mouseY);
         boolean compact = b.w() <= 28 || b.label().equals("−") || b.label().equals("+");
         if (compact) {
            int bg = b.on() ? SELECT : (hov ? -1441125824 : -1728053248);
            int border = b.on() || hov ? SELECT : 419430399;
            c.card(b.x(), b.y(), b.w(), b.h(), bg, border, 1, 5.0F);
            c.centeredText(b.label(), b.x() + b.w() / 2, b.y() + (b.h() - 8) / 2 + 1, b.on() || hov ? TEXT : DIM, false);
         } else {
            if (b.on()) {
               c.fill(b.x(), b.y() + 4, b.x() + 2, b.y() + b.h() - 4, SELECT);
            } else if (hov) {
               c.fill(b.x(), b.y() + 4, b.x() + 2, b.y() + b.h() - 4, 872415231);
            }
            c.text(b.label(), b.x() + 10, b.y() + (b.h() - 8) / 2, b.on() || hov ? TEXT : DIM, false);
         }
      }
   }

   private void openMenu(HudElement e, int mx, int my, Canvas c) {
      this.selected = e;
      Module m = ModuleManager.byId(e.moduleId());
      boolean on = ModuleManager.active(e.moduleId());
      int[] a = HudManager.anchor(e.id);
      List<String> labels = List.of(
         on ? I18n.get("swift.hud.enabled") : I18n.get("swift.hud.disabled"),
         I18n.get("swift.hud.center_h"),
         I18n.get("swift.hud.center_v"),
         I18n.get("swift.hud.reset_one")
      );
      int w = 0;

      for (String s : labels) {
         w = Math.max(w, c.textWidth(s));
      }

      w = Math.max(w + 16, c.textWidth(I18n.get("swift.hud.anchor")) + 8 + 108 + 10);
      this.menuW = w;
      this.menuX = Math.min(mx, this.width - w - 4);
      this.menuY = Math.min(my, this.height - 78 - 4);
      this.menu = new ArrayList<>();
      int y = this.menuY + 3;
      this.menu.add(new HudEditorScreen.Btn(this.menuX + 3, y, w - 6, 13, labels.get(0), on, () -> {
         if (m != null) {
            m.toggle();
         }
      }));
      y += 14;
      this.menu.add(new HudEditorScreen.Btn(this.menuX + 3, y, w - 6, 13, labels.get(1), false, () -> this.center(true)));
      y += 14;
      this.menu.add(new HudEditorScreen.Btn(this.menuX + 3, y, w - 6, 13, labels.get(2), false, () -> this.center(false)));
      y += 14;
      this.menu.add(new HudEditorScreen.Btn(this.menuX + 3, y, w - 6, 13, labels.get(3), false, () -> HudManager.reset(e.id)));
      y += 14;
      int ax = this.menuX + w - 3 - 116;
      String[] lx = new String[]{"L", "C", "R"};
      String[] ly = new String[]{"T", "M", "B"};

      for (int i = 0; i < 3; i++) {
         int v = i;
         this.menu.add(new HudEditorScreen.Btn(ax + i * 18, y, 16, 13, lx[i], a[0] == i, () -> this.setAnchorX(v)));
         this.menu.add(new HudEditorScreen.Btn(ax + 8 + (3 + i) * 18, y, 16, 13, ly[i], a[1] == i, () -> this.setAnchorY(v)));
      }
   }

   private void drawMenu(Canvas c, int mouseX, int mouseY) {
      if (this.menu != null) {
         int rows = 5;
         c.card(this.menuX, this.menuY, this.menuW, rows * 14 + 6, BAR_BG, 419430399, 1, 8.0F);
         c.text(I18n.get("swift.hud.anchor"), this.menuX + 5, this.menuY + 3 + 56 + 3, -10394518, false);
         this.paint(c, this.menu, mouseX, mouseY);
      }
   }

   private void center(boolean horizontal) {
      if (this.selected != null) {
         int[] b = this.boxes.get(this.selected.id);
         if (b != null) {
            int nx = horizontal ? (this.width - b[2]) / 2 : b[0];
            int ny = horizontal ? b[1] : (this.height - b[3]) / 2;
            HudManager.setTopLeft(this.selected.id, nx, ny, b[2], b[3], this.width, this.height);
            int[] a = HudManager.anchor(this.selected.id);
            HudManager.setAnchor(this.selected.id, horizontal ? 1 : a[0], horizontal ? a[1] : 1, b[2], b[3], this.width, this.height);
         }
      }
   }

   private void setAnchorX(int v) {
      int[] b = this.boxes.get(this.selected.id);
      if (b != null) {
         HudManager.setAnchor(this.selected.id, v, HudManager.anchor(this.selected.id)[1], b[2], b[3], this.width, this.height);
      }
   }

   private void setAnchorY(int v) {
      int[] b = this.boxes.get(this.selected.id);
      if (b != null) {
         HudManager.setAnchor(this.selected.id, HudManager.anchor(this.selected.id)[0], v, b[2], b[3], this.width, this.height);
      }
   }

   private HudElement pick(double mx, double my) {
      List<HudElement> els = HudManager.elements();

      for (int i = els.size() - 1; i >= 0; i--) {
         HudElement e = els.get(i);
         int[] b = this.boxes.get(e.id);
         if (b != null && mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) {
            return e;
         }
      }

      return null;
   }

   @Override
   public boolean click(double mx, double my, int button) {
      if (this.menu != null) {
         for (HudEditorScreen.Btn b : this.menu) {
            if (b.hit(mx, my)) {
               b.action().run();
               HudManager.save();
               Platform.game().playClick();
               this.menu = null;
               if (b.w() <= 16) {
                  this.pendingMenu = this.selected;
                  this.pendingX = this.menuX;
                  this.pendingY = this.menuY;
               }

               return true;
            }
         }

         this.menu = null;
         return true;
      } else {
         for (HudEditorScreen.Btn bx : this.btns) {
            if (bx.hit(mx, my)) {
               bx.action().run();
               HudManager.save();
               Platform.game().playClick();
               return true;
            }
         }

         if (button == 1) {
            HudElement e = this.pick(mx, my);
            if (e != null) {
               this.pendingMenu = e;
               this.pendingX = (int)mx;
               this.pendingY = (int)my;
            }

            return true;
         } else if (button != 0) {
            return true;
         } else if (this.overHandle(mx, my)) {
            int[] bxx = this.boxes.get(this.selected.id);
            this.resizing = this.selected;
            this.resizeStartW = Math.max(1, bxx[2]);
            this.resizeStartScale = HudManager.scale(this.selected.id);
            return true;
         } else {
            HudElement e = this.pick(mx, my);
            this.selected = e;
            if (e != null) {
               int[] bxx = this.boxes.get(e.id);
               this.dragging = e;
               this.grabDX = (int)mx - bxx[0];
               this.grabDY = (int)my - bxx[1];
            }

            return true;
         }
      }
   }

   @Override
   public void layout(int width, int height) {
      super.layout(width, height);
      this.menu = null;
      this.pendingMenu = null;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button) {
      if (this.resizing != null) {
         int[] b = this.boxes.get(this.resizing.id);
         if (b == null) {
            return false;
         } else {
            float f = (float)(mouseX - b[0]) / this.resizeStartW;
            HudManager.setScale(this.resizing.id, this.resizeStartScale * Math.max(0.1F, f));
            return true;
         }
      } else if (this.dragging == null) {
         return false;
      } else {
         int[] b = this.boxes.get(this.dragging.id);
         if (b == null) {
            return false;
         } else {
            int w = b[2];
            int h = b[3];
            int nx = (int)mouseX - this.grabDX;
            int ny = (int)mouseY - this.grabDY;
            this.guideX = this.guideY = Integer.MIN_VALUE;
            if (this.snap) {
               int[] sx = this.snapAxis(nx, w, this.targets(true));
               int[] sy = this.snapAxis(ny, h, this.targets(false));
               nx = sx[0];
               this.guideX = sx[1];
               ny = sy[0];
               this.guideY = sy[1];
            }

            if (this.grid && this.guideX == Integer.MIN_VALUE) {
               nx = Math.round(nx / 8.0F) * 8;
            }

            if (this.grid && this.guideY == Integer.MIN_VALUE) {
               ny = Math.round(ny / 8.0F) * 8;
            }

            nx = Math.max(2, Math.min(nx, this.width - w - 2));
            ny = Math.max(2, Math.min(ny, this.height - h - 2));
            HudManager.setTopLeft(this.dragging.id, nx, ny, w, h, this.width, this.height);
            return true;
         }
      }
   }

   private List<List<Integer>> targets(boolean xAxis) {
      List<Integer> starts = new ArrayList<>();
      List<Integer> centers = new ArrayList<>();
      List<Integer> ends = new ArrayList<>();
      int screen = xAxis ? this.width : this.height;
      starts.add(2);
      centers.add(screen / 2);
      ends.add(screen - 2);

      for (Entry<String, int[]> en : this.boxes.entrySet()) {
         if (this.dragging == null || !en.getKey().equals(this.dragging.id)) {
            int[] b = en.getValue();
            int start = xAxis ? b[0] : b[1];
            int size = xAxis ? b[2] : b[3];
            starts.add(start);
            centers.add(start + size / 2);
            ends.add(start + size);
         }
      }

      return List.of(starts, centers, ends);
   }

   private int[] snapAxis(int start, int size, List<List<Integer>> targets) {
      List<Integer> starts = targets.get(0);
      List<Integer> centers = targets.get(1);
      List<Integer> ends = targets.get(2);
      int bestDelta = 0;
      int guide = Integer.MIN_VALUE;
      int bestAbs = 6;
      int[][] pairs = new int[][]{{start, 0}, {start, 2}, {start + size / 2, 1}, {start + size, 2}, {start + size, 0}};

      for (int[] pair : pairs) {
         for (int t : pair[1] == 0 ? starts : (pair[1] == 1 ? centers : ends)) {
            int delta = t - pair[0];
            int abs = Math.abs(delta);
            if (abs < bestAbs) {
               bestAbs = abs;
               bestDelta = delta;
               guide = t;
            }
         }
      }

      return bestAbs > 5 ? new int[]{start, Integer.MIN_VALUE} : new int[]{start + bestDelta, guide};
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.resizing != null) {
         this.resizing = null;
         HudManager.save();
         return true;
      } else if (this.dragging == null) {
         return false;
      } else {
         int[] b = this.boxes.get(this.dragging.id);
         if (b != null) {
            HudManager.autoAnchor(this.dragging.id, b[2], b[3], this.width, this.height);
         }

         HudManager.save();
         this.dragging = null;
         this.guideX = this.guideY = Integer.MIN_VALUE;
         return true;
      }
   }

   @Override
   public boolean scroll(double mouseX, double mouseY, double amount) {
      HudElement e = this.pick(mouseX, mouseY);
      if (e == null) {
         e = this.selected;
      }

      if (e == null) {
         return false;
      } else {
         this.selected = e;
         HudManager.addScale(e.id, (float)(amount * 0.05));
         return true;
      }
   }

   @Override
   public boolean keyPressed(int keyCode) {
      if (this.menu != null && keyCode != 263 && keyCode != 262 && keyCode != 265 && keyCode != 264) {
         this.menu = null;
      }

      switch (keyCode) {
         case 71:
            this.grid = !this.grid;
            return true;
         case 82:
            if (this.selected == null) {
               return false;
            }

            HudManager.reset(this.selected.id);
            return true;
         case 83:
            this.snap = !this.snap;
            return true;
         case 89:
            this.guides = !this.guides;
            return true;
         case 262:
            return this.nudge(1, 0);
         case 263:
            return this.nudge(-1, 0);
         case 264:
            return this.nudge(0, 1);
         case 265:
            return this.nudge(0, -1);
         default:
            return false;
      }
   }

   private boolean nudge(int dx, int dy) {
      if (this.selected == null) {
         return false;
      } else {
         int[] b = this.boxes.get(this.selected.id);
         if (b == null) {
            return false;
         } else {
            int nx = Math.max(2, Math.min(b[0] + dx, this.width - b[2] - 2));
            int ny = Math.max(2, Math.min(b[1] + dy, this.height - b[3] - 2));
            HudManager.setTopLeft(this.selected.id, nx, ny, b[2], b[3], this.width, this.height);
            return true;
         }
      }
   }

   @Override
   public boolean closeOnEscape() {
      if (this.menu != null) {
         this.menu = null;
         return false;
      } else {
         HudManager.save();
         return true;
      }
   }

   private record Btn(int x, int y, int w, int h, String label, boolean on, Runnable action) {
      boolean hit(double mx, double my) {
         return mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + this.h;
      }
   }
}
