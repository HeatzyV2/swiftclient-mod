package dev.swiftclient.core.hud;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;

public abstract class HudElement {
   public final String id;
   public final String label;

   protected HudElement(String id, String label) {
      this.id = id;
      this.label = label;
   }

   public String moduleId() {
      return "hud_" + this.id;
   }

   public abstract int width(Canvas c, HudData d);

   public abstract int height(Canvas c);

   public int height(Canvas c, HudData d) {
      return this.height(c);
   }

   public abstract void draw(Canvas c, HudData d, int x, int y);

   public String icon() {
      return "gear";
   }

   public String[] previewLines() {
      return new String[]{this.label};
   }

   public boolean drawsWithoutData() {
      return false;
   }

   public int[] previewSize(Canvas c) {
      String[] lines = this.previewLines();
      int w = 0;

      for (String s : lines) {
         w = Math.max(w, c.textWidth(s));
      }

      return new int[]{w + 17, lines.length * (c.lineHeight() + 2) + 7};
   }

   protected ModuleSetting setting(String settingId) {
      Module m = ModuleManager.byId(this.moduleId());
      return m == null ? null : m.setting(settingId);
   }

   protected boolean opt(String settingId, boolean def) {
      ModuleSetting s = this.setting(settingId);
      return s == null ? def : s.boolValue();
   }

   protected double optValue(String settingId, double def) {
      ModuleSetting s = this.setting(settingId);
      return s == null ? def : s.value();
   }

   protected int optCycle(String settingId, int def) {
      ModuleSetting s = this.setting(settingId);
      return s == null ? def : s.cycleIndex();
   }

   protected int optColor(String settingId, int def) {
      ModuleSetting s = this.setting(settingId);
      return s == null ? def : s.colorValue();
   }

   protected static int chipW(Canvas c, String... lines) {
      int w = 0;

      for (String s : lines) {
         w = Math.max(w, c.textWidth(s));
      }

      return w + 17;
   }

   protected static int chipH(Canvas c, int n) {
      return n * (c.lineHeight() + 2) + 7;
   }

   /** A bare plate (outline, solid side, accent tab) for elements that lay out their own content. */
   protected static void plate(Canvas c, int x, int y, int w, int h) {
      c.card(x, y, w, h, 0xE60C0F16, 0, 0, 3.0F);
      c.fill(x + 2, y + 3, x + 4, y + h - 3, 0xFF3B82F6);
   }

   /** The Swift plate every simple HUD readout is drawn on: ink outline, solid side, accent tab, pixel text. */
   protected static void chip(Canvas c, int x, int y, String... lines) {
      int w = chipW(c, lines);
      int h = chipH(c, lines.length);
      c.card(x, y, w, h, 0xE60C0F16, 0, 0, 3.0F);
      c.fill(x + 2, y + 3, x + 4, y + h - 3, 0xFF3B82F6);
      int yy = y + 4;

      for (String s : lines) {
         c.text(s, x + 9, yy, -1, true);
         yy += c.lineHeight() + 2;
      }
   }
}
