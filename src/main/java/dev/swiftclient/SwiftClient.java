package dev.swiftclient;

import dev.swiftclient.core.DisplayModeFix;
import dev.swiftclient.core.theme.SwiftPanorama;
import dev.swiftclient.core.badges.BadgeState;
import dev.swiftclient.core.cosmetics.CosmeticState;
import dev.swiftclient.core.cosmetics.OfflineNames;
import dev.swiftclient.core.cosmetics.PetState;
import dev.swiftclient.core.mods.CapeSimManager;
import dev.swiftclient.core.account.AccountManager;
import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.music.MusicState;
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
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.MixinEnvironment;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SwiftClient implements ClientModInitializer {
   /** Every 30 s, per-player caches drop the players that left the world. */
   private static final int PURGE_INTERVAL_TICKS = 600;
   private static int purgeTicks;

   private static boolean mixinAuditDone;

   /**
    * Dev check ({@code -Dswiftclient.auditMixins=true}): loads every mixin target class so an
    * injection that no longer matches fails now instead of the first time a screen opens.
    */
   private static void auditMixins() {
      Log.ROOT.info("Audit des mixins...");
      try {
         MixinEnvironment.getCurrentEnvironment().audit();
         Log.ROOT.info("Audit des mixins termine sans erreur");
      } catch (Throwable t) {
         Log.ROOT.error("Audit des mixins en echec", t);
      }
   }

   private static void forgetPlayers() {
      PlayerCache.clear();
      CapeSimManager.clear();
      BadgeState.clear();
      CosmeticState.clear();
      PetState.clear();
   }

   public void onInitializeClient() {
      Log.ROOT.info(
         "Swift Client {} (Minecraft {})",
         FabricLoader.getInstance().getModContainer("swiftclient").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?"),
         FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?")
      );
      Platform.install(new GameImpl());
      HudClient.installModules();
      RpcManager.init();
      AccountManager.get().load();
      PetManager.INSTANCE.init();
      DynamicPets.setSink(new DynamicPetLoader());
      HudClient.init();
      MusicState.boot();
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> forgetPlayers());
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         if (!mixinAuditDone && Boolean.getBoolean("swiftclient.auditMixins")) {
            mixinAuditDone = true;
            auditMixins();
         }

         DisplayModeFix.tick();
         if (client.gui.overlay() == null) {
            SwiftPanorama.applyWhenReady();
         }
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
