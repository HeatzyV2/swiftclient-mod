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

   public abstract int width(Canvas var1, HudData var2);

   public abstract int height(Canvas var1);

   public int height(Canvas c, HudData d) {
      return this.height(c);
   }

   public abstract void draw(Canvas var1, HudData var2, int var3, int var4);

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

      return new int[]{w + 12, lines.length * (c.lineHeight() + 2) + 5};
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

      return w + 12;
   }

   protected static int chipH(Canvas c, int n) {
      return n * (c.lineHeight() + 2) + 5;
   }

   protected static void chip(Canvas c, int x, int y, String... lines) {
      int w = chipW(c, lines);
      int h = chipH(c, lines.length);
      c.card(x, y, w, h, 1996488704, 587202559, 1, 4.0F);
      int yy = y + 4;

      for (String s : lines) {
         c.text(s, x + 6, yy, -1, true);
         yy += c.lineHeight() + 2;
      }
   }
}
