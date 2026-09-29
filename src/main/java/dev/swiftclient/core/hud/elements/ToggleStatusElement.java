package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.platform.Tr;

/** "[Sprinting (Toggled)]": what Toggle Sprint / Toggle Sneak are doing right now. */
public final class ToggleStatusElement extends HudElement {
   public ToggleStatusElement() {
      super("togglestatus", "Toggle Sneak/Sprint");
   }

   @Override
   public String icon() {
      return "togglesprint";
   }

   private String text(HudData d) {
      if (d.sneaking()) {
         return "[" + Tr.of(ModuleManager.active("togglesneak") ? "swift.hud.sneaking_toggled" : "swift.hud.sneaking") + "]";
      } else if (d.sprinting()) {
         return "[" + Tr.of(ModuleManager.active("togglesprint") ? "swift.hud.sprinting_toggled" : "swift.hud.sprinting") + "]";
      } else {
         return "";
      }
   }

   @Override
   public int width(Canvas c, HudData d) {
      String t = this.text(d);
      return t.isEmpty() ? 0 : c.textWidth(t) + 4;
   }

   @Override
   public int height(Canvas c) {
      return c.lineHeight() + 2;
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      String t = this.text(d);
      if (!t.isEmpty()) {
         c.text(t, x + 2, y + 1, this.optColor("color", -1), true);
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"[Sprinting (Toggled)]"};
   }
}
