package dev.swiftclient.hud;

import dev.swiftclient.core.cosmetics.CapeUploadQueue;
import dev.swiftclient.core.cosmetics.CosmeticHttp;
import dev.swiftclient.core.cosmetics.HeartbeatManager;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.hud.HudVisibilite;
import dev.swiftclient.core.mods.ModsScreen;
import dev.swiftclient.core.screen.RadialScreen;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.input.SwiftKeys;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.modules.*;
import dev.swiftclient.pvp.CombatTracker;
import dev.swiftclient.world.WorldEditCui;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import dev.swiftclient.platform.CanvasImpl;
import dev.swiftclient.platform.CoreScreenHost;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/** Wires modules, key bindings and the HUD into the game. Module behaviour lives in the modules. */
public final class HudClient {
   private static final HudDataImpl HUD_DATA = new HudDataImpl();

   private HudClient() {
   }

   /** Declared first thing at startup: other systems (RPC thread...) may read modules before init(). */
   public static void installModules() {
      ModuleManager.install(PlatformModules::all);
   }

   /** Development: -Dswiftclient.devOpen=mods|multiplayer opens that screen over the title screen once. */
   private static final String DEV_OPEN = System.getProperty("swiftclient.devOpen");
   private static boolean devOpened;
   /** Development: -Dswiftclient.devShots=true takes screenshots of the world then of the mods menu. */
   private static final boolean DEV_SHOTS = Boolean.getBoolean("swiftclient.devShots");
   private static int devShotTicks;

   private static void devShots(Minecraft mc) {
      if (!DEV_SHOTS || mc.player == null) {
         return;
      }

      devShotTicks++;
      if (devShotTicks == 200 || devShotTicks == 300) {
         net.minecraft.client.Screenshot.grab(mc, false);
      } else if (devShotTicks == 240) {
         mc.player.setYRot(mc.player.getYRot() + 90.0F);
      } else if (devShotTicks == 340) {
         mc.setScreenAndShow(new CoreScreenHost(new ModsScreen(), null));
      } else if (devShotTicks == 380) {
         net.minecraft.client.Screenshot.grab(mc, false);
      }
   }

   public static void init() {
      SwiftKeys.register();
      WorldEditCui.register();
      ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(WorldEditCui::handshake));
      if (CosmeticHttp.backendConfigured()) {
         HeartbeatManager.start(() -> Minecraft.getInstance().player != null);
      }

      ClientTickEvents.END_CLIENT_TICK.register((EndTick)mc -> {
         // First tick: options and window exist, modules that start enabled can apply themselves.
         ModuleManager.start();
         CapeUploadQueue.drain(8);

         if (DEV_OPEN != null && !devOpened && mc.gui.screen() instanceof net.minecraft.client.gui.screens.TitleScreen title) {
            devOpened = true;
            if (DEV_OPEN.equals("multiplayer")) {
               mc.setScreenAndShow(new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(title));
            } else {
               mc.setScreenAndShow(new CoreScreenHost(new ModsScreen(), title));
            }
         }

         while (SwiftKeys.MENU.consumeClick()) {
            if (mc.gui.screen() == null && mc.player != null) {
               mc.setScreenAndShow(new CoreScreenHost(new ModsScreen(), null));
            }
         }

         while (SwiftKeys.RADIAL.consumeClick()) {
            if (mc.gui.screen() == null && mc.player != null) {
               mc.setScreenAndShow(new CoreScreenHost(new RadialScreen(), null));
            }
         }

         ModuleManager.tick();
         devShots(mc);
         CombatTracker.tick();
         PickupTracker.tick(ModuleManager.active("hud_itemtracker"));
      });
      HudElementRegistry.attachElementAfter(
         VanillaHudElements.MISC_OVERLAYS, Identifier.fromNamespaceAndPath("swiftclient", "hud"), (graphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            ModuleManager.render(deltaTracker.getGameTimeDeltaPartialTick(false));
            Screen ecran = mc.gui.screen();
            if (HudVisibilite.afficher(ecran != null, ecran instanceof ChatScreen)) {
               HudManager.setGuiScale(mc.getWindow().getGuiScale());
               HudManager.render(new CanvasImpl(graphics), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), HUD_DATA);
            }
         }
      );
   }
}
