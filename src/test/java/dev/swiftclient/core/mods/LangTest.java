package dev.swiftclient.core.mods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.swiftclient.modules.AutoJumpModule;
import dev.swiftclient.modules.FreelookModule;
import dev.swiftclient.modules.FullBrightModule;
import dev.swiftclient.modules.NoRainModule;
import dev.swiftclient.modules.RealisticCapeModule;
import dev.swiftclient.modules.ToggleSneakModule;
import dev.swiftclient.modules.ToggleSprintModule;
import dev.swiftclient.modules.ZoomModule;
import dev.swiftclient.testing.TestGame;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Every text the player can see has an English and a French translation. Run with
 * {@code -Dswiftclient.langDump=<file>} to write the English defaults of missing keys.
 */
class LangTest {
   private static final Path LANG = Path.of("src/main/resources/assets/swiftclient/lang");
   private static final Pattern TR = Pattern.compile("(?:Tr\\.of|Tr\\.orDefault|I18n\\.get)\\(\\s*\"(swift\\.[a-z0-9_.]+|key\\.[a-z0-9_.]+)\"");

   private static Map<String, String> read(String lang) throws IOException {
      Map<String, String> out = new LinkedHashMap<>();
      JsonObject o = JsonParser.parseString(Files.readString(LANG.resolve(lang + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
      for (Map.Entry<String, JsonElement> e : o.entrySet()) {
         out.put(e.getKey(), e.getValue().getAsString());
      }

      return out;
   }

   /** Key -> English default, for everything the module system displays. */
   static Map<String, String> moduleKeys() {
      TestGame.install();
      ModuleManager.resetForTests();
      ModuleManager.install(
         () -> List.of(
            new ZoomModule(), new FullBrightModule(), new RealisticCapeModule(), new ToggleSprintModule(), new ToggleSneakModule(),
            new AutoJumpModule(), new NoRainModule(), new FreelookModule()
         )
      );
      Map<String, String> keys = new LinkedHashMap<>();

      for (Module m : ModuleManager.modules()) {
         String base = "swift.module." + m.id;
         keys.put(base + ".name", m.name);
         if (!m.description.isBlank()) {
            keys.put(base + ".desc", m.description);
         }

         keys.put("swift.category." + ModuleSetting.optionKey(m.category), m.category);

         for (ModuleSetting s : m.settings()) {
            String sb = base + ".setting." + s.id;
            keys.put(sb + ".name", s.name);
            if (!s.description().isBlank()) {
               keys.put(sb + ".desc", s.description());
            }

            keys.put("swift.group." + ModuleSetting.optionKey(s.group()), s.group());
            if (s.type == ModuleSetting.Type.CYCLE) {
               String[] optionKeys = s.optionKeys();
               for (int i = 0; i < optionKeys.length; i++) {
                  keys.put(sb + "." + optionKeys[i], s.cycleLabelAt(i));
               }
            }
         }
      }

      return keys;
   }

   static Set<String> sourceKeys() throws IOException {
      Set<String> out = new TreeSet<>();
      try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
         for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
            Matcher m = TR.matcher(Files.readString(p, StandardCharsets.UTF_8));
            while (m.find()) {
               if (!m.group(1).endsWith(".")) {
                  out.add(m.group(1));
               }
            }
         }
      }

      return out;
   }

   @Test
   void everyDisplayedTextIsTranslated() throws IOException {
      Map<String, String> en = read("en_us");
      Map<String, String> fr = read("fr_fr");
      Map<String, String> modules = moduleKeys();
      Set<String> code = sourceKeys();

      Map<String, String> missingEn = new LinkedHashMap<>();
      modules.forEach((k, v) -> {
         if (!en.containsKey(k)) {
            missingEn.put(k, v);
         }
      });
      for (String k : code) {
         if (!en.containsKey(k)) {
            missingEn.put(k, "");
         }
      }

      String dump = System.getProperty("swiftclient.langDump");
      if (dump != null && !missingEn.isEmpty()) {
         Files.writeString(Path.of(dump), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(missingEn));
      }

      assertTrue(missingEn.isEmpty(), "missing in en_us.json: " + missingEn.keySet());
      Set<String> onlyEn = new TreeSet<>(en.keySet());
      onlyEn.removeAll(fr.keySet());
      Set<String> onlyFr = new TreeSet<>(fr.keySet());
      onlyFr.removeAll(en.keySet());
      assertTrue(onlyEn.isEmpty(), "missing in fr_fr.json: " + onlyEn);
      assertTrue(onlyFr.isEmpty(), "missing in en_us.json: " + onlyFr);
   }

   @Test
   void placeholdersMatchBetweenLanguages() throws IOException {
      Map<String, String> en = read("en_us");
      Map<String, String> fr = read("fr_fr");
      Pattern ph = Pattern.compile("%(\\d+\\$)?[sd]");
      for (Map.Entry<String, String> e : en.entrySet()) {
         if (fr.containsKey(e.getKey())) {
            assertEquals(ph.matcher(e.getValue()).results().count(), ph.matcher(fr.get(e.getKey())).results().count(), e.getKey());
         }
      }
   }
}
