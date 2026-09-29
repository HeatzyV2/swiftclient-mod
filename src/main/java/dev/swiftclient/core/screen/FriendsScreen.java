package dev.swiftclient.core.screen;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.social.Friends;
import dev.swiftclient.core.social.SocialApi;
import dev.swiftclient.core.ui.Defilement;
import dev.swiftclient.core.ui.Kit;
import dev.swiftclient.core.ui.UiScreen;
import java.util.ArrayList;
import java.util.List;

/** Friends list of the Swift account (shared with the launcher): add by name, remove, online state. */
public final class FriendsScreen extends UiScreen {
   private static final int ROW_H = 26;
   private static final int TOP = 70;
   private Kit.Zone z;
   private int height;
   private String input = "";
   private boolean inputFocused;
   private long caret;
   private String status = "";
   private long statusMs;
   private boolean busy;
   private final Defilement scroll = new Defilement();

   @Override
   public String title() {
      return Tr.of("swift.friends.title");
   }

   @Override
   public void layout(int width, int height) {
      super.layout(width, height);
      this.z = Kit.zone(width);
      this.height = height;
      Friends.refresh();
   }

   private int[] inputRect() {
      return new int[]{this.z.x(), 36, this.z.w() - 96, 20};
   }

   private int[] addRect() {
      return new int[]{this.z.right() - 88, 36, 88, 20};
   }

   private int[] removeRect(int y) {
      return new int[]{this.z.right() - 76, y + 5, 68, 16};
   }

   private int listBottom() {
      return this.height - 12;
   }

   @Override
   public void draw(Canvas c, int mouseX, int mouseY, float delta) {
      this.caret++;
      List<SocialApi.Friend> friends = sorted(Friends.list());
      long online = friends.stream().filter(SocialApi.Friend::online).count();
      Kit.titre(c, this.z, this.title());
      Kit.sousTitre(c, this.z, this.title(), Tr.of("swift.friends.count", friends.size(), online));
      int[] ir = this.inputRect();
      Kit.recherche(c, ir, this.input, this.inputFocused, this.caret, Tr.of("swift.friends.add_hint"), mouseX, mouseY);
      int[] ar = this.addRect();
      Kit.bouton(c, ar, this.busy ? Tr.of("swift.common.loading") : Tr.of("swift.friends.add"), Kit.dedans(mouseX, mouseY, ar));
      if (!this.status.isEmpty() && System.currentTimeMillis() - this.statusMs < 4000L) {
         c.text(Kit.tronque(c, this.status, this.z.w()), this.z.x(), 60, -6644317, false);
      }

      int top = TOP;
      int bottom = this.listBottom();
      if (friends.isEmpty()) {
         String msg = Friends.error() != null ? Friends.error() : (Friends.loading() ? Tr.of("swift.common.loading") : Tr.of("swift.friends.empty"));
         c.centeredText(msg, this.z.x() + this.z.w() / 2, top + 30, -10394518, false);
         return;
      }

      this.scroll.contenu(friends.size() * (ROW_H + 4), bottom - top);
      this.scroll.anime();
      int y = top - this.scroll.px();
      c.pushScissor(this.z.x(), top, this.z.w(), bottom - top);

      for (SocialApi.Friend f : friends) {
         if (y + ROW_H >= top && y <= bottom) {
            boolean over = Kit.dedans(mouseX, mouseY, this.z.x(), y, this.z.w(), ROW_H);
            Kit.ligne(c, this.z.x(), y, this.z.w(), ROW_H, false, over);
            if (f.uuid() != null) {
               c.playerHead(f.uuid(), this.z.x() + 7, y + 5, 16);
            }

            String name = Kit.tronque(c, f.username(), this.z.w() - 150);
            c.text(name, this.z.x() + 30, y + 9, -1, false);
            int dotX = this.z.x() + 30 + c.textWidth(name) + 6;
            c.roundRect(dotX, y + 11, 5, 5, 2.5F, f.online() ? -12868259 : -12960962);
            c.text(Tr.of(f.online() ? "swift.friends.online" : "swift.friends.offline"), dotX + 9, y + 9, -10394518, false);
            int[] rr = this.removeRect(y);
            Kit.bouton(c, rr, Tr.of("swift.friends.remove"), Kit.dedans(mouseX, mouseY, rr));
         }

         y += ROW_H + 4;
      }

      c.popScissor();
   }

   private static List<SocialApi.Friend> sorted(List<SocialApi.Friend> in) {
      List<SocialApi.Friend> out = new ArrayList<>(in);
      out.sort((a, b) -> a.online() != b.online() ? (a.online() ? -1 : 1) : a.username().compareToIgnoreCase(b.username()));
      return out;
   }

   private void say(String s) {
      this.status = s == null ? "" : s;
      this.statusMs = System.currentTimeMillis();
   }

   private void add() {
      String name = this.input.trim();
      if (name.length() < 3 || this.busy) {
         this.say(Tr.of("swift.friends.name_short"));
         return;
      }

      this.busy = true;
      SocialApi.requestFriendByName(name).whenComplete((res, t) -> {
         this.busy = false;
         if (t == null && res != null && res.ok()) {
            this.input = "";
            this.say(Tr.of("swift.friends.added", name));
            Friends.invalidate();
            Friends.refresh();
         } else {
            this.say(res != null && res.error() != null ? res.error() : Tr.of("swift.net.unreachable"));
         }
      });
   }

   private void remove(SocialApi.Friend f) {
      SocialApi.removeFriend(f).whenComplete((res, t) -> {
         if (t == null && res != null && res.ok()) {
            this.say(Tr.of("swift.friends.removed", f.username()));
            Friends.invalidate();
            Friends.refresh();
         } else {
            this.say(res != null && res.error() != null ? res.error() : Tr.of("swift.net.unreachable"));
         }
      });
   }

   @Override
   public boolean click(double mx, double my, int button) {
      if (button != 0) {
         return false;
      }

      this.inputFocused = Kit.dedans(mx, my, this.inputRect());
      if (this.inputFocused) {
         return true;
      } else if (Kit.dedans(mx, my, this.addRect())) {
         this.add();
         return true;
      } else if (my >= TOP && my < this.listBottom()) {
         int y = TOP - this.scroll.px();
         for (SocialApi.Friend f : sorted(Friends.list())) {
            if (Kit.dedans(mx, my, this.removeRect(y))) {
               this.remove(f);
               return true;
            }

            y += ROW_H + 4;
         }
      }

      return false;
   }

   @Override
   public boolean scroll(double mouseX, double mouseY, double amount) {
      this.scroll.cran(amount, 28.0);
      return true;
   }

   @Override
   public boolean charTyped(String s) {
      if (this.inputFocused && this.input.length() < 32) {
         this.input = this.input + s;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean keyPressed(int keyCode) {
      if (!this.inputFocused) {
         return false;
      } else if (keyCode == 259) {
         if (!this.input.isEmpty()) {
            this.input = this.input.substring(0, this.input.length() - 1);
         }

         return true;
      } else if (keyCode == 257 || keyCode == 335) {
         this.add();
         return true;
      } else if (keyCode == 256) {
         this.inputFocused = false;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean closeOnEscape() {
      return !this.inputFocused;
   }
}
