package dev.swiftclient.core.screen;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.ui.UiScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The quick wheel: a ring of module slots around Zip. Point the mouse at one and click to switch it on or off
 * without opening the menu. Esc, a right click or the wheel key closes it.
 */
public class RadialScreen extends UiScreen {
   /** The modules that make sense to flip in the middle of a game, in wheel order (the missing ones are skipped). */
   private static final String[] QUICK = {
      "fullbright", "hud_minimap", "hud_keystrokes", "hud_cps", "togglesprint", "hitboxes", "zoom", "hud_fps", "freelook", "chunk_borders",
      "light_overlay", "hud_music",
   };
   private static final int MAX_SLOTS = 8;
   private static final int SLOT = 34;
   private static final int WHEEL_KEY = 71; // GLFW_KEY_G, the default of the wheel key

   private final long opened = System.nanoTime();
   private List<Module> slots = List.of();

   @Override
   public String title() {
      return Tr.of("swift.radial.title");
   }

   @Override
   public void layout(int width, int height) {
      super.layout(width, height);
      List<Module> found = new ArrayList<>();
      for (String id : QUICK) {
         Module m = ModuleManager.byId(id);
         if (m != null && found.size() < MAX_SLOTS) {
            found.add(m);
         }
      }
      this.slots = found;
   }

   private int cx() {
      return this.width / 2;
   }

   private int cy() {
      return this.height / 2;
   }

   private int radius() {
      return Math.max(46, Math.min(86, Math.min(this.width, this.height) / 2 - SLOT - 14));
   }

   /** Centre of slot i: the first one straight up, then clockwise. */
   private int[] slotCenter(int i) {
      double a = -Math.PI / 2 + i * 2 * Math.PI / Math.max(1, this.slots.size());
      return new int[]{this.cx() + (int)Math.round(Math.cos(a) * this.radius()), this.cy() + (int)Math.round(Math.sin(a) * this.radius())};
   }

   /** The slot the pointer is aiming at: the nearest in angle, once the pointer has left the middle. */
   private int aimed(int mx, int my) {
      if (this.slots.isEmpty()) {
         return -1;
      }
      double dx = mx - this.cx();
      double dy = my - this.cy();
      if (dx * dx + dy * dy < 22 * 22) {
         return -1;
      }
      double a = Math.atan2(dy, dx) + Math.PI / 2;
      if (a < 0) {
         a += 2 * Math.PI;
      }
      double step = 2 * Math.PI / this.slots.size();
      return (int)Math.round(a / step) % this.slots.size();
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      long t = (System.nanoTime() - this.opened) / 1_000_000L;
      float open = Math.min(1.0F, t / 140.0F);
      c.fill(0, 0, this.width, this.height, ((int)(0x99 * open) << 24));

      int hover = this.aimed(mouseX, mouseY);
      // Spokes from the middle to each slot
      for (int i = 0; i < this.slots.size(); i++) {
         int[] p = this.slotCenter(i);
         boolean lit = i == hover;
         int steps = 8;
         for (int s = 1; s < steps; s++) {
            int x = this.cx() + (p[0] - this.cx()) * s / steps;
            int y = this.cy() + (p[1] - this.cy()) * s / steps;
            c.fill(x - 1, y - 1, x + 1, y + 1, lit ? Px.ACCENT : 0x55FFFFFF);
         }
      }

      // The slots
      for (int i = 0; i < this.slots.size(); i++) {
         Module m = this.slots.get(i);
         int[] p = this.slotCenter(i);
         boolean on = m.isEnabled();
         boolean lit = i == hover;
         int grow = lit ? 4 : 0;
         int size = SLOT + grow;
         int x = p[0] - size / 2;
         int y = p[1] - size / 2 - (lit ? 2 : 0);
         c.card(x, y, size, size, lit ? 0xFF1D2535 : 0xFF11151F, on ? Px.ACCENT : (lit ? 0xFFFFFFFF : 0), on || lit ? 1 : 0, 4.0F);
         c.icon(m.icon == null ? "mods" : m.icon, x + (size - 18) / 2, y + (size - 18) / 2, 18, on ? 0xFFFFFFFF : 0xFF98A3B8);
         Px.lamp(c, x + 3, y + 3, on);
      }

      // The middle: Zip, and the name and state of the aimed slot
      Px.zip(c, this.cx() - 12, this.cy() - 20, 1, false, t);
      if (hover >= 0) {
         Module m = this.slots.get(hover);
         String name = m.displayName();
         String state = Tr.of(m.isEnabled() ? "swift.radial.on" : "swift.radial.off");
         int w = Math.max(c.textWidth(name), c.textWidth(state)) + 16;
         int by = this.cy() + 14;
         c.card(this.cx() - w / 2, by, w, 24, 0xFF000000, 0xFF222B42, 1, 3.0F);
         c.centeredText(name, this.cx(), by + 4, Px.TEXT, true);
         c.centeredText(state.toUpperCase(Locale.ROOT), this.cx(), by + 14, m.isEnabled() ? Px.OK : Px.FAINT, false);
      }

      if (this.slots.isEmpty()) {
         c.centeredText(Tr.of("swift.radial.empty"), this.cx(), this.cy() + 20, Px.DIM, true);
      }
      c.centeredText(Tr.of("swift.radial.hint"), this.cx(), this.height - 18, Px.FAINT, false);
   }

   @Override
   public boolean click(double mouseX, double mouseY, int button) {
      if (button == 1) {
         this.back();
         return true;
      }
      int i = this.aimed((int)mouseX, (int)mouseY);
      if (button == 0 && i >= 0) {
         this.slots.get(i).toggle();
         return true;
      }
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode) {
      if (keyCode == WHEEL_KEY) {
         this.back();
         return true;
      }
      // 1-8 flip the slots too
      if (keyCode >= 49 && keyCode < 49 + this.slots.size()) {
         this.slots.get(keyCode - 49).toggle();
         return true;
      }
      return false;
   }
}
