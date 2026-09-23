package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.HudStats;

public final class KeystrokesElement extends HudElement {
   private static final int GAP = 2;

   public KeystrokesElement() {
      super("keystrokes", "Keystrokes");
   }

   private int box() {
      return (int)Math.round(this.optValue("size", 16.0));
   }

   private int width() {
      return this.box() * 3 + 4;
   }

   private int rowH() {
      return this.box();
   }

   private int barH() {
      return Math.max(8, this.box() * 2 / 3);
   }

   @Override
   public int width(Canvas c, HudData d) {
      return this.width();
   }

   @Override
   public int height(Canvas c) {
      int h = this.rowH() * 2 + 2;
      if (this.opt("mouse", true)) {
         h += 2 + this.mouseH();
      }

      if (this.opt("space", true)) {
         h += 2 + this.barH();
      }

      if (this.opt("sneak", false)) {
         h += 2 + this.barH();
      }

      return h;
   }

   private int mouseH() {
      return this.opt("cps", true) ? this.rowH() + 8 : this.rowH();
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      int s = this.box();
      int w = this.width();
      int pressed = this.optColor("pressedColor", -1);
      int idle = this.optColor("idleColor", 1996488704);
      this.key(c, x + s + 2, y, s, this.rowH(), "W", HudStats.pressed(1), null, pressed, idle);
      int cy = y + this.rowH() + 2;
      this.key(c, x, cy, s, this.rowH(), "A", HudStats.pressed(2), null, pressed, idle);
      this.key(c, x + s + 2, cy, s, this.rowH(), "S", HudStats.pressed(4), null, pressed, idle);
      this.key(c, x + (s + 2) * 2, cy, s, this.rowH(), "D", HudStats.pressed(8), null, pressed, idle);
      cy += this.rowH();
      if (this.opt("mouse", true)) {
         cy += 2;
         int half = (w - 2) / 2;
         boolean cps = this.opt("cps", true);
         this.key(c, x, cy, half, this.mouseH(), "LMB", HudStats.pressed(128), cps ? String.valueOf(HudStats.leftCps()) : null, pressed, idle);
         this.key(
            c, x + half + 2, cy, w - half - 2, this.mouseH(), "RMB", HudStats.pressed(256), cps ? String.valueOf(HudStats.rightCps()) : null, pressed, idle
         );
         cy += this.mouseH();
      }

      if (this.opt("space", true)) {
         cy += 2;
         this.key(c, x, cy, w, this.barH(), "SPACE", HudStats.pressed(16), null, pressed, idle);
         cy += this.barH();
      }

      if (this.opt("sneak", false)) {
         cy += 2;
         this.key(c, x, cy, w, this.barH(), "SHIFT", HudStats.pressed(32), null, pressed, idle);
      }
   }

   private void key(Canvas c, int x, int y, int w, int h, String label, boolean down, String sub, int pressedColor, int idleColor) {
      c.card(x, y, w, h, down ? pressedColor : idleColor, down ? pressedColor : 587202559, 1, 3.0F);
      int fg = down ? -15724268 : -1;
      int textY = sub == null ? y + (h - 8) / 2 : y + 3;
      c.centeredText(label, x + w / 2, textY, fg, !down);
      if (sub != null) {
         c.centeredText(sub, x + w / 2, y + h - 10, down ? -13420996 : -4604474, !down);
      }
   }

   @Override
   public boolean drawsWithoutData() {
      return true;
   }

   @Override
   public String[] previewLines() {
      return new String[]{"W A S D"};
   }
}
