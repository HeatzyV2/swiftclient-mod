package dev.swiftclient.core.mods;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.modules.PlatformModules;
import dev.swiftclient.testing.TestGame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Every icon name the code can ask for must exist as a texture, otherwise the game draws the purple "missing" tile. */
class IconsTest {
   /** The repository's src/ folder (each Minecraft version runs its tests from versions/<mc>/). */
   private static final Path SRC = Path.of(System.getProperty("swiftclient.src", "src"));
   private static final Path ICONS = SRC.resolve("main/resources/assets/swiftclient/textures/gui/icons");

   @BeforeEach
   void setUp() {
      TestGame.install();
      ModuleManager.resetForTests();
      PlatformModules.all().forEach(ModuleManager::register);
   }

   private static boolean exists(String name) {
      return Files.exists(ICONS.resolve(name + ".png"));
   }

   @Test
   void everyModuleHasItsIcon() {
      Set<String> missing = new TreeSet<>();
      for (Module m : ModuleManager.modules()) {
         if (m.icon != null && !exists(m.icon)) {
            missing.add(m.id + " -> " + m.icon);
         }
      }

      assertTrue(missing.isEmpty(), "modules with a missing icon: " + missing);
   }

   @Test
   void everyHudElementHasItsIcon() {
      Set<String> missing = new TreeSet<>();
      for (HudElement e : HudManager.elements()) {
         String icon = e.icon();
         if (icon != null && !exists(icon)) {
            missing.add(e.id + " -> " + icon);
         }
      }

      assertTrue(missing.isEmpty(), "HUD elements with a missing icon: " + missing);
   }

   @Test
   void everyIconNamedInTheSourcesExists() throws IOException {
      Pattern literal = Pattern.compile("\\.icon\\(\"([a-z_0-9]+)\"");
      Set<String> missing = new TreeSet<>();
      try (Stream<Path> files = Files.walk(SRC.resolve("main/java"))) {
         for (Path f : (Iterable<Path>)files.filter(p -> p.toString().endsWith(".java"))::iterator) {
            Matcher m = literal.matcher(Files.readString(f));
            while (m.find()) {
               if (!exists(m.group(1))) {
                  missing.add(f.getFileName() + " -> " + m.group(1));
               }
            }
         }
      }

      assertTrue(missing.isEmpty(), "icons used in the code but missing as textures: " + missing);
   }
}
