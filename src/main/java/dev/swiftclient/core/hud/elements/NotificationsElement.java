package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.ui.Notifications;
import java.util.List;

/** Swift toasts (friends, screenshots...), sliding in and fading out. */
public final class NotificationsElement extends HudElement {
   private static final int W = 150;
   private static final int H = 30;
   private static final int GAP = 4;

   public NotificationsElement() {
      super("notifications", "Notifications");
   }

   @Override
   public String icon() {
      return "bell";
   }

   @Override
   public int width(Canvas c, HudData d) {
      return Notifications.active().isEmpty() ? 0 : W;
   }

   @Override
   public int height(Canvas c) {
      return H;
   }

   @Override
   public int height(Canvas c, HudData d) {
      int n = Math.max(1, Notifications.active().size());
      return n * (H + GAP) - GAP;
   }

   @Override
   public boolean drawsWithoutData() {
      return true;
   }

   @Override
   public void draw(Canvas c, HudData d, int x, int y) {
      List<Notifications.Toast> toasts = Notifications.active();
      long now = System.currentTimeMillis();
      int yy = y;

      for (Notifications.Toast t : toasts) {
         long age = now - t.start();
         long left = t.durationMs() - age;
         float in = Math.min(1.0F, age / 220.0F);
         float out = Math.min(1.0F, left / 300.0F);
         float k = Math.max(0.0F, Math.min(in, out));
         float ease = 1.0F - (1.0F - k) * (1.0F - k);
         int slide = Math.round((1.0F - ease) * 24.0F);
         int a = Math.round(ease * 255.0F);
         if (a > 8) {
            int bx = x + slide;
            c.card(bx, yy, W, H, (Math.round(ease * 0xE6) << 24) | 0x0B0D12, (Math.round(ease * 0x33) << 24) | 0xFFFFFF, 1, 6.0F);
            c.fill(bx + 1, yy + 5, bx + 3, yy + H - 5, (a << 24) | 0x3B82F6);
            c.icon(t.icon(), bx + 8, yy + (H - 12) / 2, 12, (a << 24) | 0xFFFFFF);
            String title = trunc(c, t.title(), W - 32);
            String body = trunc(c, t.body(), W - 32);
            if (body.isEmpty()) {
               c.text(title, bx + 26, yy + (H - 8) / 2, (a << 24) | 0xFFFFFF, false);
            } else {
               c.text(title, bx + 26, yy + 6, (a << 24) | 0xFFFFFF, false);
               c.text(body, bx + 26, yy + 17, (a << 24) | 0x9AA3B2, false);
            }
         }

         yy += H + GAP;
      }
   }

   private static String trunc(Canvas c, String s, int max) {
      if (c.textWidth(s) <= max) {
         return s;
      } else {
         String t = s;
         while (t.length() > 1 && c.textWidth(t + "...") > max) {
            t = t.substring(0, t.length() - 1);
         }

         return t + "...";
      }
   }

   @Override
   public String[] previewLines() {
      return new String[]{"Friend online", "Steve joined Swift"};
   }
}
