package dev.swiftclient.core.screen;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.ui.Defilement;
import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.ui.UiScreen;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * HUD editor. The whole screen is the canvas: nothing permanent covers the game view.
 *
 * <ul>
 *   <li>A slim floating toolbar (snap, grid, guides, global scale, undo / redo, elements, reset, done) that fades
 *       while you drag.</li>
 *   <li>Click to select, Shift+click to add, drag on empty space to box-select, drag to move the selection
 *       (snaps to edges and centres, hold Ctrl to move freely), corner handles to resize, arrows to nudge
 *       (Shift = 8 px), wheel to scale, Ctrl+Z / Ctrl+Y to undo / redo.</li>
 *   <li>An inspector card appears next to the selected element: on/off, scale, anchor, centre, reset.</li>
 *   <li>An elements drawer ({@code E}) lists everything, including modules that are off, so any of them can be found and enabled.</li>
 * </ul>
 */
public final class HudEditorScreen extends UiScreen {
   private static final int ACCENT = 0xFF3B82F6;
   private static final int ACCENT_HI = 0xFF6FA8FF;
   private static final int WARN = 0xFFF5B544;
   private static final int TEXT = 0xFFFFFFFF;
   private static final int DIM = 0xFF9AA6BA;
   private static final int FAINT = 0xFF5F6B80;
   private static final int PANEL = 0xF0090C12;
   private static final int PANEL_EDGE = 0xFF222B42;
   private static final int OK = 0xFF3ECF8E;

   private static final int SNAP_DIST = 5;
   private static final int GRID = 8;
   private static final int HANDLE = 6;
   private static final int TOOL_H = 22;
   private static final int TOOLBAR_Y = 6;
   private static final int DRAWER_W = 150;
   private static final int ROW_H = 19;
   private static final int INSPECTOR_W = 156;
   private static final int INSPECTOR_H = 94;

   private static final int K_RIGHT = 262;
   private static final int K_LEFT = 263;
   private static final int K_DOWN = 264;
   private static final int K_UP = 265;
   private static final int K_DELETE = 261;

   private enum Mode {
      NONE, MOVE, RESIZE, MARQUEE
   }

   // Selection and view options
   private final Set<String> sel = new LinkedHashSet<>();
   private String primary;
   private boolean snap = true;
   private boolean grid = false;
   private boolean guides = true;

   // Frame data
   private final Map<String, int[]> boxes = new HashMap<>();
   private boolean live;

   // Gesture
   private Mode mode = Mode.NONE;
   private int downX;
   private int downY;
   private boolean moved;
   private final Map<String, int[]> startBoxes = new HashMap<>();
   private String collapseTo;
   private String resizeId;
   private int resizeCorner;
   private float resizeStartScale;
   private int[] resizeStartBox;
   private int curX;
   private int curY;
   private int guideX = Integer.MIN_VALUE;
   private int guideY = Integer.MIN_VALUE;
   private Map<String, String> base;

   // History
   private final Deque<Map<String, String>> undo = new ArrayDeque<>();
   private final Deque<Map<String, String>> redo = new ArrayDeque<>();

   // Toolbar, drawer, inspector
   private final List<Tool> tools = new ArrayList<>();
   private String tipLabel;
   private int tipX;
   private boolean drawerOpen;
   private float drawerT;
   private final Defilement drawerScroll = new Defilement();
   private int[] inspector;
   private final List<Hit> inspectorHits = new ArrayList<>();
   private float chromeFade = 1.0F;
   private long lastFrame = System.nanoTime();
   private final long born = System.nanoTime();

   private record Tool(String id, String icon, String label, String key, boolean on, boolean enabled, boolean primary, int[] r, Runnable action) {
   }

   private record Hit(int[] r, Runnable action) {
   }

   @Override
   public String title() {
      return Tr.of("swift.menu.hud");
   }

   @Override
   public void layout(int width, int height) {
      super.layout(width, height);
      this.base = null;
      this.mode = Mode.NONE;
   }

   // ---------------------------------------------------------------------------------------------
   // Frame
   // ---------------------------------------------------------------------------------------------

   private int[] size(Canvas c, HudElement e, HudData d, boolean liveOn) {
      float s = HudManager.scale(e.id) * HudManager.facteurEcran();
      int[] raw;
      if (!liveOn && !e.drawsWithoutData()) {
         raw = e.previewSize(c);
      } else {
         raw = new int[]{e.width(c, d), e.height(c, d)};
      }

      if (raw[0] <= 0 || raw[1] <= 0) {
         raw = e.previewSize(c);
      }

      return new int[]{Math.round(raw[0] * s), Math.round(raw[1] * s)};
   }

   private boolean on(HudElement e) {
      return ModuleManager.active(e.moduleId());
   }

   private static boolean in(double mx, double my, int[] r) {
      return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
   }

   private static boolean overlaps(int[] a, int[] b) {
      return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
   }

   private HudElement element(String id) {
      for (HudElement e : HudManager.elements()) {
         if (e.id.equals(id)) {
            return e;
         }
      }

      return null;
   }

   private void computeBoxes(Canvas c, HudData d) {
      this.boxes.clear();
      Map<String, int[]> sizes = new HashMap<>();
      for (HudElement e : HudManager.elements()) {
         sizes.put(e.id, this.size(c, e, d, this.live && this.on(e)));
      }

      HudManager.layoutDefaults(this.width, this.height, id -> sizes.get(id)[1]);
      for (HudElement e : HudManager.elements()) {
         int[] wh = sizes.get(e.id);
         int[] xy = HudManager.resolve(e.id, wh[0], wh[1], this.width, this.height);
         this.boxes.put(e.id, new int[]{xy[0], xy[1], wh[0], wh[1]});
      }
   }

   private boolean chromeBlocked(double mx, double my) {
      if (this.mode != Mode.NONE) {
         return false;
      }

      for (Tool t : this.tools) {
         if (in(mx, my, t.r())) {
            return true;
         }
      }

      if (this.drawerOpen && mx < this.drawerRight()) {
         return true;
      }

      return this.inspector != null && in(mx, my, this.inspector);
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      long now = System.nanoTime();
      float dt = Math.min(0.1F, (now - this.lastFrame) / 1.0E9F);
      this.lastFrame = now;
      this.curX = mouseX;
      this.curY = mouseY;
      this.drawerT += ((this.drawerOpen ? 1.0F : 0.0F) - this.drawerT) * (1.0F - (float)Math.exp(-18.0F * dt));
      if (Math.abs((this.drawerOpen ? 1.0F : 0.0F) - this.drawerT) < 0.004F) {
         this.drawerT = this.drawerOpen ? 1.0F : 0.0F;
      }

      boolean busy = this.mode == Mode.MOVE || this.mode == Mode.RESIZE || this.mode == Mode.MARQUEE;
      this.chromeFade += ((busy ? 0.28F : 1.0F) - this.chromeFade) * (1.0F - (float)Math.exp(-14.0F * dt));
      this.drawerScroll.anime();

      c.fill(0, 0, this.width, this.height, 0x70070A10);
      if (this.grid) {
         this.drawGrid(c);
      }

      HudData d = HudManager.lastData();
      this.live = d != null && d.inWorld();
      this.computeBoxes(c, d);
      this.pruneSelection();

      List<HudElement> els = HudManager.elements();
      String hover = busy || this.chromeBlocked(mouseX, mouseY) ? null : this.pickId(mouseX, mouseY);
      for (HudElement e : els) {
         this.drawElement(c, e, d, hover);
      }

      this.drawSelection(c);
      this.drawGuides(c);
      if (this.mode == Mode.MARQUEE) {
         int[] m = this.marqueeRect();
         c.fill(m[0], m[1], m[0] + m[2], m[1] + m[3], 0x203B82F6);
         frame(c, m[0], m[1], m[2], m[3], 0xCC3B82F6);
      }

      if (this.mode == Mode.MOVE || this.mode == Mode.RESIZE) {
         this.drawReadout(c);
      }

      this.tools.clear();
      this.drawToolbar(c, mouseX, mouseY);
      this.drawDrawer(c, mouseX, mouseY);
      this.drawInspector(c, mouseX, mouseY);
      this.drawTip(c);
      this.drawHint(c);
   }

   private void drawGrid(Canvas c) {
      for (int x = GRID; x < this.width; x += GRID) {
         c.fill(x, 0, x + 1, this.height, x % (GRID * 4) == 0 ? 0x16FFFFFF : 0x0AFFFFFF);
      }

      for (int y = GRID; y < this.height; y += GRID) {
         c.fill(0, y, this.width, y + 1, y % (GRID * 4) == 0 ? 0x16FFFFFF : 0x0AFFFFFF);
      }

      c.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x30FFFFFF);
      c.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x30FFFFFF);
   }

   private void drawElement(Canvas c, HudElement e, HudData d, String hover) {
      boolean on = this.on(e);
      int[] b = this.boxes.get(e.id);
      float s = HudManager.scale(e.id) * HudManager.facteurEcran();
      c.pushScale(b[0], b[1], s);
      if ((!this.live || !on) && !e.drawsWithoutData()) {
         int bw = Math.round(b[2] / s);
         int bh = Math.round(b[3] / s);
         c.card(0, 0, bw, bh, on ? 0x7A0A0D14 : 0x3A0A0D14, on ? 0x26FFFFFF : 0x22FFFFFF, 1, 6.0F);
         int yy = 4;
         for (String str : e.previewLines()) {
            c.text(str, 6, yy, on ? -1 : 0x88AAB4C6, on);
            yy += c.lineHeight() + 2;
         }
      } else {
         e.draw(c, d, 0, 0);
      }

      c.popScale();
      if (!on) {
         // Off modules are ghosts: dashed frame so they read as "not active" but stay findable
         this.dashed(c, b, 0x66AAB4C6);
      }

      boolean selected = this.sel.contains(e.id);
      if (!selected && e.id.equals(hover)) {
         frame(c, b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, on ? 0x99FFFFFF : 0x77FFFFFF);
         this.nameTag(c, e, b, false);
      }

      if (!selected && this.mode == Mode.MOVE && this.primary != null) {
         int[] pb = this.boxes.get(this.primary);
         if (pb != null && overlaps(pb, b) && !this.sel.contains(e.id)) {
            c.roundRect(b[0], b[1], b[2], b[3], 5.0F, 0x1AF5B544);
            frame(c, b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, WARN);
         }
      }
   }

   /**
    * A real outline: four thin lines. Canvas.card paints the border colour over the whole area first, so a card
    * with a see-through fill would hide whatever it is drawn around.
    */
   private static void frame(Canvas c, int x, int y, int w, int h, int argb) {
      c.fill(x, y, x + w, y + 1, argb);
      c.fill(x, y + h - 1, x + w, y + h, argb);
      c.fill(x, y + 1, x + 1, y + h - 1, argb);
      c.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
   }

   private void dashed(Canvas c, int[] b, int argb) {
      for (int x = b[0]; x < b[0] + b[2]; x += 6) {
         int w = Math.min(3, b[0] + b[2] - x);
         c.fill(x, b[1], x + w, b[1] + 1, argb);
         c.fill(x, b[1] + b[3] - 1, x + w, b[1] + b[3], argb);
      }

      for (int y = b[1]; y < b[1] + b[3]; y += 6) {
         int h = Math.min(3, b[1] + b[3] - y);
         c.fill(b[0], y, b[0] + 1, y + h, argb);
         c.fill(b[0] + b[2] - 1, y, b[0] + b[2], y + h, argb);
      }
   }

   private void nameTag(Canvas c, HudElement e, int[] b, boolean selected) {
      String tag = this.on(e) ? e.label : Tr.of("swift.hud.off_tag", e.label);
      int w = c.textWidth(tag) + 10;
      int x = Math.max(2, Math.min(b[0], this.width - w - 2));
      int y = b[1] - 15 >= 2 ? b[1] - 15 : b[1] + b[3] + 3;
      c.card(x, y, w, 12, 0xE0090C12, selected ? ACCENT : 0xFF2A3350, 1, 3.0F);
      c.text(tag, x + 5, y + 2, selected ? ACCENT_HI : DIM, false);
   }

   private void drawSelection(Canvas c) {
      if (this.sel.isEmpty()) {
         return;
      }

      for (String id : this.sel) {
         int[] b = this.boxes.get(id);
         if (b == null) {
            continue;
         }

         boolean clash = false;
         for (String other : this.boxes.keySet()) {
            if (!other.equals(id) && !this.sel.contains(other) && this.on(this.element(other)) && overlaps(b, this.boxes.get(other))) {
               clash = true;
               break;
            }
         }

         c.roundRect(b[0], b[1], b[2], b[3], 5.0F, clash ? 0x16F5B544 : 0x163B82F6);
         frame(c, b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, clash ? WARN : ACCENT);
      }

      int[] g = this.groupBox();
      if (this.sel.size() > 1) {
         this.dashed(c, new int[]{g[0] - 3, g[1] - 3, g[2] + 6, g[3] + 6}, 0xAA6FA8FF);
      }

      if (this.primary != null && this.element(this.primary) != null && this.sel.size() == 1) {
         this.nameTag(c, this.element(this.primary), g, true);
         this.handles(c, g);
      } else if (this.sel.size() > 1) {
         String t = Tr.of("swift.hud.selected_n", this.sel.size());
         int w = c.textWidth(t) + 10;
         int x = Math.max(2, Math.min(g[0], this.width - w - 2));
         int y = g[1] - 18 >= 2 ? g[1] - 18 : g[1] + g[3] + 6;
         c.card(x, y, w, 12, 0xE0090C12, ACCENT, 1, 3.0F);
         c.text(t, x + 5, y + 2, ACCENT_HI, false);
      }
   }

   private int[][] handleRects(int[] b) {
      int h = HANDLE;
      return new int[][]{
         {b[0] - h / 2 - 1, b[1] - h / 2 - 1, h, h},
         {b[0] + b[2] - h / 2 + 1, b[1] - h / 2 - 1, h, h},
         {b[0] - h / 2 - 1, b[1] + b[3] - h / 2 + 1, h, h},
         {b[0] + b[2] - h / 2 + 1, b[1] + b[3] - h / 2 + 1, h, h},
      };
   }

   private void handles(Canvas c, int[] b) {
      int[][] hs = this.handleRects(b);
      for (int i = 0; i < 4; i++) {
         boolean hot = this.mode == Mode.RESIZE && this.resizeCorner == i || in(this.curX, this.curY, grow(hs[i], 3));
         c.card(hs[i][0], hs[i][1], hs[i][2], hs[i][3], hot ? ACCENT_HI : 0xFF0B0E15, ACCENT, 1, 2.0F);
      }
   }

   private static int[] grow(int[] r, int by) {
      return new int[]{r[0] - by, r[1] - by, r[2] + 2 * by, r[3] + 2 * by};
   }

   private int handleAt(double mx, double my) {
      if (this.sel.size() != 1 || this.primary == null) {
         return -1;
      }

      int[] b = this.boxes.get(this.primary);
      if (b == null) {
         return -1;
      }

      int[][] hs = this.handleRects(b);
      for (int i = 0; i < 4; i++) {
         if (in(mx, my, grow(hs[i], 3))) {
            return i;
         }
      }

      return -1;
   }

   private void drawGuides(Canvas c) {
      if (this.mode != Mode.MOVE || !this.guides) {
         return;
      }

      if (this.guideX != Integer.MIN_VALUE) {
         c.fill(this.guideX, 0, this.guideX + 1, this.height, 0xCC6FA8FF);
      }

      if (this.guideY != Integer.MIN_VALUE) {
         c.fill(0, this.guideY, this.width, this.guideY + 1, 0xCC6FA8FF);
      }
   }

   private void drawReadout(Canvas c) {
      int[] g = this.groupBox();
      String s;
      if (this.mode == Mode.RESIZE && this.resizeId != null) {
         s = String.format(Locale.ROOT, "%d%%", Math.round(HudManager.scale(this.resizeId) * 100.0F));
      } else {
         s = String.format(Locale.ROOT, "%d, %d", g[0], g[1]);
      }

      int w = c.textWidth(s) + 10;
      int x = Math.max(2, Math.min(g[0] + g[2] / 2 - w / 2, this.width - w - 2));
      int y = g[1] + g[3] + 6 + 12 < this.height ? g[1] + g[3] + 6 : g[1] - 18;
      c.card(x, y, w, 12, 0xF0090C12, ACCENT, 1, 3.0F);
      c.text(s, x + 5, y + 2, TEXT, false);
   }

   // ---------------------------------------------------------------------------------------------
   // Toolbar
   // ---------------------------------------------------------------------------------------------

   private static int fade(int argb, float k) {
      return Math.round((argb >>> 24) * Math.max(0.0F, Math.min(1.0F, k))) << 24 | argb & 0xFFFFFF;
   }

   private void addTool(List<Tool> list, String id, String icon, String label, String key, boolean on, boolean enabled, boolean primary, Runnable action) {
      list.add(new Tool(id, icon, label, key, on, enabled, primary, new int[]{0, 0, 24, TOOL_H}, action));
   }

   private void drawToolbar(Canvas c, int mouseX, int mouseY) {
      List<Tool> items = new ArrayList<>();
      this.addTool(items, "snap", "target", Tr.of("swift.hud.snap"), "S", this.snap, true, false, () -> this.snap = !this.snap);
      this.addTool(items, "grid", "grid", Tr.of("swift.hud.grid"), "G", this.grid, true, false, () -> this.grid = !this.grid);
      this.addTool(items, "guides", "ruler", Tr.of("swift.hud.guides"), "Y", this.guides, true, false, () -> this.guides = !this.guides);
      this.addTool(items, "sep", null, null, null, false, false, false, null);
      this.addTool(items, "scale-", "minus", Tr.of("swift.hud.scale"), null, false, HudManager.hudScale() > 1.0F, false, () -> this.globalScale(-0.25F));
      this.addTool(items, "scale-v", null, null, null, false, false, false, null);
      this.addTool(items, "scale+", "plus", Tr.of("swift.hud.scale"), null, false, HudManager.hudScale() < 4.0F, false, () -> this.globalScale(0.25F));
      this.addTool(items, "sep", null, null, null, false, false, false, null);
      this.addTool(items, "undo", "undo", Tr.of("swift.hud.undo"), "Ctrl+Z", false, !this.undo.isEmpty(), false, this::undo);
      this.addTool(items, "redo", "redo", Tr.of("swift.hud.redo"), "Ctrl+Y", false, !this.redo.isEmpty(), false, this::redo);
      this.addTool(items, "sep", null, null, null, false, false, false, null);
      this.addTool(items, "elements", "layers", Tr.of("swift.hud.elements"), "E", this.drawerOpen, true, false, () -> this.drawerOpen = !this.drawerOpen);
      this.addTool(items, "reset", "reload", Tr.of("swift.hud.reset_all"), null, false, true, false, this::resetAll);
      this.addTool(items, "done", "check", Tr.of("swift.common.done"), "Esc", false, true, true, this::finish);

      int gap = 3;
      int sepW = 7;
      int valueW = 30;
      int total = -gap;
      for (Tool t : items) {
         total += gap + (t.id().equals("sep") ? sepW : (t.id().equals("scale-v") ? valueW : 24));
      }

      int x = (this.width - total) / 2;
      int y = TOOLBAR_Y;
      c.card(x - 6, y - 3, total + 12, TOOL_H + 6, fade(PANEL, this.chromeFade), fade(PANEL_EDGE, this.chromeFade), 1, 8.0F);
      boolean interactive = this.chromeFade > 0.6F;
      this.tipLabel = null;
      for (Tool t : items) {
         int w = t.id().equals("sep") ? sepW : (t.id().equals("scale-v") ? valueW : 24);
         int[] r = {x, y, w, TOOL_H};
         if (t.id().equals("sep")) {
            c.fill(x + w / 2, y + 4, x + w / 2 + 1, y + TOOL_H - 4, fade(0x22FFFFFF, this.chromeFade));
         } else if (t.id().equals("scale-v")) {
            c.centeredText(String.format(Locale.ROOT, "%.2f", HudManager.hudScale()).replaceAll("0+$", "").replaceAll("[.]$", "") + "×", x + w / 2, y + 7, fade(TEXT, this.chromeFade), false);
         } else {
            boolean over = interactive && t.enabled() && in(mouseX, mouseY, r);
            int bg = t.primary() ? (over ? 0xFF5B9BFF : ACCENT) : (t.on() ? 0x333B82F6 : (over ? 0xFF1D2535 : 0x00000000));
            int border = t.on() ? ACCENT : (over && !t.primary() ? 0xFF3A4766 : 0);
            c.card(r[0], r[1], r[2], r[3], fade(bg, this.chromeFade), fade(border, this.chromeFade), border == 0 ? 0 : 1, 6.0F);
            int col = !t.enabled() ? 0xFF3A4152 : (t.primary() || t.on() || over ? TEXT : DIM);
            c.icon(t.icon(), r[0] + 6, r[1] + 5, 12, fade(col, this.chromeFade));
            if (over && t.label() != null) {
               this.tipLabel = t.key() == null ? t.label() : t.label() + "  ·  " + t.key();
               this.tipX = x + w / 2;
            }

            this.tools.add(new Tool(t.id(), t.icon(), t.label(), t.key(), t.on(), t.enabled(), t.primary(), r, t.action()));
         }

         x += w + gap;
      }
   }

   private void drawTip(Canvas c) {
      if (this.tipLabel == null) {
         return;
      }

      int w = c.textWidth(this.tipLabel) + 12;
      int x = Math.max(4, Math.min(this.tipX - w / 2, this.width - w - 4));
      int y = TOOLBAR_Y + TOOL_H + 8;
      c.card(x, y, w, 14, 0xF0000000, 0xFF3A4766, 1, 3.0F);
      c.text(this.tipLabel, x + 6, y + 3, TEXT, false);
   }

   private void globalScale(float delta) {
      this.gesture(() -> HudManager.addHudScale(delta));
   }

   private void resetAll() {
      this.gesture(HudManager::resetAll);
      this.sel.clear();
      this.primary = null;
   }

   private void finish() {
      HudManager.save();
      this.back();
   }

   // ---------------------------------------------------------------------------------------------
   // Elements drawer
   // ---------------------------------------------------------------------------------------------

   private int drawerRight() {
      return Math.round(this.drawerT * (DRAWER_W + 8));
   }

   private int[] drawerRect() {
      int x = Math.round((this.drawerT - 1.0F) * (DRAWER_W + 8)) + 8;
      return new int[]{x, TOOLBAR_Y + TOOL_H + 14, DRAWER_W, this.height - (TOOLBAR_Y + TOOL_H + 14) - 10};
   }

   private void drawDrawer(Canvas c, int mouseX, int mouseY) {
      if (this.drawerT <= 0.0F) {
         return;
      }

      int[] r = this.drawerRect();
      c.card(r[0], r[1], r[2], r[3], PANEL, PANEL_EDGE, 1, 8.0F);
      c.text(Tr.of("swift.hud.elements").toUpperCase(Locale.ROOT), r[0] + 10, r[1] + 8, TEXT, true);
      Px.streak(c, r[0] + 10, r[1] + 19);
      int top = r[1] + 28;
      int bottom = r[1] + r[3] - 6;
      List<HudElement> els = HudManager.elements();
      this.drawerScroll.contenu(els.size() * ROW_H, bottom - top);
      c.pushScissor(r[0] + 2, top, r[2] - 4, bottom - top);
      int y = top - this.drawerScroll.px();
      for (HudElement e : els) {
         if (y + ROW_H >= top && y <= bottom) {
            boolean on = this.on(e);
            boolean chosen = this.sel.contains(e.id);
            boolean over = in(mouseX, mouseY, new int[]{r[0] + 4, y, r[2] - 8, ROW_H - 1}) && mouseY >= top && mouseY <= bottom;
            if (chosen || over) {
               c.card(r[0] + 4, y, r[2] - 8, ROW_H - 2, chosen ? 0x333B82F6 : 0xFF161B27, chosen ? ACCENT : 0, chosen ? 1 : 0, 4.0F);
            }

            c.icon(e.icon() == null ? "gear" : e.icon(), r[0] + 9, y + 4, 10, on ? (chosen ? TEXT : DIM) : 0xFF3A4152);
            int nameMax = r[2] - 9 - 12 - 8 - 32;
            c.text(trunc(c, e.label, nameMax), r[0] + 24, y + 5, on ? (chosen || over ? TEXT : DIM) : FAINT, false);
            Px.toggle(c, r[0] + r[2] - 8 - 26, y + 3, on);
         }

         y += ROW_H;
      }

      c.popScissor();
   }

   private static String trunc(Canvas c, String s, int maxW) {
      if (c.textWidth(s) <= maxW) {
         return s;
      }

      while (s.length() > 1 && c.textWidth(s + "…") > maxW) {
         s = s.substring(0, s.length() - 1);
      }

      return s + "…";
   }

   private boolean clickDrawer(double mx, double my) {
      int[] r = this.drawerRect();
      if (!in(mx, my, r)) {
         return false;
      }

      int top = r[1] + 28;
      if (my < top) {
         return true;
      }

      int y = top - this.drawerScroll.px();
      for (HudElement e : HudManager.elements()) {
         if (my >= y && my < y + ROW_H) {
            if (mx >= r[0] + r[2] - 8 - 30) {
               Module m = ModuleManager.byId(e.moduleId());
               if (m != null) {
                  m.toggle();
               }
            } else {
               this.select(e.id, shift());
            }

            Platform.game().playClick();
            return true;
         }

         y += ROW_H;
      }

      return true;
   }

   // ---------------------------------------------------------------------------------------------
   // Inspector
   // ---------------------------------------------------------------------------------------------

   private void drawInspector(Canvas c, int mouseX, int mouseY) {
      this.inspector = null;
      this.inspectorHits.clear();
      if (this.sel.size() != 1 || this.primary == null || this.mode == Mode.MOVE || this.mode == Mode.RESIZE || this.mode == Mode.MARQUEE) {
         return;
      }

      HudElement e = this.element(this.primary);
      int[] b = this.boxes.get(this.primary);
      if (e == null || b == null) {
         return;
      }

      int w = INSPECTOR_W;
      int h = INSPECTOR_H;
      int x = Math.max(4 + this.drawerRight(), Math.min(b[0], this.width - w - 4));
      int y = b[1] + b[3] + 8;
      if (y + h > this.height - 4) {
         y = b[1] - h - 8;
      }

      if (y < TOOLBAR_Y + TOOL_H + 10) {
         // No room above or below: tuck it beside the element
         y = Math.max(TOOLBAR_Y + TOOL_H + 12, Math.min(b[1], this.height - h - 4));
         x = b[0] + b[2] + 8 + w <= this.width - 4 ? b[0] + b[2] + 8 : Math.max(4, b[0] - w - 8);
      }

      this.inspector = new int[]{x, y, w, h};
      c.card(x, y, w, h, PANEL, PANEL_EDGE, 1, 8.0F);
      boolean on = this.on(e);
      Module m = ModuleManager.byId(e.moduleId());

      // Title + on/off
      c.icon(e.icon() == null ? "gear" : e.icon(), x + 9, y + 8, 10, on ? TEXT : FAINT);
      c.text(trunc(c, e.label, w - 62), x + 24, y + 9, TEXT, true);
      int[] sw = {x + w - 9 - 26, y + 7, 26, 12};
      Px.toggle(c, sw[0], sw[1], on);
      this.inspectorHits.add(new Hit(grow(sw, 3), () -> {
         if (m != null) {
            m.toggle();
         }
      }));

      // Scale
      int ry = y + 28;
      c.text(Tr.of("swift.hud.scale"), x + 9, ry + 5, DIM, false);
      int[] minus = {x + w - 9 - 72, ry, 20, 18};
      int[] plus = {x + w - 9 - 20, ry, 20, 18};
      this.smallButton(c, minus, "minus", mouseX, mouseY);
      this.smallButton(c, plus, "plus", mouseX, mouseY);
      c.centeredText(Math.round(HudManager.scale(e.id) * 100.0F) + "%", x + w - 9 - 46 + 0, ry + 5, TEXT, false);
      this.inspectorHits.add(new Hit(minus, () -> this.gesture(() -> this.scaleBy(e, -0.1F))));
      this.inspectorHits.add(new Hit(plus, () -> this.gesture(() -> this.scaleBy(e, 0.1F))));

      // Anchor grid + actions
      int ay = y + 54;
      c.text(Tr.of("swift.hud.anchor"), x + 9, ay - 1, DIM, false);
      int[] a = HudManager.anchor(e.id);
      int gx = x + 9;
      int gy = ay + 9;
      for (int j = 0; j < 3; j++) {
         for (int i = 0; i < 3; i++) {
            int[] cell = {gx + i * 11, gy + j * 9, 9, 7};
            boolean active = a[0] == i && a[1] == j;
            boolean over = in(mouseX, mouseY, cell);
            c.card(cell[0], cell[1], cell[2], cell[3], active ? ACCENT : (over ? 0xFF2A3350 : 0xFF161B27), 0, 0, 2.0F);
            int ci = i;
            int cj = j;
            this.inspectorHits.add(new Hit(cell, () -> this.gesture(() -> this.setAnchor(e, ci, cj))));
         }
      }

      int bx = x + 52;
      int bw = w - 52 - 9;
      int bwEach = (bw - 8) / 3;
      this.actionButton(c, new int[]{bx, ay + 4, bwEach, 22}, "alignh", Tr.of("swift.hud.center_h"), mouseX, mouseY, () -> this.gesture(() -> this.center(true)));
      this.actionButton(c, new int[]{bx + bwEach + 4, ay + 4, bwEach, 22}, "alignv", Tr.of("swift.hud.center_v"), mouseX, mouseY, () -> this.gesture(() -> this.center(false)));
      this.actionButton(c, new int[]{bx + 2 * (bwEach + 4), ay + 4, bwEach, 22}, "reload", Tr.of("swift.hud.reset_one"), mouseX, mouseY, () -> this.gesture(() -> HudManager.reset(e.id)));
   }

   private void smallButton(Canvas c, int[] r, String icon, int mouseX, int mouseY) {
      boolean over = in(mouseX, mouseY, r);
      c.card(r[0], r[1], r[2], r[3], over ? 0xFF1D2535 : 0xFF11151F, over ? 0xFF3A4766 : 0, 1, 4.0F);
      c.icon(icon, r[0] + (r[2] - 10) / 2, r[1] + (r[3] - 10) / 2, 10, over ? TEXT : DIM);
   }

   private void actionButton(Canvas c, int[] r, String icon, String tip, int mouseX, int mouseY, Runnable action) {
      boolean over = in(mouseX, mouseY, r);
      c.card(r[0], r[1], r[2], r[3], over ? 0xFF1D2535 : 0xFF11151F, over ? 0xFF3A4766 : 0, 1, 4.0F);
      c.icon(icon, r[0] + (r[2] - 12) / 2, r[1] + 5, 12, over ? TEXT : DIM);
      if (over) {
         this.tipLabel = tip;
         this.tipX = r[0] + r[2] / 2;
      }

      this.inspectorHits.add(new Hit(r, action));
   }

   private void scaleBy(HudElement e, float delta) {
      int[] b = this.boxes.get(e.id);
      float old = HudManager.scale(e.id);
      HudManager.addScale(e.id, delta);
      float now = HudManager.scale(e.id);
      if (b != null && old > 0.0F && now != old) {
         // Keep the top-left corner where it is
         int nw = Math.round(b[2] * now / old);
         int nh = Math.round(b[3] * now / old);
         HudManager.setTopLeft(e.id, b[0], b[1], nw, nh, this.width, this.height);
      }
   }

   private void setAnchor(HudElement e, int ax, int ay) {
      int[] b = this.boxes.get(e.id);
      if (b != null) {
         HudManager.setAnchor(e.id, ax, ay, b[2], b[3], this.width, this.height);
      }
   }

   private void center(boolean horizontal) {
      for (String id : this.sel) {
         int[] b = this.boxes.get(id);
         if (b == null) {
            continue;
         }

         int nx = horizontal ? (this.width - b[2]) / 2 : b[0];
         int ny = horizontal ? b[1] : (this.height - b[3]) / 2;
         HudManager.setTopLeft(id, nx, ny, b[2], b[3], this.width, this.height);
         int[] a = HudManager.anchor(id);
         HudManager.setAnchor(id, horizontal ? 1 : a[0], horizontal ? a[1] : 1, b[2], b[3], this.width, this.height);
      }
   }

   // ---------------------------------------------------------------------------------------------
   // Hint
   // ---------------------------------------------------------------------------------------------

   private void drawHint(Canvas c) {
      String hint = this.sel.isEmpty() ? Tr.of("swift.hud.hint_idle") : (this.sel.size() > 1 ? Tr.of("swift.hud.hint_multi") : Tr.of("swift.hud.hint_sel"));
      int w = c.textWidth(hint) + 20;
      if (w > this.width - 20) {
         hint = trunc(c, hint, this.width - 40);
         w = c.textWidth(hint) + 20;
      }

      int x = (this.width - w) / 2;
      int y = this.height - 22;
      c.card(x, y, w, 14, 0xB0090C12, 0xFF1B2438, 1, 7.0F);
      c.centeredText(hint, this.width / 2, y + 3, FAINT, false);
      if (!this.live) {
         String p = Tr.of("swift.hud.preview");
         c.centeredText(trunc(c, p, this.width - 40), this.width / 2, y - 12, 0xFF44506A, false);
      }
   }

   // ---------------------------------------------------------------------------------------------
   // Selection helpers
   // ---------------------------------------------------------------------------------------------

   private void pruneSelection() {
      this.sel.removeIf(id -> !this.boxes.containsKey(id));
      if (this.primary != null && !this.sel.contains(this.primary)) {
         this.primary = this.sel.isEmpty() ? null : this.sel.iterator().next();
      }
   }

   private void select(String id, boolean additive) {
      if (!additive) {
         this.sel.clear();
      }

      if (additive && this.sel.contains(id)) {
         this.sel.remove(id);
         this.primary = this.sel.isEmpty() ? null : this.sel.iterator().next();
      } else {
         this.sel.add(id);
         this.primary = id;
      }
   }

   private int[] groupBox() {
      int x0 = Integer.MAX_VALUE;
      int y0 = Integer.MAX_VALUE;
      int x1 = Integer.MIN_VALUE;
      int y1 = Integer.MIN_VALUE;
      for (String id : this.sel) {
         int[] b = this.boxes.get(id);
         if (b != null) {
            x0 = Math.min(x0, b[0]);
            y0 = Math.min(y0, b[1]);
            x1 = Math.max(x1, b[0] + b[2]);
            y1 = Math.max(y1, b[1] + b[3]);
         }
      }

      return x0 == Integer.MAX_VALUE ? new int[]{0, 0, 0, 0} : new int[]{x0, y0, x1 - x0, y1 - y0};
   }

   private String pickId(double mx, double my) {
      List<HudElement> els = HudManager.elements();
      String best = null;
      int bestArea = Integer.MAX_VALUE;
      for (int i = els.size() - 1; i >= 0; i--) {
         int[] b = this.boxes.get(els.get(i).id);
         if (b != null && in(mx, my, b)) {
            // Smallest box wins: a small readout on top of a big one stays reachable
            int area = b[2] * b[3];
            if (area < bestArea) {
               bestArea = area;
               best = els.get(i).id;
            }
         }
      }

      return best;
   }

   private int[] marqueeRect() {
      int x = Math.min(this.downX, this.curX);
      int y = Math.min(this.downY, this.curY);
      return new int[]{x, y, Math.abs(this.curX - this.downX), Math.abs(this.curY - this.downY)};
   }

   // ---------------------------------------------------------------------------------------------
   // History
   // ---------------------------------------------------------------------------------------------

   /** Runs a change and records it for undo. */
   private void gesture(Runnable change) {
      Map<String, String> before = HudManager.exporter();
      change.run();
      HudManager.save();
      this.commit(before);
   }

   private void commit(Map<String, String> before) {
      if (before != null && !before.equals(HudManager.exporter())) {
         this.undo.push(before);
         while (this.undo.size() > 60) {
            this.undo.removeLast();
         }

         this.redo.clear();
      }
   }

   private void undo() {
      if (!this.undo.isEmpty()) {
         this.redo.push(HudManager.exporter());
         HudManager.importer(this.undo.pop());
      }
   }

   private void redo() {
      if (!this.redo.isEmpty()) {
         this.undo.push(HudManager.exporter());
         HudManager.importer(this.redo.pop());
      }
   }

   // ---------------------------------------------------------------------------------------------
   // Input
   // ---------------------------------------------------------------------------------------------

   @Override
   public boolean click(double mx, double my, int button) {
      this.curX = (int)mx;
      this.curY = (int)my;
      for (Tool t : this.tools) {
         if (t.enabled() && in(mx, my, t.r()) && this.chromeFade > 0.6F) {
            t.action().run();
            Platform.game().playClick();
            return true;
         }
      }

      if (this.drawerOpen && this.clickDrawer(mx, my)) {
         return true;
      }

      if (this.inspector != null && in(mx, my, this.inspector)) {
         for (Hit h : this.inspectorHits) {
            if (in(mx, my, h.r())) {
               h.action().run();
               Platform.game().playClick();
               break;
            }
         }

         return true;
      }

      if (button == 1) {
         String id = this.pickId(mx, my);
         if (id != null) {
            this.select(id, false);
         }

         return true;
      }

      if (button != 0) {
         return true;
      }

      this.downX = (int)mx;
      this.downY = (int)my;
      this.moved = false;
      this.collapseTo = null;
      this.base = HudManager.exporter();
      int corner = this.handleAt(mx, my);
      if (corner >= 0) {
         this.mode = Mode.RESIZE;
         this.resizeCorner = corner;
         this.resizeId = this.primary;
         this.resizeStartBox = this.boxes.get(this.primary).clone();
         this.resizeStartScale = HudManager.scale(this.primary);
         return true;
      }

      String id = this.pickId(mx, my);
      if (id != null) {
         if (shift()) {
            this.select(id, true);
         } else if (!this.sel.contains(id)) {
            this.select(id, false);
         } else {
            // Pressing inside a group keeps it for dragging; a plain click (no drag) narrows it on release
            this.collapseTo = id;
            this.primary = id;
         }

         this.beginMove();
      } else {
         this.mode = Mode.MARQUEE;
         if (!shift()) {
            this.sel.clear();
            this.primary = null;
         }
      }

      return true;
   }

   private void beginMove() {
      this.mode = Mode.MOVE;
      this.startBoxes.clear();
      for (String id : this.sel) {
         int[] b = this.boxes.get(id);
         if (b != null) {
            this.startBoxes.put(id, b.clone());
         }
      }
   }

   @Override
   public boolean mouseDragged(double mx, double my, int button) {
      this.curX = (int)mx;
      this.curY = (int)my;
      if (this.mode == Mode.NONE) {
         return false;
      }

      if (!this.moved && Math.abs(mx - this.downX) + Math.abs(my - this.downY) < 3) {
         return true;
      }

      this.moved = true;
      switch (this.mode) {
         case MARQUEE -> this.dragMarquee();
         case RESIZE -> this.dragResize(mx, my);
         case MOVE -> this.dragMove(mx, my);
         default -> {
         }
      }

      return true;
   }

   private void dragMarquee() {
      int[] m = this.marqueeRect();
      Set<String> keep = shift() ? new LinkedHashSet<>(this.sel) : new LinkedHashSet<>();
      for (HudElement e : HudManager.elements()) {
         int[] b = this.boxes.get(e.id);
         if (b != null && overlaps(m, b)) {
            keep.add(e.id);
         }
      }

      this.sel.clear();
      this.sel.addAll(keep);
      this.primary = this.sel.isEmpty() ? null : this.sel.iterator().next();
   }

   private void dragResize(double mx, double my) {
      int[] b = this.resizeStartBox;
      boolean left = this.resizeCorner == 0 || this.resizeCorner == 2;
      boolean top = this.resizeCorner == 0 || this.resizeCorner == 1;
      float f = left ? (float)(b[0] + b[2] - mx) / Math.max(1, b[2]) : (float)(mx - b[0]) / Math.max(1, b[2]);
      HudManager.setScale(this.resizeId, this.resizeStartScale * Math.max(0.1F, f));
      float k = HudManager.scale(this.resizeId) / this.resizeStartScale;
      int nw = Math.round(b[2] * k);
      int nh = Math.round(b[3] * k);
      int nx = left ? b[0] + b[2] - nw : b[0];
      int ny = top ? b[1] + b[3] - nh : b[1];
      HudManager.setTopLeft(this.resizeId, nx, ny, nw, nh, this.width, this.height);
   }

   private void dragMove(double mx, double my) {
      int dx = (int)mx - this.downX;
      int dy = (int)my - this.downY;
      int gx0 = Integer.MAX_VALUE;
      int gy0 = Integer.MAX_VALUE;
      int gx1 = Integer.MIN_VALUE;
      int gy1 = Integer.MIN_VALUE;
      for (int[] b : this.startBoxes.values()) {
         gx0 = Math.min(gx0, b[0]);
         gy0 = Math.min(gy0, b[1]);
         gx1 = Math.max(gx1, b[0] + b[2]);
         gy1 = Math.max(gy1, b[1] + b[3]);
      }

      int gw = gx1 - gx0;
      int gh = gy1 - gy0;
      int nx = gx0 + dx;
      int ny = gy0 + dy;
      this.guideX = Integer.MIN_VALUE;
      this.guideY = Integer.MIN_VALUE;
      if (this.snap && !ctrl()) {
         int[] sx = this.snapAxis(nx, gw, this.targets(true));
         int[] sy = this.snapAxis(ny, gh, this.targets(false));
         nx = sx[0];
         this.guideX = sx[1];
         ny = sy[0];
         this.guideY = sy[1];
      }

      if (this.grid && this.guideX == Integer.MIN_VALUE) {
         nx = Math.round(nx / (float)GRID) * GRID;
      }

      if (this.grid && this.guideY == Integer.MIN_VALUE) {
         ny = Math.round(ny / (float)GRID) * GRID;
      }

      nx = Math.max(2, Math.min(nx, this.width - gw - 2));
      ny = Math.max(2, Math.min(ny, this.height - gh - 2));
      int ddx = nx - gx0;
      int ddy = ny - gy0;
      for (Map.Entry<String, int[]> en : this.startBoxes.entrySet()) {
         int[] b = en.getValue();
         HudManager.setTopLeft(en.getKey(), b[0] + ddx, b[1] + ddy, b[2], b[3], this.width, this.height);
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
      for (Map.Entry<String, int[]> en : this.boxes.entrySet()) {
         if (!this.sel.contains(en.getKey()) && this.on(this.element(en.getKey()))) {
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
      int bestDelta = 0;
      int guide = Integer.MIN_VALUE;
      int bestAbs = SNAP_DIST + 1;
      // {position of the moving group, kind of target it can lock to: 0 start, 1 centre, 2 end}
      int[][] pairs = {{start, 0}, {start, 2}, {start + size / 2, 1}, {start + size, 2}, {start + size, 0}};
      for (int[] pair : pairs) {
         for (int t : targets.get(pair[1])) {
            int delta = t - pair[0];
            int abs = Math.abs(delta);
            if (abs < bestAbs) {
               bestAbs = abs;
               bestDelta = delta;
               guide = t;
            }
         }
      }

      return bestAbs > SNAP_DIST ? new int[]{start, Integer.MIN_VALUE} : new int[]{start + bestDelta, guide};
   }

   @Override
   public boolean mouseReleased(double mx, double my, int button) {
      Mode m = this.mode;
      if (m == Mode.NONE) {
         return false;
      }

      this.mode = Mode.NONE;
      if (m == Mode.MOVE) {
         if (!this.moved && this.collapseTo != null && !shift()) {
            this.select(this.collapseTo, false);
         }

         for (String id : this.sel) {
            int[] b = this.boxes.get(id);
            if (b != null && this.moved) {
               HudManager.autoAnchor(id, b[2], b[3], this.width, this.height);
            }
         }
      }

      this.guideX = Integer.MIN_VALUE;
      this.guideY = Integer.MIN_VALUE;
      HudManager.save();
      this.commit(this.base);
      this.base = null;
      return true;
   }

   @Override
   public boolean scroll(double mx, double my, double amount) {
      if (this.drawerOpen && mx < this.drawerRight()) {
         this.drawerScroll.cran(amount, ROW_H * 2);
         return true;
      }

      String id = this.pickId(mx, my);
      if (id == null) {
         id = this.primary;
      }

      if (id == null) {
         return false;
      }

      if (!this.sel.contains(id)) {
         this.select(id, false);
      }

      String target = id;
      this.gesture(() -> this.scaleBy(this.element(target), (float)(amount * 0.05)));
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode) {
      switch (keyCode) {
         case 69: // E
            this.drawerOpen = !this.drawerOpen;
            return true;
         case 71: // G
            this.grid = !this.grid;
            return true;
         case 83: // S
            this.snap = !this.snap;
            return true;
         case 89: // Y: redo with Ctrl, guides otherwise
            if (ctrl()) {
               this.redo();
            } else {
               this.guides = !this.guides;
            }

            return true;
         case 90: // Z
            if (ctrl()) {
               if (shift()) {
                  this.redo();
               } else {
                  this.undo();
               }

               return true;
            }

            return false;
         case 65: // A
            if (ctrl()) {
               this.sel.clear();
               for (HudElement e : HudManager.elements()) {
                  if (this.on(e)) {
                     this.sel.add(e.id);
                  }
               }

               this.primary = this.sel.isEmpty() ? null : this.sel.iterator().next();
               return true;
            }

            return false;
         case 82: // R
            if (this.sel.isEmpty()) {
               return false;
            }

            this.gesture(() -> {
               for (String id : this.sel) {
                  HudManager.reset(id);
               }
            });
            return true;
         case K_RIGHT:
            return this.nudge(1, 0);
         case K_LEFT:
            return this.nudge(-1, 0);
         case K_DOWN:
            return this.nudge(0, 1);
         case K_UP:
            return this.nudge(0, -1);
         case K_DELETE:
            return false;
         default:
            return false;
      }
   }

   private boolean nudge(int dx, int dy) {
      if (this.sel.isEmpty()) {
         return false;
      }

      int step = shift() ? 8 : 1;
      int[] g = this.groupBox();
      int ddx = Math.max(2 - g[0], Math.min(dx * step, this.width - g[2] - 2 - g[0]));
      int ddy = Math.max(2 - g[1], Math.min(dy * step, this.height - g[3] - 2 - g[1]));
      this.gesture(() -> {
         for (String id : this.sel) {
            int[] b = this.boxes.get(id);
            if (b != null) {
               HudManager.setTopLeft(id, b[0] + ddx, b[1] + ddy, b[2], b[3], this.width, this.height);
            }
         }
      });
      return true;
   }

   @Override
   public boolean closeOnEscape() {
      if (this.drawerOpen) {
         this.drawerOpen = false;
         return false;
      }

      if (!this.sel.isEmpty()) {
         this.sel.clear();
         this.primary = null;
         return false;
      }

      HudManager.save();
      return true;
   }
}
