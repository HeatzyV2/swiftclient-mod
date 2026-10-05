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
            int bx = x + slide;
            int al = Math.round(ease * 0xE0);
            c.card(bx, yy, W, H, (al << 24) | 0x0A0D14, (Math.round(ease * 0x26) << 24) | 0xFFFFFF, 1, 8.0F);
            c.roundRect(bx + 8, yy + (H - 20) / 2, 20, 20, 10.0F, (a << 24) | 0x3B82F6);
            c.icon(t.icon(), bx + 11, yy + (H - 14) / 2, 14, (a << 24) | 0xFFFFFF);
            String title = trunc(c, t.title(), W - 44);
            String body = trunc(c, t.body(), W - 44);
            if (body.isEmpty()) {
               c.text(title, bx + 36, yy + (H - 8) / 2 - 1, (a << 24) | 0xFFFFFF, true);
            } else {
               c.text(title, bx + 36, yy + 6, (a << 24) | 0xFFFFFF, true);
               c.text(body, bx + 36, yy + 16, (a << 24) | 0x9AA6BA, false);
            }
            float rest = Math.max(0.0F, Math.min(1.0F, left / (float)t.durationMs()));
            c.roundRect(bx + 8, yy + H - 5, Math.max(2, Math.round((W - 16) * rest)), 2, 1.0F, (Math.round(a * 0.8F) << 24) | 0x3B82F6);
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
