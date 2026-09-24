package dev.swiftclient.core.screen;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.Crosshair;
import dev.swiftclient.core.hud.CrosshairPixels;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.modules.CrosshairModule;
import dev.swiftclient.core.ui.UiScreen;

public final class CrosshairEditorScreen extends UiScreen {
   private static final int N = 16;
   private boolean painting = false;
   private boolean paintValue = true;

   @Override
   public String title() {
      return "Crosshair";
   }

   private int cell() {
      return Math.max(8, Math.min((this.width - 80) / 16, (this.height - 150) / 16));
   }

   private int gridW() {
      return this.cell() * 16;
   }

   private int gridX() {
      return (this.width - this.gridW()) / 2;
   }

   private int gridY() {
      return 52;
   }

   private int[] clearRect() {
      return new int[]{this.width / 2 - 104, this.gridY() + this.gridW() + 16, 100, 22};
   }

   private int[] doneRect() {
      return new int[]{this.width / 2 + 4, this.gridY() + this.gridW() + 16, 100, 22};
   }

   private int color() {
      return ModuleManager.get(CrosshairModule.class).color.colorValue();
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      c.fill(0, 0, this.width, this.height, -267711214);
      c.centeredText("CROSSHAIR EDITOR", this.width / 2, 16, -1, true);
      c.centeredText("Left click to draw · right click to erase · drag to paint", this.width / 2, 32, -6642766, false);
      int gx = this.gridX();
      int gy = this.gridY();
      int cs = this.cell();
      int col = this.color();

      for (int y = 0; y < 16; y++) {
         for (int x = 0; x < 16; x++) {
            int px = gx + x * cs;
            int py = gy + y * cs;
            int bg = (x + y & 1) == 0 ? -15066338 : -15461096;
            c.fill(px, py, px + cs, py + cs, bg);
            if (CrosshairPixels.get(x, y)) {
               c.fill(px + 1, py + 1, px + cs - 1, py + cs - 1, col);
            }
         }
      }

      c.card(gx, gy, this.gridW(), this.gridW(), 0, 872415231, 1, 3.0F);
      int mid = 8;
      c.fill(gx + mid * cs, gy, gx + mid * cs + 1, gy + this.gridW(), 587202559);
      c.fill(gx, gy + mid * cs, gx + this.gridW(), gy + mid * cs + 1, 587202559);
      c.centeredText("preview", gx + this.gridW() + 34, gy + 2, -10394518, false);
      Crosshair.draw(c, gx + this.gridW() + 34, gy + 24);
      this.button(c, this.clearRect(), "Clear", mouseX, mouseY);
      this.button(c, this.doneRect(), "Done", mouseX, mouseY);
   }

   private void button(Canvas c, int[] r, String label, int mx, int my) {
      boolean over = mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
      c.card(r[0], r[1], r[2], r[3], over ? -13882063 : -14671580, 872415231, 1, 5.0F);
      c.centeredText(label, r[0] + r[2] / 2, r[1] + (r[3] - 8) / 2, -1, false);
   }

   private boolean paintAt(double mx, double my, boolean value) {
      int gx = this.gridX();
      int gy = this.gridY();
      int cs = this.cell();
      int x = (int)((mx - gx) / cs);
      int y = (int)((my - gy) / cs);
      if (!(mx < gx) && !(my < gy) && x < 16 && y < 16) {
         CrosshairPixels.set(x, y, value);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean click(double mx, double my, int button) {
      if (in(mx, my, this.clearRect())) {
         CrosshairPixels.clear();
         return true;
      } else if (in(mx, my, this.doneRect())) {
         this.back();
         return true;
      } else {
         this.paintValue = button != 1;
         if (this.paintAt(mx, my, this.paintValue)) {
            this.painting = true;
            return true;
         } else {
            return true;
         }
      }
   }

   @Override
   public boolean mouseDragged(double mx, double my, int button) {
      if (this.painting) {
         this.paintAt(mx, my, this.paintValue);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseReleased(double mx, double my, int button) {
      this.painting = false;
      return false;
   }

   @Override
   public boolean keyPressed(int keyCode) {
      if (keyCode == 256) {
         this.back();
         return true;
      } else {
         return false;
      }
   }

   private static boolean in(double mx, double my, int[] r) {
      return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
   }
}
