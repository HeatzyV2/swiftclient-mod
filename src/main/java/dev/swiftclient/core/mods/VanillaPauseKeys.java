package dev.swiftclient.core.mods;

import java.util.Locale;
import java.util.Set;

public final class VanillaPauseKeys {
   public static final int MAX_INTEGRATE = 13;
   private static final Set<String> KEYS = Set.of(
      "menu.returnToGame",
      "gui.advancements",
      "gui.stats",
      "menu.sendFeedback",
      "menu.reportBugs",
      "menu.feedback",
      "menu.options",
      "menu.shareToLan",
      "menu.returnToMenu",
      "menu.disconnect",
      "menu.playerReporting"
   );
   private static final Set<String> EXTRA_KEYS = Set.of("modmenu.title");
   private static final Set<String> EXTRA_LABELS = Set.of("mods", "friends", "amis", "amies");

   private VanillaPauseKeys() {
   }

   public static boolean isVanilla(String translationKey) {
      return translationKey != null && KEYS.contains(translationKey);
   }

   public static boolean shouldRestyle(String translationKey, String label) {
      if (isVanilla(translationKey)) {
         return true;
      } else {
         return translationKey != null && EXTRA_KEYS.contains(translationKey)
            ? true
            : label != null && EXTRA_LABELS.contains(label.trim().toLowerCase(Locale.ROOT));
      }
   }
}
