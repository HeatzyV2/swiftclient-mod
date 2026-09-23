package dev.swiftclient.core.theme;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Platform;
import java.util.List;

public final class ThemeManager {
   private static final String KEY = "theme.current";
   private static final String DEFAULT_ID = "bloomy";
   private static final long DURATION = 1600L;
   private static String currentId;
   private static String pendingId;
   private static long startMs;

   private ThemeManager() {
   }

   public static String currentId() {
      if (currentId == null) {
         currentId = Platform.game().getConfig("theme.current", "");
         if (currentId.isEmpty() || Themes.byId(currentId) == null) {
            List<Theme> all = Themes.all();
            currentId = Themes.byId("bloomy") != null ? "bloomy" : (all.isEmpty() ? "" : all.get(0).id());
         }
      }

      return currentId;
   }

   public static void select(String id) {
      if (id != null && !id.equals(currentId()) && Themes.byId(id) != null) {
         pendingId = id;
         startMs = now();
         Platform.game().playClick();
      }
   }

   private static long now() {
      return System.currentTimeMillis();
   }

   private static float progress() {
      if (startMs == 0L) {
         return -1.0F;
      } else {
         float p = (float)(now() - startMs) / 1600.0F;
         if (p >= 0.5F && pendingId != null) {
            commit(pendingId);
            pendingId = null;
         }

         if (p >= 1.0F) {
            startMs = 0L;
            return -1.0F;
         } else {
            return p;
         }
      }
   }

   private static void commit(String id) {
      currentId = id;
      Platform.game().setConfig("theme.current", id);
      Theme t = Themes.byId(id);
      if (t != null && t.panorama() != null && !t.panorama().isBlank()) {
         Platform.game().applyPanorama(t.panorama());
      }
   }

   public static String currentPanorama() {
      Theme t = Themes.byIdOrFirst(currentId());
      return t == null ? "" : (t.panorama() == null ? "" : t.panorama());
   }

   public static boolean isAnimating() {
      return startMs != 0L;
   }

   public static void tick() {
      progress();
   }

   public static void drawBackground(Canvas c, int w, int h) {
      progress();
      Theme t = Themes.byId(currentId());
      if (t == null) {
         c.fill(0, 0, w, h, -15723496);
      } else {
         if (t.top() == t.bottom()) {
            c.fill(0, 0, w, h, t.top());
         } else {
            c.gradientV(0, 0, w, h, t.top(), t.bottom());
         }
      }
   }

   public static void drawCurtain(Canvas c, int w, int h) {
      float p = progress();
      if (!(p < 0.0F)) {
         float tri = p < 0.5F ? p * 2.0F : (1.0F - p) * 2.0F;
         float cover = easeInOut(tri);
         int half = Math.round(cover * (w / 2.0F + 2.0F));
         if (half > 0) {
            drawPanel(c, 0, half, h, false);
            drawPanel(c, w - half, half, h, true);
         }
      }
   }

   private static void drawPanel(Canvas c, int x, int wdt, int h, boolean rightSide) {
      if (wdt > 0) {
         c.gradientV(x, 0, wdt, h, -15133935, -16251387);
         int folds = Math.max(3, wdt / 24);

         for (int i = 1; i < folds; i++) {
            int fx = x + Math.round((float)i * wdt / folds);
            c.fill(fx - 1, 0, fx + 1, h, 771751936);
            c.fill(fx + 1, 0, fx + 2, h, 318767103);
         }

         for (int s = 0; s < 8; s++) {
            int alpha = (8 - s) * 5;
            int col = rightSide ? x + s : x + wdt - 1 - s;
            c.fill(col, 0, col + 1, h, alpha << 24);
         }
      }
   }

   private static float easeInOut(float t) {
      return t < 0.5F ? 2.0F * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 2.0) / 2.0F;
   }
}
