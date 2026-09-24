package dev.swiftclient;

import dev.swiftclient.core.DisplayModeFix;
import dev.swiftclient.core.badges.BadgeState;
import dev.swiftclient.core.cosmetics.CosmeticState;
import dev.swiftclient.core.cosmetics.OfflineNames;
import dev.swiftclient.core.cosmetics.PetState;
import dev.swiftclient.core.mods.CapeSimManager;
import dev.swiftclient.core.account.AccountManager;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.music.MusicState;
import dev.swiftclient.core.music.WindowsSmtc;
import dev.swiftclient.core.pet.DynamicPets;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.hud.HudClient;
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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.minecraft.client.player.AbstractClientPlayer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SwiftClient implements ClientModInitializer {
   /** Every 30 s, per-player caches drop the players that left the world. */
   private static final int PURGE_INTERVAL_TICKS = 600;
   private static int purgeTicks;

   private static void forgetPlayers() {
      PlayerCache.clear();
      CapeSimManager.clear();
      BadgeState.clear();
      CosmeticState.clear();
      PetState.clear();
   }

   public void onInitializeClient() {
      System.out.println("[SwiftClient] ===== BUILD 2026-07-22l · Cape realiste : positionnement WaveyCapes (matrices par segment, 26.2) =====");
      Platform.install(new GameImpl());
      RpcManager.init();
      AccountManager.get().load();
      PetManager.INSTANCE.init();
      DynamicPets.setSink(new DynamicPetLoader());
      HudClient.init();
      MusicState.boot();
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> forgetPlayers());
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         DisplayModeFix.tick();
         WindowsSmtc.setActive(ModuleManager.active("hud_music"));
         if (client.player != null) {
            PlayerCache.set(client.player);
         } else if (client.level == null) {
            // Out of any world: never keep the last LocalPlayer (and its ClientLevel) alive.
            PlayerCache.clear();
         }

         if (client.level != null && ++purgeTicks >= PURGE_INTERVAL_TICKS) {
            purgeTicks = 0;
            Set<UUID> present = new HashSet<>(OfflineNames.selfUuids());
            for (AbstractClientPlayer p : client.level.players()) {
               present.add(p.getUUID());
            }

            BadgeState.retainOnly(present);
            CosmeticState.retainOnly(present);
            PetState.retainOnly(present);
         }

         if (SkinRenderer.getSkinTextureId() == null) {
            SkinRenderer.preloadSkin();
         }
      });
   }
}
