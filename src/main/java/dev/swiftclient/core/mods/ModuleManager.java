package dev.swiftclient.core.mods;

import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.mods.modules.ArmorModule;
import dev.swiftclient.core.mods.modules.AutoJumpModule;
import dev.swiftclient.core.mods.modules.BiomeModule;
import dev.swiftclient.core.mods.modules.BlockOverlayModule;
import dev.swiftclient.core.mods.modules.CpsModule;
import dev.swiftclient.core.mods.modules.FpsModule;
import dev.swiftclient.core.mods.modules.FullBrightModule;
import dev.swiftclient.core.mods.modules.HudModule;
import dev.swiftclient.core.mods.modules.KeystrokesModule;
import dev.swiftclient.core.mods.modules.MemoryModule;
import dev.swiftclient.core.mods.modules.MusicModule;
import dev.swiftclient.core.mods.modules.NoRainModule;
import dev.swiftclient.core.mods.modules.RealisticCapeModule;
import dev.swiftclient.core.mods.modules.ScoreboardModule;
import dev.swiftclient.core.mods.modules.SpeedModule;
import dev.swiftclient.core.mods.modules.TabAnimModule;
import dev.swiftclient.core.mods.modules.ToggleSneakModule;
import dev.swiftclient.core.mods.modules.ToggleSprintModule;
import dev.swiftclient.core.mods.modules.VanillaUiModule;
import dev.swiftclient.core.mods.modules.WiderTabModule;
import dev.swiftclient.core.mods.modules.ZoomModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ModuleManager {
   private static final List<Module> MODULES = new ArrayList<>();
   private static final Set<String> DEDICATED_HUD_MODULES = Set.of("music", "fps", "memory", "biome", "speed", "cps", "keystrokes", "armor", "scoreboard");
   private static boolean loaded = false;

   private ModuleManager() {
   }

   public static synchronized void init() {
      if (!loaded) {
         loaded = true;
         register(new ZoomModule());
         register(new FullBrightModule());
         register(new RealisticCapeModule());
         register(new ToggleSprintModule());
         register(new ToggleSneakModule());
         register(new AutoJumpModule());
         register(new VanillaUiModule());
         register(new TabAnimModule());
         register(new WiderTabModule());
         register(new BlockOverlayModule());
         register(new NoRainModule());

         for (HudElement el : HudManager.elements()) {
            if (!DEDICATED_HUD_MODULES.contains(el.id)) {
               register(new HudModule(el));
            }
         }

         register(new MusicModule());
         register(new FpsModule());
         register(new MemoryModule());
         register(new BiomeModule());
         register(new SpeedModule());
         register(new CpsModule());
         register(new KeystrokesModule());
         register(new ArmorModule());
         register(new ScoreboardModule());

         // Nouveaux modules Swift Client. Ceux qui appellent notImplemented() n ont pas encore
         // de comportement : ils restent enregistres (valeurs sauvegardees conservees) mais caches.
         register(new dev.swiftclient.core.mods.modules.ShulkerPreviewModule());
         register(new dev.swiftclient.core.mods.modules.QuickSwapModule());
         register(new dev.swiftclient.core.mods.modules.DynamicLightsModule());
         register(new dev.swiftclient.core.mods.modules.FoodStatusModule());
         register(new dev.swiftclient.core.mods.modules.DeathPointModule());
         register(new dev.swiftclient.core.mods.modules.HitSoundsModule());
         register(new dev.swiftclient.core.mods.modules.OldAnimationsModule());
         register(new dev.swiftclient.core.mods.modules.ParticleMultiplierModule());
         register(new dev.swiftclient.core.mods.modules.AutoReconnectModule());
         register(new dev.swiftclient.core.mods.modules.StreamerModeModule());
         register(new dev.swiftclient.core.mods.modules.ChatToolsModule());
         register(new dev.swiftclient.core.mods.modules.MacroKeybindsModule());
         register(new dev.swiftclient.core.mods.modules.DiscordRpcModule());
      }
   }

   public static void register(Module m) {
      MODULES.add(m);
      m.load();
   }

   public static List<Module> modules() {
      init();
      return MODULES;
   }

   public static Module byId(String id) {
      init();

      for (Module m : MODULES) {
         if (m.id.equals(id)) {
            return m;
         }
      }

      return null;
   }

   public static boolean active(String id) {
      Module m = byId(id);
      return m != null && m.isEnabled();
   }
}
