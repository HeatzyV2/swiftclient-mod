package dev.swiftclient.core.screen;

import dev.swiftclient.core.combat.CombatRecap;
import dev.swiftclient.core.combat.FightLog;
import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.mods.modules.CombatRecapModule;
import dev.swiftclient.core.session.SessionGraph;
import dev.swiftclient.core.session.SessionStats;
import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.ui.UiScreen;

/** The Session page: this session, the one before, and all time, as three stat cards with Zip next to them. */
public class SessionScreen extends UiScreen {
   private static final int PAD = 14;
   private final long opened = System.nanoTime();

   @Override
   public String title() {
      return Tr.of("swift.session.title");
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      long t = (System.nanoTime() - this.opened) / 1_000_000L;
      Px.title(c, this.title(), PAD, 10);
      Px.zip(c, this.width - PAD - 30, 4, 1, SessionStats.currentSeconds() > 0 && t % 4000L < 2000L, t);

      int gap = 8;
      int top = 40;
      int cols = this.width >= 330 ? 3 : 1;
      int w = (this.width - PAD * 2 - gap * (cols - 1)) / cols;
      int h = cols == 1 ? 62 : Math.min(100, this.height - top - 16);

      boolean any = SessionStats.totalSeconds() > 0L;
      String[][] cards = {
         {Tr.of("swift.session.this"), SessionStats.duration(SessionStats.currentSeconds()), SessionStats.distance(SessionStats.currentDistance())},
         {Tr.of("swift.session.last"), SessionStats.duration(SessionStats.previousSeconds()), SessionStats.distance(SessionStats.previousDistance())},
         {Tr.of("swift.session.total"), SessionStats.duration(SessionStats.totalSeconds()), SessionStats.distance(SessionStats.totalDistance())},
      };
      for (int i = 0; i < cards.length; i++) {
         int col = i % cols;
         int row = i / cols;
         int x = PAD + col * (w + gap);
         int y = top + row * (h + gap + 4);
         boolean live = i == 0;
         c.card(x, y, w, h, 0xFF11151F, live ? Px.ACCENT : 0, live ? 1 : 0, 4.0F);
         c.text(cards[i][0].toUpperCase(java.util.Locale.ROOT), x + 10, y + 9, live ? Px.ACCENT_HI : Px.FAINT, false);
         Px.streak(c, x + 10, y + 21);
         // Big numbers at twice the size
         c.pushScale(x + 10, y + 32, 2.0F);
         c.text(cards[i][1], 0, 0, Px.TEXT, true);
         c.popScale();
         c.text(Tr.of("swift.session.time"), x + 10, y + 52, Px.FAINT, false);
         c.pushScale(x + 10, y + 68, 2.0F);
         c.text(cards[i][2], 0, 0, Px.DIM, true);
         c.popScale();
         c.text(Tr.of("swift.session.distance"), x + 10, y + 88, Px.FAINT, false);
      }

      int footY = top + ((cards.length + cols - 1) / cols) * (h + gap + 4) + 4;
      String sessions = Tr.of("swift.session.count", SessionStats.sessions());
      c.text(sessions, PAD, Math.min(footY, this.height - 14), Px.FAINT, false);
      if (!any) {
         c.text(Tr.of("swift.session.none"), PAD, Math.min(footY + 12, this.height - 26), Px.DIM, false);
      }

      // Performance graphs and the last fight, when the page is tall enough to hold them
      int y = footY + (any ? 16 : 28);
      int graphH = 30;
      int gw = this.width - PAD * 2;
      if (SessionGraph.size() >= 2 && y + graphH * 2 + 20 <= this.height) {
         float[] fps = SessionGraph.fps();
         float[] ping = SessionGraph.ping();
         spark(c, PAD, y, gw, graphH, fps, Px.ACCENT_HI, Tr.of("swift.session.graph.fps"), Math.round(avg(fps)) + " FPS");
         y += graphH + 8;
         if (max(ping) >= 0.0F) {
            spark(c, PAD, y, gw, graphH, ping, 0xFF6BD36B, Tr.of("swift.session.graph.ping"), Math.round(avg(ping)) + " ms");
            y += graphH + 8;
         }
      }

      CombatRecap.Summary fight = FightLog.RECAP.last();
      if (fight != null && y + 22 <= this.height) {
         c.text(Tr.of("swift.session.fight").toUpperCase(java.util.Locale.ROOT), PAD, y, Px.FAINT, false);
         c.text(CombatRecapModule.line(fight), PAD, y + 11, Px.DIM, false);
      }
   }

   private static float avg(float[] v) {
      float sum = 0.0F;
      int n = 0;
      for (float f : v) {
         if (f >= 0.0F) {
            sum += f;
            n++;
         }
      }

      return n == 0 ? 0.0F : sum / n;
   }

   private static float max(float[] v) {
      float m = -1.0F;
      for (float f : v) {
         m = Math.max(m, f);
      }

      return m;
   }

   /** A filled sparkline: one column per sample, scaled to the series' own maximum, with its average on the right. */
   private static void spark(Canvas c, int x, int y, int w, int h, float[] data, int color, String label, String stat) {
      c.card(x, y, w, h, 0xFF11151F, 0, 0, 4.0F);
      c.text(label.toUpperCase(java.util.Locale.ROOT), x + 8, y + 5, Px.FAINT, false);
      c.text(stat, x + w - 8 - c.textWidth(stat), y + 5, Px.TEXT, false);
      float top = Math.max(1.0F, max(data));
      int plotX = x + 8;
      int plotW = w - 16;
      int plotTop = y + 16;
      int plotH = h - 20;
      int n = data.length;
      for (int i = 0; i < plotW; i++) {
         int idx = Math.min(n - 1, (int)((long)i * n / plotW));
         float v = data[idx];
         if (v >= 0.0F) {
            int bar = Math.max(1, Math.round(v / top * plotH));
            c.fill(plotX + i, plotTop + plotH - bar, plotX + i + 1, plotTop + plotH, color & 0x00FFFFFF | 0xCC000000);
         }
      }
   }
}
