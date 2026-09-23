package dev.swiftclient.core.hud;

public final class HudVisibilite {
   private HudVisibilite() {
   }

   public static boolean afficher(boolean ecranOuvert, boolean estLeChat) {
      return !ecranOuvert || estLeChat;
   }
}
