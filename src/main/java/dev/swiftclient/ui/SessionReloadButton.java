package dev.swiftclient.ui;

import dev.swiftclient.compat.Mc;
import dev.swiftclient.core.account.AccountEntry;
import dev.swiftclient.core.account.AccountManager;
import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.ui.widget.CoreWidget;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/**
 * The "reload session" button of the server list: asks Microsoft for a fresh token for the active Swift
 * account and applies it, so an expired session no longer needs a game restart.
 */
public final class SessionReloadButton {
   public static final int SIZE = 20;

   private SessionReloadButton() {
   }

   public static CanvasWidget create(int x, int y) {
      CoreWidget face = new CoreWidget() {
         @Override
         public void draw(Canvas c, int bx, int by, int w, int h, boolean hovered, boolean active, int mouseX, int mouseY, float delta) {
            c.vanillaButton(bx, by, w, h, hovered);
            c.icon("reload", bx + 3, by + 3, w - 6, hovered ? -1 : -1275068417);
         }

         @Override
         public void onClick() {
            reload();
         }

         @Override
         public String tooltip() {
            return Tr.of("swift.session.reload.tooltip");
         }
      };
      return new CanvasWidget(x, y, SIZE, SIZE, Component.literal(Tr.of("swift.session.reload.tooltip")), face);
   }

   private static void reload() {
      Optional<AccountEntry> active = AccountManager.get().getActive();
      if (active.isEmpty()) {
         toast(Tr.of("swift.session.reload.none"));
         return;
      }

      AccountEntry account = active.get();
      AccountManager.get().refreshAndSwitch(account, ok -> toast(ok
         ? Tr.of("swift.session.reload.ok", account.getUsername())
         : Tr.of("swift.session.reload.failed")));
   }

   private static void toast(String message) {
      Minecraft mc = Minecraft.getInstance();
      SystemToast.addOrUpdate(Mc.toasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.literal(Tr.of("swift.session.reload.title")), Component.literal(message));
   }
}
