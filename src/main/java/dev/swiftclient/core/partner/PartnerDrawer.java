package dev.swiftclient.core.partner;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Platform;
import java.util.List;

public class PartnerDrawer {
   private static final int STAR = -10163;
   private static final int TEXT = -1;
   private static final int TEXT_SUB = -6250336;
   public static final int PANEL_W = 308;
   public static final int TOP_Y = 6;
   private static final int ROW_H = 24;
   private static final int GAP = 2;
   private int screenW;
   private final List<PartnerServer> servers = PartnerServers.all();

   public int bottomY() {
      return 6 + this.height();
   }

   public boolean hasServers() {
      return !this.servers.isEmpty();
   }

   private int panelX() {
      return (this.screenW - 308) / 2;
   }

   public int height() {
      return this.servers.isEmpty() ? 0 : this.servers.size() * 26;
   }

   public void draw(Canvas c, int screenW, int mouseX, int mouseY) {
      this.screenW = screenW;
      if (!this.servers.isEmpty()) {
         int px = this.panelX();
         int y = 6;

         for (PartnerServer s : this.servers) {
            boolean over = hit(mouseX, mouseY, px, y, 308, 24);
            c.vanillaButton(px, y, 308, 24, over);
            c.text("★", px + 8, y + (24 - c.lineHeight()) / 2 + 1, -10163, true);
            int tx = px + 24;
            if (!s.description().isBlank()) {
               c.text(s.name(), tx, y + 4, -1, true);
               c.text(s.description(), tx, y + 13, -6250336, true);
            } else {
               c.text(s.name(), tx, y + (24 - c.lineHeight()) / 2 + 1, -1, true);
            }

            y += 26;
         }
      }
   }

   public PartnerDrawer.ClickResult clickAt(int screenW, double mouseX, double mouseY) {
      this.screenW = screenW;
      if (this.servers.isEmpty()) {
         return PartnerDrawer.ClickResult.NONE;
      } else {
         int px = this.panelX();
         int y = 6;

         for (PartnerServer s : this.servers) {
            if (hit(mouseX, mouseY, px, y, 308, 24)) {
               Platform.game().playClick();
               return new PartnerDrawer.ClickResult(false, s);
            }

            y += 26;
         }

         return PartnerDrawer.ClickResult.NONE;
      }
   }

   private static boolean hit(double mx, double my, int x, int y, int w, int h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   public record ClickResult(boolean toggled, PartnerServer join) {
      static final PartnerDrawer.ClickResult NONE = new PartnerDrawer.ClickResult(false, null);
   }
}
