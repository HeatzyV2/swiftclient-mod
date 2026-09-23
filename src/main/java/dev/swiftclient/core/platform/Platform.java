package dev.swiftclient.core.platform;

public final class Platform {
   private static Game game;

   private Platform() {
   }

   public static void install(Game g) {
      game = g;
   }

   public static Game game() {
      if (game == null) {
         throw new IllegalStateException("Platform.install(Game) n'a pas ete appele : l'entrypoint de cette version de Minecraft doit le faire au demarrage.");
      } else {
         return game;
      }
   }
}
