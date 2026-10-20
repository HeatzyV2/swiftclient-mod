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
   /** Whether a singleplayer world is open (screens that host or edit it). */
   public static final boolean[] SINGLEPLAYER = {false};
   /** Signed-in player: {uuid, name, Minecraft access token}; all null = no account. */
   public static final String[] PLAYER = {null, null, null};

   private TestGame() {
   }

   /** English strings, as the game would show them. Missing key: the key itself, like vanilla I18n. */
   private static final Map<String, String> EN = loadEnglish();

   private static Map<String, String> loadEnglish() {
      try (var in = TestGame.class.getResourceAsStream("/assets/swiftclient/lang/en_us.json")) {
         Map<String, String> out = new HashMap<>();
         com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
            .getAsJsonObject()
            .entrySet()
            .forEach(e -> out.put(e.getKey(), e.getValue().getAsString()));
         return out;
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   public static String translate(String key, Object... args) {
      String v = EN.get(key);
      return v == null ? key : String.format(v, args);
   }

   public static void install() {
      CONFIG.clear();
      java.util.Arrays.fill(PLAYER, null);
      SINGLEPLAYER[0] = false;

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
         case "translate" -> translate((String)args[0], args.length > 1 ? (Object[])args[1] : new Object[0]);
         case "languages" -> java.util.List.of(
            new dev.swiftclient.core.platform.Lang("en_us", "English", "United States"),
            new dev.swiftclient.core.platform.Lang("fr_fr", "Français", "France"),
            new dev.swiftclient.core.platform.Lang("de_de", "Deutsch", "Deutschland"),
            new dev.swiftclient.core.platform.Lang("es_es", "Español", "España"),
            new dev.swiftclient.core.platform.Lang("it_it", "Italiano", "Italia"),
            new dev.swiftclient.core.platform.Lang("pt_br", "Português", "Brasil"),
            new dev.swiftclient.core.platform.Lang("ja_jp", "日本語", "日本"),
            new dev.swiftclient.core.platform.Lang("nl_nl", "Nederlands", "Nederland"),
            new dev.swiftclient.core.platform.Lang("pl_pl", "Polski", "Polska"),
            new dev.swiftclient.core.platform.Lang("ru_ru", "Русский", "Россия"),
            new dev.swiftclient.core.platform.Lang("sv_se", "Svenska", "Sverige"),
            new dev.swiftclient.core.platform.Lang("tr_tr", "Türkçe", "Türkiye"),
            new dev.swiftclient.core.platform.Lang("en_pt", "Pirate Speak", "The Seven Seas"));
         case "currentLanguage" -> "fr_fr";
         case "accounts" -> java.util.List.of(
            new dev.swiftclient.core.platform.Account("u1", "Zor4tox_", true, false, false),
            new dev.swiftclient.core.platform.Account("u2", "HeatzyV2", false, false, false),
            new dev.swiftclient.core.platform.Account("u3", "Steve", false, false, true));
         case "inSingleplayer" -> SINGLEPLAYER[0];
         case "worldName" -> SINGLEPLAYER[0] ? "Elysia Survival" : null;
         case "readClipboard" -> CLIPBOARD[0];
         case "getUuid" -> PLAYER[0];
         case "getUsername" -> PLAYER[1];
         case "getAccessToken" -> PLAYER[2];
         case "copyToClipboard" -> {
            CLIPBOARD[0] = (String)args[0];
            yield null;
         }
         default -> method.getReturnType() == boolean.class ? false : method.getReturnType() == int.class ? 0 : null;
      });
      Platform.install(game);
   }
}
