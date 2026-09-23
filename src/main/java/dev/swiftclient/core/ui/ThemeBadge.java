package dev.swiftclient.core.ui;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.theme.Theme;
import dev.swiftclient.core.theme.ThemeManager;
import dev.swiftclient.core.theme.Themes;

/** Minimal theme label — text only. */
public final class ThemeBadge {
   private static final int CARD_W = 90;
   private static final int CARD_H = 16;

   private ThemeBadge() {
   }

   private static int posX(int screenW) {
      return screenW - CARD_W - 14;
   }

   private static int posY() {
      return 32;
   }

   public static void render(Canvas c, int screenW, int mouseX, int mouseY) {
      int x = posX(screenW);
      int y = posY();
      boolean hover = mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H;
      Theme t = Themes.byIdOrFirst(ThemeManager.currentId());
      String label = t == null ? "Theme" : t.name();
      int col = hover ? -12877066 : -4473925;
      c.text(label, x, y + 2, col, false);
      c.text("▾", x + c.textWidth(label) + 4, y + 2, -10066330, false);
   }

   public static boolean isOnBadge(int screenW, double mouseX, double mouseY) {
      int x = posX(screenW);
      int y = posY();
      return mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H;
   }
}
