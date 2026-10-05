package dev.swiftclient.core.mods;

import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.mods.modules.*;
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
   /**
    * Menu order ("All" view). Deliberately mixed: signature Swift features first, then HUD, PvP and visual
    * modules interleaved. "@hud" stands for the generic HUD element modules. Unknown ids are appended.
    */
   private static final List<String> ORDER = List.of(
      "friends", "hud_minimap", "waypoints", "motion_blur", "hud_pvpinfo", "hud_combo", "saturation", "freelook", "hud_keystrokes",
      "item_physics", "hud_direction", "time_changer", "nametags", "hud_cps", "chunk_borders", "zoom", "fog", "hud_reach", "hud_bossbar",
      "hit_color", "hud_music", "hud_fps", "particles", "snaplook", "worldedit_cui", "old_visuals", "hud_ping", "hud_armor", "uhc_overlay",
      "hud_itemcounter", "clear_glass", "team_view", "hud_cooldowns", "crosshair", "hud_scoreboard", "weather_changer", "hud_server",
      "light_overlay", "hitboxes", "hud_notifications", "fullbright", "hud_itemtracker", "items_2d", "hud_tnt", "tab_editor", "chat",
      "hud_memory", "hide_foliage", "hud_stopwatch", "togglesprint", "togglesneak", "hud_togglestatus", "fire", "hud_playtime",
      "better_foliage", "nick_hider", "titles", "hud_packs", "blockoverlay", "screenshot", "gui_blur", "hud_speed", "autojump", "@hud",
      "hud_biome", "mod_menu", "discord_rpc", "vanillaui"
   );
   /** Category chips, in this order. */
   public static final List<String> CATEGORIES = List.of("HUD", "PvP", "Visual", "World", "Interface");
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
            new TabEditorModule(),
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
            new GuiBlurModule(),
            new PingModule(),
            new DirectionModule(),
            new ServerModule(),
            new PlaytimeModule(),
            new ToggleStatusModule(),
            new ComboModule(),
            new ReachModule(),
            new PvpInfoModule(),
            new BossBarModule(),
            new CooldownsModule(),
            new ItemCounterModule(),
            new ItemTrackerModule(),
            new PackDisplayModule(),
            new MinimapModule(),
            new NotificationsModule(),
            new SessionModule(),
            new ScenesModule(),
            new TntCountdownModule(),
            new TitlesModule(),
            new FriendsModule()
         )) {
            byId.put(m.id, m);
         }

         for (String id : ORDER) {
            if ("@hud".equals(id)) {
               for (HudElement el : HudManager.elements()) {
                  if (!byId.containsKey(el.moduleId()) && byId(el.moduleId()) == null) {
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
