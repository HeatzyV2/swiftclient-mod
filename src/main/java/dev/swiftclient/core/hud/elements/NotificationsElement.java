package dev.swiftclient.core.hud.elements;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.ui.Notifications;
import java.util.List;

/** Swift toasts (friends, screenshots...), sliding in and fading out. */
public final class NotificationsElement extends HudElement {
   private static final int W = 160;
   private static final int H = 32;
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
            // Stepped slide-in (4 px per frame), solid plate, accent tab, icon block and a draining timer
            int bx = x + (slide / 4) * 4;
            c.card(bx, yy, W, H, 0xF20C0F16, 0xFF222B42, 1, 3.0F);
            c.fill(bx + 2, yy + 3, bx + 4, yy + H - 3, 0xFF3B82F6);
            c.card(bx + 7, yy + (H - 18) / 2 - 1, 18, 18, 0xFF3B82F6, 0, 0, 2.0F);
            c.icon(t.icon(), bx + 10, yy + (H - 12) / 2 - 1, 12, 0xFFFFFFFF);
            String title = trunc(c, t.title(), W - 40);
            String body = trunc(c, t.body(), W - 40);
            if (body.isEmpty()) {
               c.text(title, bx + 30, yy + (H - 8) / 2 - 1, 0xFFFFFFFF, true);
            } else {
               c.text(title, bx + 30, yy + 5, 0xFFFFFFFF, true);
               c.text(body, bx + 30, yy + 15, 0xFF9AA6BA, false);
            }
            float rest = Math.max(0.0F, Math.min(1.0F, left / (float)t.durationMs()));
            c.fill(bx + 7, yy + H - 5, bx + 7 + Math.round((W - 14) * rest), yy + H - 3, 0xFF3B82F6);
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
