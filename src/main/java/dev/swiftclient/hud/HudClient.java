package dev.swiftclient.hud;

import dev.swiftclient.core.cosmetics.CapeUploadQueue;
import dev.swiftclient.core.cosmetics.CosmeticHttp;
import dev.swiftclient.core.cosmetics.HeartbeatManager;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.hud.HudVisibilite;
import dev.swiftclient.core.mods.ModsScreen;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.input.SwiftKeys;
import dev.swiftclient.modules.AutoJumpModule;
import dev.swiftclient.modules.FreelookModule;
import dev.swiftclient.modules.FullBrightModule;
import dev.swiftclient.modules.NoRainModule;
import dev.swiftclient.modules.RealisticCapeModule;
import dev.swiftclient.modules.ToggleSneakModule;
import dev.swiftclient.modules.ToggleSprintModule;
import dev.swiftclient.modules.ZoomModule;
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
      ModuleManager.install(
         () -> List.of(
            new ZoomModule(),
            new FullBrightModule(),
            new RealisticCapeModule(),
            new ToggleSprintModule(),
            new ToggleSneakModule(),
            new AutoJumpModule(),
            new NoRainModule(),
            new FreelookModule()
         )
      );
   }

   public static void init() {
      SwiftKeys.register();
      if (CosmeticHttp.backendConfigured()) {
         HeartbeatManager.start(() -> Minecraft.getInstance().player != null);
      }

      ClientTickEvents.END_CLIENT_TICK.register((EndTick)mc -> {
         // First tick: options and window exist, modules that start enabled can apply themselves.
         ModuleManager.start();
         CapeUploadQueue.drain(8);

         while (SwiftKeys.MENU.consumeClick()) {
            if (mc.gui.screen() == null && mc.player != null) {
               mc.setScreenAndShow(new CoreScreenHost(new ModsScreen(), null));
            }
         }

         ModuleManager.tick();
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
