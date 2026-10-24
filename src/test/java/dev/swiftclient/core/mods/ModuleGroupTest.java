package dev.swiftclient.core.mods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.swiftclient.modules.PlatformModules;
import dev.swiftclient.testing.TestGame;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModuleGroupTest {
   @BeforeEach
   void setUp() {
      TestGame.install();
      TestGame.CONFIG.clear();
      ModuleManager.resetForTests();
      ModuleManager.install(PlatformModules::all);
   }

   private static List<Module> tiles() {
      List<Module> out = new ArrayList<>();
      for (Module m : ModuleManager.modules()) {
         if (m.parent() == null) {
            out.add(m);
         }
      }

      return out;
   }

   @Test
   void everyPartOfEveryGroupExists() {
      for (Module m : ModuleManager.modules()) {
         if (m instanceof ModuleGroup g) {
            assertEquals(g.childIds().size(), g.children().size(), g.id + " has a missing or shared part");
            for (Module part : g.children()) {
               assertSame(g, part.parent(), part.id);
            }
         }
      }
   }

   @Test
   void theMenuShowsFewTiles() {
      List<Module> tiles = tiles();
      assertEquals(44, tiles.size(), "tiles: " + tiles.stream().map(m -> m.id).toList());
      for (Module m : tiles) {
         assertTrue(ModuleManager.CATEGORIES.contains(m.category), m.id + " is in " + m.category);
      }
   }

   @Test
   void masterSwitchGatesItsParts() {
      ModuleGroup g = (ModuleGroup)ModuleManager.byId("dropped_items");
      Module physics = ModuleManager.byId("item_physics");
      assertNotNull(g);
      physics.setEnabled(true);
      assertTrue(g.rawEnabled(), "switching a part on switches its group on");
      assertTrue(physics.isEnabled());
      g.setEnabled(false);
      assertFalse(physics.isEnabled(), "a part stops with its group");
      assertTrue(physics.rawEnabled(), "and keeps its own switch");
   }

   @Test
   void exclusivePartsSwitchEachOtherOff() {
      Module physics = ModuleManager.byId("item_physics");
      Module flat = ModuleManager.byId("items_2d");
      physics.setEnabled(true);
      flat.setEnabled(true);
      assertTrue(flat.rawEnabled());
      assertFalse(physics.rawEnabled());
   }

   @Test
   void groupSwitchedOnWithNoPartTurnsTheFirstOneOn() {
      ModuleGroup g = (ModuleGroup)ModuleManager.byId("build_tools");
      for (Module part : g.children()) {
         part.setEnabled(false);
      }

      g.setEnabled(false);
      g.setEnabled(true);
      assertTrue(g.children().get(0).rawEnabled());
   }

   @Test
   void unsavedGroupFollowsItsParts() {
      TestGame.CONFIG.put("mod.chunk_borders.enabled", "1");
      ModuleManager.resetForTests();
      ModuleManager.install(PlatformModules::all);
      assertTrue(ModuleManager.byId("build_tools").rawEnabled());
      assertTrue(ModuleManager.byId("chunk_borders").isEnabled());
   }
}
