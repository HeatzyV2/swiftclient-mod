package dev.swiftclient.testing;

import dev.swiftclient.core.config.ConfigStore;
import dev.swiftclient.core.platform.Game;
import dev.swiftclient.core.platform.Platform;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Minimal Game for unit tests: an in-memory config map, everything else inert. */
public final class TestGame {
   public static final Map<String, String> CONFIG = new HashMap<>();
   /** Profiles and other structured config go to a real store in a fresh temporary folder. */
   public static ConfigStore store;

   public static final String[] CLIPBOARD = {""};

   private TestGame() {
   }

   public static void install() {
      CONFIG.clear();

      try {
         store = new ConfigStore(Files.createTempDirectory("swiftclient-test"));
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }

      Game game = (Game)Proxy.newProxyInstance(Game.class.getClassLoader(), new Class<?>[]{Game.class}, (proxy, method, args) -> switch (method.getName()) {
         case "getConfig" -> CONFIG.getOrDefault((String)args[0], (String)args[1]);
         case "setConfig" -> {
            if (args[1] == null) {
               CONFIG.remove((String)args[0]);
            } else {
               CONFIG.put((String)args[0], (String)args[1]);
            }

            yield null;
         }
         case "configDir" -> Path.of("build", "test-config");
         case "config" -> store;
         case "readClipboard" -> CLIPBOARD[0];
         case "copyToClipboard" -> {
            CLIPBOARD[0] = (String)args[0];
            yield null;
         }
         default -> method.getReturnType() == boolean.class ? false : method.getReturnType() == int.class ? 0 : null;
      });
      Platform.install(game);
   }
}
