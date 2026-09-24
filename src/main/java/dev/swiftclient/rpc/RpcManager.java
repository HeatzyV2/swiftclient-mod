package dev.swiftclient.rpc;

import dev.swiftclient.core.log.Log;
import org.slf4j.Logger;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.rpc.DiscordIpc;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;

public final class RpcManager {
   private static final Logger LOG = Log.get("Discord");
   /** Discord Application ID (Swift Client). */
   private static final String CLIENT_ID = "1552313392608448634";
   private static final String LARGE_IMG = "logo";
   private static final long START_TS = System.currentTimeMillis() / 1000L;
   private static final DiscordIpc ipc = new DiscordIpc(CLIENT_ID);
   private static String lastKey = null;
   private static int tickCounter = 0;
   private static final ExecutorService RPC_IO = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "SwiftClient-RPC-IO");
      t.setDaemon(true);
      return t;
   });

   public static void init() {
      new Thread(() -> {
         if (ipc.connect()) {
            LOG.info("Discord RPC connecte");
            update(Minecraft.getInstance());
         } else {
            LOG.info("Discord RPC indisponible (Discord non lance ?)");
         }
      }, "SwiftClient-RPC-Connect").start();
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         if (++tickCounter >= 100) {
            tickCounter = 0;
            if (ipc.isConnected()) {
               update(client);
            }
         }
      });
      Runtime.getRuntime().addShutdownHook(new Thread(ipc::close, "SwiftClient-RPC-Shutdown"));
   }

   /** Force refresh after language change. */
   public static void refresh() {
      lastKey = null;
      if (ipc.isConnected()) {
         update(Minecraft.getInstance());
      }
   }

   private static void update(Minecraft mc) {
      if (mc != null && !ModuleManager.active("discord_rpc")) {
         if (!"off".equals(lastKey)) {
            lastKey = "off";
            RPC_IO.execute(ipc::clearActivity);
         }
      } else if (mc != null) {
         String details = I18n.get("swift.rpc.details");
         String largeText = I18n.get("swift.rpc.large_text");
         ServerData server = mc.getCurrentServer();
         String state;
         if (mc.level != null && server != null && !mc.hasSingleplayerServer()) {
            ModuleSetting hide = ModuleManager.byId("discord_rpc").setting("hide_ip");
            state = hide == null || hide.boolValue() ? I18n.get("swift.rpc.on_server_hidden") : I18n.get("swift.rpc.on_server", server.ip);
         } else if (mc.level != null) {
            String worldName = I18n.get("swift.rpc.world");
            if (mc.getSingleplayerServer() != null) {
               try {
                  worldName = mc.getSingleplayerServer().getWorldData().getLevelName();
               } catch (Throwable ignored) {
               }
            }

            state = I18n.get("swift.rpc.singleplayer", worldName);
         } else {
            state = I18n.get("swift.rpc.in_menu");
         }

         String lang = mc.options.languageCode;
         String key = lang + "|" + details + "|" + state;
         if (!key.equals(lastKey)) {
            lastKey = key;
            String d = details;
            String s = state;
            String lt = largeText;
            RPC_IO.execute(() -> ipc.setActivity(d, s, LARGE_IMG, lt, START_TS));
         }
      }
   }
}
