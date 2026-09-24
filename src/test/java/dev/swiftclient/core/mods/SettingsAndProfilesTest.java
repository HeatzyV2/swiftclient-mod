package dev.swiftclient.core.mods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import dev.swiftclient.core.mods.modules.BlockOverlayModule;
import dev.swiftclient.core.mods.modules.CrosshairModule;
import dev.swiftclient.testing.TestGame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SettingsAndProfilesTest {
   @BeforeEach
   void setUp() {
      TestGame.install();
      ModuleManager.resetForTests();
      Profiles.resetForTests();
   }

   @Test
   void cycleIsStoredByKeyAndOldPositionsAreConverted() {
      TestGame.CONFIG.put("mod.blockoverlay.mode", "1.0");
      BlockOverlayModule m = ModuleManager.get(BlockOverlayModule.class);
      assertEquals(BlockOverlayModule.MODE_HIDDEN, m.mode.cycleIndex(), "same choice as before");
      assertEquals("hidden", TestGame.CONFIG.get("mod.blockoverlay.mode"), "rewritten as a key");

      m.mode.cycleNext();
      assertEquals("custom_outline", TestGame.CONFIG.get("mod.blockoverlay.mode"));
   }

   @Test
   void unknownCycleKeyKeepsTheDefault() {
      TestGame.CONFIG.put("mod.crosshair.style", "hexagon");
      assertEquals(0, ModuleManager.get(CrosshairModule.class).style.cycleIndex());
   }

   @Test
   void colourIsStoredAsHashArgbAndOldHexIsConverted() {
      TestGame.CONFIG.put("mod.crosshair.color", "ff4c5bff");
      CrosshairModule m = ModuleManager.get(CrosshairModule.class);
      assertEquals(0xFF4C5BFF, m.color.colorValue());
      assertEquals("#FF4C5BFF", TestGame.CONFIG.get("mod.crosshair.color"));
   }

   @Test
   void profilesWithOldCyclePositionsAreNormalised() {
      JsonObject list = new JsonObject();
      JsonObject pvp = new JsonObject();
      JsonObject modules = new JsonObject();
      JsonObject overlay = new JsonObject();
      JsonObject settings = new JsonObject();
      settings.addProperty("mode", 1.0);
      overlay.add("settings", settings);
      modules.add("blockoverlay", overlay);
      pvp.add("modules", modules);
      list.add("PvP", pvp);
      JsonObject section = new JsonObject();
      section.addProperty("active", "PvP");
      section.add("list", list);
      TestGame.store.setProfiles(section);

      assertEquals("PvP", Profiles.actif());
      JsonObject saved = TestGame.store.profiles().getAsJsonObject("list").getAsJsonObject("PvP");
      assertEquals("hidden", saved.getAsJsonObject("modules").getAsJsonObject("blockoverlay").getAsJsonObject("settings").get("mode").getAsString());
   }

   @Test
   void shareCodeRoundTrip() {
      ModuleManager.get(CrosshairModule.class).gap.setFraction(0.5);
      Profiles.creer("PvP");
      String code = Profiles.codePartage("PvP");
      assertTrue(code.startsWith("SWIFT1."), code);

      String imported = Profiles.importerCode(code);
      assertEquals("PvP (2)", imported, "name made unique");
      assertEquals(Profiles.modulesActifs("PvP"), Profiles.modulesActifs(imported));
      assertTrue(Profiles.noms().contains(imported));
   }

   @Test
   void invalidShareCodesAreRejectedWithAReadableMessage() {
      assertEquals("This is not a Swift Client profile code", assertThrows(IllegalArgumentException.class, () -> Profiles.importerCode("hello")).getMessage());
      assertEquals(
         "Invalid or incomplete profile code", assertThrows(IllegalArgumentException.class, () -> Profiles.importerCode("SWIFT1.not-base64-gzip")).getMessage()
      );
      String code = Profiles.codePartage(Profiles.actif());
      assertThrows(IllegalArgumentException.class, () -> Profiles.importerCode(code.substring(0, code.length() / 2)));
   }
}
