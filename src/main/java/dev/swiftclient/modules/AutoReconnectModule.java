package dev.swiftclient.modules;

import dev.swiftclient.compat.Mc;
import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

/**
 * After an unexpected disconnect, a Reconnect button counts down and joins the last server again. Off, the
 * button stays (without the countdown) so a reconnect is always one click away.
 */
@ConsumedBy("DisconnectedScreenMixin")
public final class AutoReconnectModule extends Module {
   public final ModuleSetting delay;
   private ServerData lastServer;
   private Screen activeScreen;
   private Button button;
   private int ticksLeft;

   public AutoReconnectModule() {
      super("auto_reconnect", "Auto Reconnect", "Reconnects to the last server after an unexpected disconnect, after a short countdown.", "Interface", "reload", false);
      this.delay = this.slider("delay", "Delay", 5.0, 3.0, 60.0, 1.0, "s").desc("Time before reconnecting. Press Back to cancel.");
   }

   /** Remembers the server while you play on it (any state of the module, for the manual button). */
   public static void track() {
      if (!ModuleManager.isLoaded()) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      AutoReconnectModule m = ModuleManager.get(AutoReconnectModule.class);
      if (mc.player != null && mc.level != null) {
         ServerData server = mc.isLocalServer() ? null : mc.getCurrentServer();
         m.lastServer = server != null && !server.isLan() && !server.isRealm() ? server : null;
      } else if (Mc.screen() instanceof TitleScreen || Mc.screen() instanceof JoinMultiplayerScreen) {
         m.lastServer = null;
      }
   }

   /** The disconnect screen was (re)built: the button to add to it, or null (DisconnectedScreenMixin). */
   public static Button reconnectButton(DisconnectedScreen screen) {
      if (!ModuleManager.isLoaded()) {
         return null;
      }

      AutoReconnectModule m = ModuleManager.get(AutoReconnectModule.class);
      ServerData server = m.lastServer;
      if (server == null) {
         return null;
      }

      if (screen != m.activeScreen) {
         m.activeScreen = screen;
         m.ticksLeft = (int)Math.round(m.delay.value() * 20.0);
      }

      Minecraft mc = Minecraft.getInstance();
      m.button = Button.builder(m.label(), b -> {
         b.active = false;
         reconnect(mc, server);
      }).bounds(screen.width - 160, screen.height - 30, 150, 20).build();
      return m.button;
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (Mc.screen() != this.activeScreen || this.activeScreen == null || this.button == null || this.lastServer == null) {
         return;
      }

      if (--this.ticksLeft <= 0) {
         ServerData server = this.lastServer;
         this.button.active = false;
         this.activeScreen = null;
         reconnect(mc, server);
      } else {
         this.button.setMessage(this.label());
      }
   }

   private Component label() {
      return this.isEnabled()
         ? Component.literal(String.format(Locale.ROOT, "Reconnect (%.0f)", Math.ceil(this.ticksLeft / 20.0)))
         : Component.literal("Reconnect");
   }

   private static void reconnect(Minecraft mc, ServerData server) {
      ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), mc, ServerAddress.parseString(server.ip), server, false, null);
   }
}
