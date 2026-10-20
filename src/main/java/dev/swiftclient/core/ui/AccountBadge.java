package dev.swiftclient.core.ui;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Account;
import dev.swiftclient.core.platform.Platform;

/**
 * Account button of the title screen, top right corner: a chamfered slot like the menu buttons, with the active
 * account's head and name (or "+ Account" when signed out) and a chevron. Clicking it opens the accounts.
 */
public final class AccountBadge {
   private static final int H = 22;
   private static final int MARGIN = 8;
   private static final int MAX_NAME = 110;
   /** Where the button was last drawn: x, y, w, h (its width follows the name). */
   private static final int[] LAST = new int[]{-1, -1, 0, 0};

   private AccountBadge() {
   }

   public static void render(Canvas c, int screenW, int mouseX, int mouseY) {
      Account active = null;
      for (Account a : Platform.game().accounts()) {
         if (a.active()) {
            active = a;
            break;
         }
      }

      String label = active != null ? trim(c, active.username(), MAX_NAME) : tr("swift.account.add");
      int head = active != null ? 14 : 0;
      int w = 8 + (head > 0 ? head + 7 : 0) + c.textWidth(label) + 18;
      // Stay clear of Essential's right column when present
      int x = dev.swiftclient.core.ModCompat.essentialLoaded() ? 36 : screenW - w - MARGIN;
      int y = MARGIN;
      LAST[0] = x;
      LAST[1] = y;
      LAST[2] = w;
      LAST[3] = H;
      boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + H;
      Px.slot(c, x, y, w, H, 5, hover ? 0xF0151C2C : 0xE611141B, hover ? Px.ACCENT : 0xFF222838);
      int tx = x + 8;
      if (head > 0) {
         c.playerHead(active.uuid(), tx, y + (H - head) / 2, head);
         tx += head + 7;
      }

      c.text(label, tx, y + (H - c.lineHeight()) / 2 + 1, hover ? 0xFFFFFFFF : 0xFFC9D3E6, false);
      c.icon("chevron_down", x + w - 14, y + (H - 8) / 2, 8, hover ? Px.ACCENT : 0xFF98A3B8);
   }

   private static String tr(String key) {
      try {
         return Platform.game().translate(key);
      } catch (Throwable t) {
         return key;
      }
   }

   public static boolean isOnBadge(int screenW, double mouseX, double mouseY) {
      return LAST[0] >= 0 && mouseX >= LAST[0] && mouseX < LAST[0] + LAST[2] && mouseY >= LAST[1] && mouseY < LAST[1] + LAST[3];
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
