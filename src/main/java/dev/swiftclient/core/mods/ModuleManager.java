package dev.swiftclient.core.mods;

import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.mods.modules.ArmorModule;
import dev.swiftclient.core.mods.modules.BiomeModule;
import dev.swiftclient.core.mods.modules.BlockOverlayModule;
import dev.swiftclient.core.mods.modules.CpsModule;
import dev.swiftclient.core.mods.modules.CrosshairModule;
import dev.swiftclient.core.mods.modules.DiscordRpcModule;
import dev.swiftclient.core.mods.modules.FpsModule;
import dev.swiftclient.core.mods.modules.GuiBlurModule;
import dev.swiftclient.core.mods.modules.HudModule;
import dev.swiftclient.core.mods.modules.KeystrokesModule;
import dev.swiftclient.core.mods.modules.MemoryModule;
import dev.swiftclient.core.mods.modules.MusicModule;
import dev.swiftclient.core.mods.modules.ScoreboardModule;
import dev.swiftclient.core.mods.modules.SpeedModule;
import dev.swiftclient.core.mods.modules.TabAnimModule;
import dev.swiftclient.core.mods.modules.VanillaUiModule;
import dev.swiftclient.core.mods.modules.WiderTabModule;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;

public final class ModuleManager {
   private static final Logger LOG = Log.get("Modules");
   private static final List<Module> MODULES = new ArrayList<>();
   private static final Map<Class<?>, Module> BY_CLASS = new HashMap<>();
   private static final Set<String> DEDICATED_HUD_MODULES = Set.of("music", "fps", "memory", "biome", "speed", "cps", "keystrokes", "armor", "scoreboard");
   /** Menu order. "@hud" stands for the generic HUD element modules. Unknown ids are appended. */
   private static final List<String> ORDER = List.of(
      "zoom", "fullbright", "realisticcape", "togglesprint", "togglesneak", "autojump", "vanillaui", "tabanim", "widertab", "blockoverlay", "norain",
      "@hud", "hud_music", "hud_fps", "hud_memory", "hud_biome", "hud_speed", "hud_cps", "hud_keystrokes", "hud_armor", "hud_scoreboard",
      "discord_rpc", "crosshair", "freelook", "gui_blur"
   );
   private static final Set<String> HOOKS = Set.of("onEnable", "onDisable", "onTick", "onRender");
   /** "moduleId:phase" already reported, so a module failing every tick logs once. */
   private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
   private static Supplier<List<? extends Module>> platformModules = List::of;
   private static boolean loaded = false;
   private static boolean started = false;

   private ModuleManager() {
   }

   /**
    * Declares the modules that need the game (zoom, fullbright...), created by the Minecraft side. Must run
    * before anything touches the registry, which is built lazily on first access.
    */
   public static synchronized void install(Supplier<List<? extends Module>> platform) {
      if (loaded) {
         throw new IllegalStateException("ModuleManager deja initialise : install() doit etre appele en premier");
      }

      platformModules = platform;
   }

   private static synchronized void init() {
      if (!loaded) {
         loaded = true;
         Map<String, Module> byId = new LinkedHashMap<>();

         for (Module m : platformModules.get()) {
            byId.put(m.id, m);
         }

         for (Module m : List.of(
            new VanillaUiModule(),
            new TabAnimModule(),
            new WiderTabModule(),
            new BlockOverlayModule(),
            new MusicModule(),
            new FpsModule(),
            new MemoryModule(),
            new BiomeModule(),
            new SpeedModule(),
            new CpsModule(),
            new KeystrokesModule(),
            new ArmorModule(),
            new ScoreboardModule(),
            new DiscordRpcModule(),
            new CrosshairModule(),
            new GuiBlurModule()
         )) {
            byId.put(m.id, m);
         }

         for (String id : ORDER) {
            if ("@hud".equals(id)) {
               for (HudElement el : HudManager.elements()) {
                  if (!DEDICATED_HUD_MODULES.contains(el.id)) {
                     register(new HudModule(el));
                  }
               }
            } else {
               Module m = byId.remove(id);
               if (m != null) {
                  register(m);
               }
            }
         }

         for (Module m : byId.values()) {
            register(m);
         }

         LOG.info("{} module(s) enregistre(s)", MODULES.size());
      }
   }

   private static void ensureInit() {
      if (!loaded) {
         init();
      }
   }

   /** Test support: forget every module and go back to the uninitialised state. */
   static synchronized void resetForTests() {
      MODULES.clear();
      BY_CLASS.clear();
      REPORTED.clear();
      platformModules = List::of;
      loaded = false;
      started = false;
   }

   public static synchronized boolean isLoaded() {
      return loaded;
   }

   /**
    * Adds a module. Refuses one that has no behaviour: it must override a lifecycle hook or declare
    * where its behaviour lives with {@link ConsumedBy}.
    */
   public static synchronized void register(Module m) {
      String missing = behaviourProblem(m.getClass());
      if (missing != null) {
         throw new IllegalStateException("Module '" + m.id + "' (" + m.getClass().getName() + ") : " + missing);
      }

      for (Module other : MODULES) {
         if (other.id.equals(m.id)) {
            throw new IllegalStateException("Module id en double : " + m.id);
         }
      }

      MODULES.add(m);
      if (!(m instanceof HudModule)) {
         BY_CLASS.put(m.getClass(), m);
      }

      m.load();
      if (started && m.isEnabled()) {
         safely(m, "enable", m::fireEnable);
      }
   }

   /** Null when the class has behaviour, otherwise why it is rejected. */
   static String behaviourProblem(Class<?> type) {
      ConsumedBy consumedBy = type.getAnnotation(ConsumedBy.class);
      if (consumedBy != null) {
         return consumedBy.value().length == 0 ? "@ConsumedBy vide" : null;
      }

      for (Class<?> c = type; c != null && c != Module.class; c = c.getSuperclass()) {
         for (Method method : c.getDeclaredMethods()) {
            if (HOOKS.contains(method.getName())) {
               return null;
            }
         }
      }

      return "aucun comportement (ni hook onEnable/onDisable/onTick/onRender, ni @ConsumedBy)";
   }

   /**
    * Called once the game is ready: onEnable for modules that start enabled, onDisable for the others so
    * they can clean up state left from a previous session (e.g. a vanilla option still overridden).
    */
   public static synchronized void start() {
      ensureInit();
      if (!started) {
         started = true;

         for (Module m : MODULES) {
            if (m.isEnabled()) {
               safely(m, "enable", m::fireEnable);
            } else {
               safely(m, "disable", m::fireDisable);
            }
         }

         try {
            // Loads the profiles now: those saved by older builds get their cycle positions rewritten as keys.
            Profiles.actif();
         } catch (Throwable t) {
            LOG.error("Chargement des profils impossible", t);
         }
      }
   }

   public static void tick() {
      if (started) {
         for (Module m : MODULES) {
            if (m.isEnabled()) {
               safely(m, "tick", m::fireTick);
            }
         }
      }
   }

   public static void render(float partialTick) {
      if (started) {
         for (Module m : MODULES) {
            if (m.isEnabled()) {
               safely(m, "render", () -> m.fireRender(partialTick));
            }
         }
      }
   }

   static void onToggled(Module m) {
      if (started) {
         if (m.isEnabled()) {
            safely(m, "enable", m::fireEnable);
         } else {
            safely(m, "disable", m::fireDisable);
         }
      }
   }

   private static void safely(Module m, String phase, Runnable r) {
      try {
         r.run();
      } catch (Throwable t) {
         if (REPORTED.add(m.id + ":" + phase)) {
            LOG.error("Module {} : erreur dans {} (signalee une seule fois)", m.id, phase, t);
         }
      }
   }

   public static List<Module> modules() {
      ensureInit();
      return MODULES;
   }

   /** Typed access to a registered module: {@code ModuleManager.get(ZoomModule.class).fov.value()}. */
   public static <T extends Module> T get(Class<T> type) {
      ensureInit();
      Module m = BY_CLASS.get(type);
      if (m == null) {
         throw new IllegalStateException("Module non enregistre : " + type.getName());
      }

      return type.cast(m);
   }

   /** By-id lookup, for generic code only (profiles, menu, HUD elements bound to their module). */
   public static Module byId(String id) {
      ensureInit();

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
