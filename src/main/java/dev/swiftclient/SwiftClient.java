package dev.swiftclient;

import dev.swiftclient.core.DisplayModeFix;
import dev.swiftclient.core.account.AccountManager;
import dev.swiftclient.core.music.MusicState;
import dev.swiftclient.core.partner.PartnerServers;
import dev.swiftclient.core.partner.PartnerTracking;
import dev.swiftclient.core.pet.DynamicPets;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.hud.HudClient;
import dev.swiftclient.net.CondorNet;
import dev.swiftclient.pet.DynamicPetLoader;
import dev.swiftclient.pet.PetManager;
import dev.swiftclient.platform.GameImpl;
import dev.swiftclient.rpc.RpcManager;
import dev.swiftclient.ui.PlayerCache;
import dev.swiftclient.ui.SkinRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.minecraft.client.multiplayer.ServerData;

public class SwiftClient implements ClientModInitializer {
   public void onInitializeClient() {
      System.out.println("[SwiftClient] ===== BUILD 2026-07-22l · Cape realiste : positionnement WaveyCapes (matrices par segment, 26.2) =====");
      Platform.install(new GameImpl());
      RpcManager.init();
      AccountManager.get().load();
      PetManager.INSTANCE.init();
      DynamicPets.setSink(new DynamicPetLoader());
      HudClient.init();
      MusicState.boot();
      // CondorNet disabled in Swift Client
      // PartnerServers disabled
      ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> {
         ServerData server = client.getCurrentServer();
         // Partner tracking removed
      });
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         DisplayModeFix.tick();
         if (client.player != null) {
            PlayerCache.set(client.player);
         }

         if (SkinRenderer.getSkinTextureId() == null) {
            SkinRenderer.preloadSkin();
         }
      });
   }
}
