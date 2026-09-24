package dev.swiftclient.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigMigrationTest {
   /** A config as older builds wrote it, with a bit of everything. */
   static Map<String, String> representativeLegacy() {
      Map<String, String> p = new LinkedHashMap<>();
      p.put("mod.zoom.enabled", "1");
      p.put("mod.zoom.fov", "30.0");
      p.put("mod.zoom.mode", "1.0");
      p.put("mod.zoom.key", "86.0");
      p.put("mod.crosshair.color", "ff4c5bff");
      p.put("mod.crosshair.pixels", "0000ffff0000");
      p.put("mod.fullbright.enabled", "0");
      p.put("mod.hud_music.opacity", "55.0");
      p.put("hud_pos_fps", "0.02,0.03");
      p.put("hud_anchor_fps", "0,2");
      p.put("hud_scale_fps", "1.25");
      p.put("hud_scale_global", "2.0");
      p.put("theme.current", "bloomy");
      p.put("lang.favorites", "fr_fr,en_us");
      p.put("nametag.ownname", "1");
      p.put("spotify.clientId", "abc123");
      p.put("partners.hidden", "play.example.com:25565");
      return p;
   }

   @Test
   void emptyInputGivesADefaultDocument() {
      List<String> warnings = new ArrayList<>();
      JsonObject root = ConfigMigration.fromLegacy(Map.of(), null, warnings);
      assertEquals(2, root.get("schemaVersion").getAsInt());
      assertTrue(ConfigCodec.flatten(root).isEmpty());
      assertTrue(warnings.isEmpty());
      assertFalse(root.has("profiles"));
   }

   @Test
   void everyKnownValueSurvivesTheMigration() {
      List<String> warnings = new ArrayList<>();
      Map<String, String> legacy = representativeLegacy();
      JsonObject root = ConfigMigration.fromLegacy(legacy, null, warnings);
      assertTrue(warnings.isEmpty(), warnings.toString());

      for (Map.Entry<String, String> e : legacy.entrySet()) {
         String back = ConfigCodec.get(root, e.getKey());
         if (e.getValue().contains(".") && e.getValue().matches("[0-9.,]+") && !e.getKey().startsWith("partners")) {
            // Numbers and coordinates: same numeric value.
            String[] a = e.getValue().split(",");
            String[] b = back.split(",");
            assertEquals(a.length, b.length, e.getKey());
            for (int i = 0; i < a.length; i++) {
               assertEquals(Double.parseDouble(a[i]), Double.parseDouble(b[i]), 1e-6, e.getKey());
            }
         } else {
            assertEquals(e.getValue(), back, e.getKey());
         }
      }

      // Typed and structured, not a bag of strings.
      JsonObject zoom = root.getAsJsonObject("modules").getAsJsonObject("zoom");
      assertTrue(zoom.get("enabled").getAsBoolean());
      assertEquals(30.0, zoom.getAsJsonObject("settings").get("fov").getAsDouble());
      JsonObject fps = root.getAsJsonObject("hud").getAsJsonObject("elements").getAsJsonObject("fps");
      assertEquals(0.02, fps.getAsJsonArray("pos").get(0).getAsDouble(), 1e-9);
      assertEquals(2, fps.getAsJsonArray("anchor").get(1).getAsInt());
      assertEquals("bloomy", root.getAsJsonObject("general").get("theme.current").getAsString());
   }

   @Test
   void obsoleteKeysAreReportedAndDropped() {
      Map<String, String> legacy = new LinkedHashMap<>(representativeLegacy());
      legacy.put("mod.shulker_preview.enabled", "1");
      legacy.put("mod.macro_keybinds.macro_1", "0.0");
      legacy.put("partners.cache", "[{\"name\":\"GlowSMP\"}]");
      legacy.put("mod.togglesprint.backwards", "1");
      List<String> warnings = new ArrayList<>();
      JsonObject root = ConfigMigration.fromLegacy(legacy, null, warnings);
      assertEquals(4, warnings.size(), warnings.toString());
      assertNull(ConfigCodec.get(root, "mod.shulker_preview.enabled"));
      assertNull(ConfigCodec.get(root, "partners.cache"));
      assertNull(ConfigCodec.get(root, "mod.togglesprint.backwards"));
      assertEquals("1", ConfigCodec.get(root, "mod.zoom.enabled"), "the rest is kept");
   }

   @Test
   void malformedValuesAreSkippedWithoutFailing() {
      Map<String, String> legacy = new LinkedHashMap<>();
      legacy.put("hud_pos_fps", "not,a,position");
      legacy.put("hud_anchor_cps", "x,y");
      legacy.put("hud_scale_global", "big");
      legacy.put("mod.zoom.fov", "45.0");
      List<String> warnings = new ArrayList<>();
      JsonObject root = ConfigMigration.fromLegacy(legacy, null, warnings);
      assertEquals(3, warnings.size(), warnings.toString());
      assertEquals("45.0", ConfigCodec.get(root, "mod.zoom.fov"));
   }

   @Test
   void multipleProfilesAreMigratedAndBadEntriesSkipped() {
      String legacy = """
         {
           "active": "PvP",
           "profiles": {
             "Default": {
               "zoom": { "enabled": "0", "fov": "30.0", "mode": "0.0" },
               "hud": { "hud_pos_fps": "0.02,0.03", "hud_scale_global": "2.0" }
             },
             "PvP": {
               "zoom": { "enabled": "1", "fov": "20.0", "mode": "1.0" },
               "crosshair": { "enabled": "1", "color": "ffff0000" },
               "quick_swap": { "enabled": "1" },
               "togglesprint": { "enabled": "1", "backwards": "1" },
               "hud": { "hud_pos_cps": "oops" },
               "broken": 42
             },
             "Garbage": 7
           }
         }
         """;
      List<String> warnings = new ArrayList<>();
      JsonObject root = ConfigMigration.fromLegacy(Map.of(), JsonParser.parseString(legacy).getAsJsonObject(), warnings);
      JsonObject profiles = root.getAsJsonObject("profiles");
      assertEquals("PvP", profiles.get("active").getAsString());
      JsonObject list = profiles.getAsJsonObject("list");
      assertEquals(List.of("Default", "PvP"), new ArrayList<>(list.keySet()));

      Map<String, Map<String, String>> pvp = ConfigCodec.profileFromJson(list.getAsJsonObject("PvP"));
      assertEquals("1", pvp.get("zoom").get("enabled"));
      assertEquals(20.0, Double.parseDouble(pvp.get("zoom").get("fov")));
      assertEquals("ffff0000", pvp.get("crosshair").get("color"));
      assertFalse(pvp.containsKey("quick_swap"));
      assertFalse(pvp.get("togglesprint").containsKey("backwards"));

      Map<String, Map<String, String>> def = ConfigCodec.profileFromJson(list.getAsJsonObject("Default"));
      assertEquals("0.02,0.03", def.get("hud").get("hud_pos_fps"));
      // quick_swap, backwards, bad hud_pos_cps, "broken" section, "Garbage" profile
      assertEquals(5, warnings.size(), warnings.toString());
   }

   // --- File level: ConfigStore ---

   private static void writeLegacy(Path dir) throws Exception {
      StringBuilder props = new StringBuilder("#SwiftClient\n");
      for (Map.Entry<String, String> e : representativeLegacy().entrySet()) {
         props.append(e.getKey()).append('=').append(e.getValue().replace(":", "\\:")).append('\n');
      }

      props.append("mod.shulker_preview.enabled=1\n");
      Files.writeString(dir.resolve("swiftclient.properties"), props, StandardCharsets.ISO_8859_1);
      Files.writeString(
         dir.resolve("swiftclient-profiles.json"),
         "{\"active\":\"Default\",\"profiles\":{\"Default\":{\"zoom\":{\"enabled\":\"1\",\"mode\":\"1.0\"}},\"PvP\":{\"fullbright\":{\"enabled\":\"1\"}}}}"
      );
   }

   @Test
   void storeMigratesOnceKeepsBackupsAndReloadsTheNewFile(@TempDir Path dir) throws Exception {
      writeLegacy(dir);
      ConfigStore store = new ConfigStore(dir);
      assertEquals("30.0", store.get("mod.zoom.fov", null));
      assertEquals("bloomy", store.get("theme.current", null));
      assertEquals("play.example.com:25565", store.get("partners.hidden", null));
      assertNull(store.get("mod.shulker_preview.enabled", null));
      assertTrue(store.migrated());
      assertEquals(1, store.migrationWarnings().size(), store.migrationWarnings().toString());
      assertEquals(List.of("Default", "PvP"), new ArrayList<>(store.profiles().getAsJsonObject("list").keySet()));

      assertTrue(Files.isRegularFile(dir.resolve("swiftclient.json")));
      assertTrue(Files.isRegularFile(dir.resolve("swiftclient.properties.bak")));
      assertTrue(Files.isRegularFile(dir.resolve("swiftclient-profiles.json.bak")));
      assertFalse(Files.exists(dir.resolve("swiftclient.properties")));
      assertFalse(Files.exists(dir.resolve("swiftclient-profiles.json")));

      store.set("mod.zoom.fov", "25.0");
      store.flush();
      ConfigStore reopened = new ConfigStore(dir);
      assertEquals("25.0", reopened.get("mod.zoom.fov", null));
      assertFalse(reopened.migrated(), "second start reads swiftclient.json, no migration");
   }

   @Test
   void corruptFileIsKeptAsideAndDefaultsAreUsed(@TempDir Path dir) throws Exception {
      Files.writeString(dir.resolve("swiftclient.json"), "{ this is not json");
      ConfigStore store = new ConfigStore(dir);
      assertEquals("fallback", store.get("theme.current", "fallback"));
      assertTrue(Files.isRegularFile(dir.resolve("swiftclient.json.corrupt")));
   }

   @Test
   void sectionsFromANewerVersionArePreserved(@TempDir Path dir) throws Exception {
      Files.writeString(dir.resolve("swiftclient.json"), "{\"schemaVersion\":3,\"future\":{\"x\":1},\"general\":{\"theme.current\":\"night\"}}");
      ConfigStore store = new ConfigStore(dir);
      assertEquals("night", store.get("theme.current", null));
      store.set("theme.current", "day");
      store.flush();
      JsonObject saved = JsonParser.parseString(Files.readString(dir.resolve("swiftclient.json"))).getAsJsonObject();
      assertEquals(3, saved.get("schemaVersion").getAsInt(), "never downgraded");
      assertEquals(1, saved.getAsJsonObject("future").get("x").getAsInt());
      assertEquals("day", saved.getAsJsonObject("general").get("theme.current").getAsString());
   }

   @Test
   void removingAKeyWorks(@TempDir Path dir) {
      ConfigStore store = new ConfigStore(dir);
      store.set("mod.togglesprint.vanilla_prev", "false");
      assertEquals("false", store.get("mod.togglesprint.vanilla_prev", null));
      store.set("mod.togglesprint.vanilla_prev", null);
      assertNull(store.get("mod.togglesprint.vanilla_prev", null));
   }
}
