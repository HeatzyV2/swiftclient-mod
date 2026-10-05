package dev.swiftclient.core.screen;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Tr;
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
      int h = cols == 1 ? 62 : Math.min(120, this.height - top - 16);

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
   }
}
