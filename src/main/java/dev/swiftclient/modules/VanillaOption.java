package dev.swiftclient.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.platform.Platform;
import net.minecraft.client.OptionInstance;

/**
 * Lets a module take over a vanilla boolean option and give it back. The player's own value is kept in
 * swiftclient.properties ({@code mod.<id>.vanilla_prev}) until the module is switched off, so it is
 * restored even if the game was closed with the module on.
 */
final class VanillaOption {
   private VanillaOption() {
   }

   static void force(Module m, OptionInstance<Boolean> option, boolean value) {
      String key = key(m);
      if (Platform.game().getConfig(key, null) == null) {
         Platform.game().setConfig(key, Boolean.toString(option.get()));
      }

      if (option.get() != value) {
         option.set(value);
      }
   }

   static void restore(Module m, OptionInstance<Boolean> option) {
      String key = key(m);
      String prev = Platform.game().getConfig(key, null);
      if (prev != null) {
         option.set(Boolean.parseBoolean(prev));
         Platform.game().setConfig(key, null);
      }
   }

   private static String key(Module m) {
      return "mod." + m.id + ".vanilla_prev";
   }
}
