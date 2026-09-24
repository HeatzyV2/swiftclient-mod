package dev.swiftclient.relay;

import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.net.Endpoints;
import org.slf4j.Logger;
import dev.swiftclient.core.relay.RelayClient;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public class InviteManager {
   private static final Logger LOG = Log.get("Relay");
   private static RelayClient activeRelay;
   private static int activePublicPort = -1;
   private static int activeLanPort = -1;

   public static CompletableFuture<String> startSession(String modeJeu, String difficulte, boolean triche) {
      CompletableFuture<String> result = new CompletableFuture<>();
      IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
      if (server == null) {
         result.completeExceptionally(new IllegalStateException(Tr.of("swift.host.err.no_world")));
         return result;
      } else if (Endpoints.relay() == null) {
         result.completeExceptionally(new IllegalStateException(Tr.of("swift.host.err.no_relay")));
         return result;
      } else if (activeRelay != null && activeRelay.isAlive() && activePublicPort > 0) {
         result.complete(adresse(activePublicPort));
         return result;
      } else {
         int freePort;
         try (ServerSocket ss = new ServerSocket(0)) {
            freePort = ss.getLocalPort();
         } catch (IOException ignored) {
            result.completeExceptionally(new RuntimeException(Tr.of("swift.host.err.no_port")));
            return result;
         }
         GameType mode = switch (modeJeu) {
            case "creative" -> GameType.CREATIVE;
            case "adventure" -> GameType.ADVENTURE;
            case "spectator" -> GameType.SPECTATOR;
            default -> GameType.SURVIVAL;
         };
         boolean ok = server.publishServer(net.minecraft.server.MinecraftServer.MultiplayerScope.LAN, mode, triche, freePort);
         if (!ok) {
            result.completeExceptionally(new RuntimeException(Tr.of("swift.host.err.refused", freePort)));
            return result;
         } else {
            activeLanPort = freePort;

            Difficulty diff = switch (difficulte) {
               case "peaceful" -> Difficulty.PEACEFUL;
               case "easy" -> Difficulty.EASY;
               case "hard" -> Difficulty.HARD;
               default -> Difficulty.NORMAL;
            };
            server.execute(() -> server.setDifficulty(diff, true));
            activeRelay = new RelayClient(activeLanPort, port -> {
               activePublicPort = port;
               result.complete(adresse(port));
            }, err -> {
               LOG.warn("Relais : {}", err);
               if (!result.isDone()) {
                  result.completeExceptionally(new RuntimeException(Tr.of("swift.host.err.relay_down")));
               }
            });
            activeRelay.start();
            return result;
         }
      }
   }

   public static void stop() {
      if (activeRelay != null) {
         activeRelay.stop();
         activeRelay = null;
      }

      activePublicPort = -1;
      activeLanPort = -1;
   }

   public static String currentAddress() {
      IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
      if (server == null && activeRelay != null) {
         stop();
      }

      return activeRelay != null && activeRelay.isAlive() && activePublicPort > 0 ? adresse(activePublicPort) : null;
   }

   private static String adresse(int port) {
      return Endpoints.relayHost() + ":" + port;
   }
}
