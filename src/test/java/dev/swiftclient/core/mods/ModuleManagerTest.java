package dev.swiftclient.core.mods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.swiftclient.core.mods.modules.CrosshairModule;
import dev.swiftclient.core.mods.modules.MusicModule;
import dev.swiftclient.modules.AutoJumpModule;
import dev.swiftclient.modules.FreelookModule;
import dev.swiftclient.modules.FullBrightModule;
import dev.swiftclient.modules.NoRainModule;
import dev.swiftclient.modules.RealisticCapeModule;
import dev.swiftclient.modules.ToggleSneakModule;
import dev.swiftclient.modules.ToggleSprintModule;
import dev.swiftclient.modules.ZoomModule;
import dev.swiftclient.testing.TestGame;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModuleManagerTest {
   @BeforeEach
   void setUp() {
      TestGame.install();
      ModuleManager.resetForTests();
   }

   // --- A module without behaviour cannot be registered ---

   static final class Phantom extends Module {
      Phantom() {
         super("phantom", "Phantom", "", "Utility", "gear", true);
         this.toggle("x", "X", true);
      }
   }

   @ConsumedBy({})
   static final class EmptyConsumedBy extends Module {
      EmptyConsumedBy() {
         super("empty", "Empty", "", "Utility", "gear", false);
      }
   }

   @Test
   void moduleWithoutBehaviourIsRejected() {
      IllegalStateException e = assertThrows(IllegalStateException.class, () -> ModuleManager.register(new Phantom()));
      assertTrue(e.getMessage().contains("phantom"), e.getMessage());
      assertThrows(IllegalStateException.class, () -> ModuleManager.register(new EmptyConsumedBy()));
      assertNull(ModuleManager.byId("phantom"));
   }

   @Test
   void everyShippedModuleDeclaresItsBehaviour() {
      for (Class<?> type : List.of(
         ZoomModule.class, FullBrightModule.class, RealisticCapeModule.class, ToggleSprintModule.class, ToggleSneakModule.class,
         AutoJumpModule.class, NoRainModule.class, FreelookModule.class
      )) {
         assertNull(ModuleManager.behaviourProblem(type), type.getSimpleName());
      }

      // Core modules: building the registry validates each of them.
      List<Module> all = ModuleManager.modules();
      assertTrue(all.size() >= 17, "core modules registered: " + all.size());
      for (Module m : all) {
         assertNull(ModuleManager.behaviourProblem(m.getClass()), m.id);
      }
   }

   @Test
   void phaseZeroPhantomsAreGone() {
      for (String id : List.of(
         "shulker_preview", "quick_swap", "dynamic_lights", "food_status", "death_point", "hit_sounds", "old_animations",
         "particle_multiplier", "auto_reconnect", "streamer_mode", "chat_tools", "macro_keybinds"
      )) {
         assertNull(ModuleManager.byId(id), id);
      }
   }

   @Test
   void duplicateIdIsRejected() {
      ModuleManager.register(new Recording("dup"));
      assertThrows(IllegalStateException.class, () -> ModuleManager.register(new Recording("dup")));
   }

   // --- Lifecycle ---

   static final class Recording extends Module {
      final List<String> calls = new ArrayList<>();
      boolean failTick;

      Recording(String id) {
         super(id, id, "", "Utility", "gear", false);
      }

      @Override
      protected void onEnable() {
         this.calls.add("enable");
      }

      @Override
      protected void onDisable() {
         this.calls.add("disable");
      }

      @Override
      protected void onTick() {
         this.calls.add("tick");
         if (this.failTick) {
            throw new RuntimeException("boom");
         }
      }

      @Override
      protected void onRender(float partialTick) {
         this.calls.add("render");
      }
   }

   @Test
   void lifecycleFollowsTheEnabledState() {
      Recording m = new Recording("rec");
      TestGame.CONFIG.put("mod.rec.enabled", "1");
      ModuleManager.register(m);
      assertTrue(m.calls.isEmpty(), "nothing runs before start()");

      ModuleManager.start();
      ModuleManager.tick();
      ModuleManager.render(0.5F);
      assertEquals(List.of("enable", "tick", "render"), m.calls);

      m.calls.clear();
      m.setEnabled(false);
      ModuleManager.tick();
      ModuleManager.render(0.5F);
      assertEquals(List.of("disable"), m.calls, "no tick/render while disabled");
      assertEquals("0", TestGame.CONFIG.get("mod.rec.enabled"));

      m.calls.clear();
      m.toggle();
      assertEquals(List.of("enable"), m.calls);
   }

   @Test
   void modulesStartingDisabledGetACleanupCall() {
      Recording m = new Recording("off");
      ModuleManager.register(m);
      ModuleManager.start();
      ModuleManager.tick();
      assertEquals(List.of("disable"), m.calls);
   }

   @Test
   void aFailingModuleDoesNotStopTheOthers() {
      Recording bad = new Recording("bad");
      Recording good = new Recording("good");
      bad.failTick = true;
      TestGame.CONFIG.put("mod.bad.enabled", "1");
      TestGame.CONFIG.put("mod.good.enabled", "1");
      ModuleManager.register(bad);
      ModuleManager.register(good);
      ModuleManager.start();
      ModuleManager.tick();
      ModuleManager.tick();
      assertEquals(List.of("enable", "tick", "tick"), good.calls);
      assertEquals(List.of("enable", "tick", "tick"), bad.calls);
   }

   @Test
   void typedAccessReturnsTheRegisteredInstance() {
      CrosshairModule crosshair = ModuleManager.get(CrosshairModule.class);
      assertSame(crosshair, ModuleManager.byId("crosshair"));
      assertNotNull(crosshair.color);
      assertThrows(IllegalStateException.class, () -> ModuleManager.get(ZoomModule.class), "platform module not installed in this test");
   }

   @Test
   void settingsDeclaredThroughFactoriesAreScopedToTheModule() {
      MusicModule music = ModuleManager.get(MusicModule.class);
      assertNotNull(music.setting("cover"));
      CrosshairModule crosshair = ModuleManager.get(CrosshairModule.class);
      crosshair.gap.setFraction(1.0);
      assertEquals("8.0", TestGame.CONFIG.get("mod.crosshair.gap"));
      assertFalse(crosshair.dot.boolValue());
   }
}
