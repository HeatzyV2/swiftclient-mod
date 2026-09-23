package dev.swiftclient.core.ui;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Account;
import dev.swiftclient.core.platform.Platform;

/** Minimal top account control — text, no LightClient pill slab. */
public final class AccountBadge {
   private static final int CARD_W = 100;
   private static final int CARD_H = 16;

   private AccountBadge() {
   }

   private static int posX(int screenW) {
      // Stay clear of Essential's right column when present
      if (dev.swiftclient.core.ModCompat.essentialLoaded()) {
         return 36;
      }
      return screenW - CARD_W - 14;
   }

   private static int posY() {
      return dev.swiftclient.core.ModCompat.essentialLoaded() ? 14 : 14;
   }

   public static void render(Canvas c, int screenW, int mouseX, int mouseY) {
      int x = posX(screenW);
      int y = posY();
      boolean hover = mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H;
      Account active = null;
      for (Account a : Platform.game().accounts()) {
         if (a.active()) {
            active = a;
            break;
         }
      }

      String label = active != null ? trim(c, active.username(), CARD_W - 12) : tr("swift.account.add");
      int col = hover ? -12877066 : -4473925;
      c.text(label, x, y + 2, col, false);
      c.text("▾", x + c.textWidth(label) + 4, y + 2, -10066330, false);
   }

   private static String tr(String key) {
      try {
         return Platform.game().translate(key);
      } catch (Throwable t) {
         return key;
      }
   }

   public static boolean isOnBadge(int screenW, double mouseX, double mouseY) {
      int x = posX(screenW);
      int y = posY();
      return mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H;
   }

   private static String trim(Canvas c, String s, int maxW) {
      if (c.textWidth(s) <= maxW) {
         return s;
      }
      while (s.length() > 1 && c.textWidth(s + "…") > maxW) {
         s = s.substring(0, s.length() - 1);
      }
      return s + "…";
   }
}
