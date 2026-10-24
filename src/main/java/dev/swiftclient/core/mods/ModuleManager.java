package dev.swiftclient.core.mods;

import dev.swiftclient.core.hud.HudElement;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.platform.Platform;
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
      "friends_group", "hud_minimap", "waypoints_group", "combat", "weapon_inspect", "image", "environment", "animations", "freelook_group",
      "keystrokes_group", "custom_glint", "hands", "cosmetic_effects", "infos", "zoom", "damage", "items_hud", "toggle_move", "nametags",
      "crosshair", "particles_group", "hud_music", "dropped_items", "armor_group", "team_view", "hitboxes", "tooltips", "elytra_swap",
      "fullbright", "server_hud", "blocks", "custom_fov", "hud_tnt", "build_tools", "chat", "streamer_group", "hud_stopwatch",
      "profiles_auto", "screenshot", "gui_blur", "autojump", "notifications_group", "discord_rpc", "client_settings", "@hud"
   );
   /**
    * Tiles that stand for several modules. The parts keep their ids (configs and profiles stay valid) and are
    * shown as the tabs of their group instead of tiles of their own.
    */
   private static List<ModuleGroup> groups() {
      return List.of(
         new ModuleGroup("toggle_move", "Toggle Sprint/Sneak", "Sprint and sneak with one press, and see on screen when they are toggled.", "PvP", "togglesprint",
            "togglesprint", "togglesneak", "hud_togglestatus"),
         new ModuleGroup("freelook_group", "Freelook", "Look around without turning (Freelook) or glance behind you (Snaplook).", "PvP", "freelook",
            "freelook", "snaplook"),
         new ModuleGroup("combat", "Combat", "Everything about your fights: reach, combo, opponent card and a recap at the end.", "PvP", "sword",
            "hud_pvpinfo", "hud_reach", "hud_combo", "combat_recap"),
         new ModuleGroup("keystrokes_group", "Keystrokes", "Movement keys, mouse buttons and clicks per second.", "PvP", "keyboard",
            "hud_keystrokes", "hud_cps"),
         new ModuleGroup("infos", "Infos", "Small HUD lines: FPS, ping, coordinates, clock, biome... Pick the ones you want, each one moves on its own in the HUD Editor.", "HUD", "info",
            "hud_fps", "hud_perf", "hud_ping", "hud_coords", "hud_direction", "hud_speed", "hud_biome", "hud_server", "hud_playtime",
            "hud_clock", "hud_date", "hud_days", "hud_effects", "hud_memory", "hud_packs"),
         new ModuleGroup("items_hud", "Items", "What you carry: item and totem counters, pickups and cooldowns.", "HUD", "stack",
            "hud_itemcounter", "totem_counter", "hud_itemtracker", "hud_cooldowns"),
         new ModuleGroup("armor_group", "Armor", "Your armor on screen, and a warning before it breaks.", "HUD", "shield",
            "hud_armor", "armor_warning"),
         new ModuleGroup("server_hud", "Server HUD", "Restyle what the server shows: scoreboard, boss bars, titles and the player list.", "HUD", "list",
            "hud_scoreboard", "hud_bossbar", "titles", "tab_editor"),
         new ModuleGroup("hands", "Hands", "Position, field of view and 3D arm of your first-person hands.", "Visual", "hand",
            "hand_pose", "view_model", "hand_model"),
         new ModuleGroup("animations", "Animations", "Swing, item switch and 1.7 animations.", "Visual", "swap",
            "swing_animation", "slow_swing", "hotbar_swap", "old_visuals"),
         new ModuleGroup("environment", "Environment", "Your own time, weather, sky and fog. Only on your screen.", "Visual", "sunrise",
            "time_changer", "weather_changer", "atmosphere", "fog"),
         new ModuleGroup("image", "Image", "Colour grading and motion blur on the world image.", "Visual", "saturation",
            "saturation", "motion_blur"),
         new ModuleGroup("particles_group", "Particles", "More useful particles, fewer distracting ones, and bursts where your hits land.", "Visual", "sparkles",
            "particles", "hit_particles"),
         new ModuleGroup("damage", "Damage", "How damage looks: hit colour, camera tilt, fire, low health and damage numbers.", "Visual", "heart",
            "hit_color", "damage_tilt", "fire", "low_health_warning", "damage_indicator"),
         new ModuleGroup("dropped_items", "Dropped Items", "How items on the ground look: physics or flat, and valuable loot drawn bigger.", "Visual", "physics",
            "item_physics", "items_2d", "uhc_overlay").exclusive("item_physics", "items_2d"),
         new ModuleGroup("tooltips", "Tooltips", "Shulker and Ender Chest previews, and more details in item tooltips.", "Visual", "layers",
            "shulker_tooltip", "ender_preview", "tooltip_plus"),
         new ModuleGroup("blocks", "Blocks", "Block outline, clear glass and foliage.", "Visual", "blockoverlay",
            "blockoverlay", "clear_glass", "hide_foliage", "better_foliage").exclusive("hide_foliage", "better_foliage"),
         new ModuleGroup("cosmetic_effects", "Effects", "Cosmetic effects only you see: aura, relics, jump circles, kill effect, skin...", "Visual", "star",
            "aura", "floating_relics", "pharaoh_headdress", "relic_particles", "hand_aura", "jump_circles", "kill_effect", "totem_spirit", "skin_change"),
         new ModuleGroup("waypoints_group", "Waypoints", "Mark places, find where you died and copy coordinates with one key.", "World", "pin",
            "waypoints", "death_marker", "coord_snapper"),
         new ModuleGroup("build_tools", "Build Tools", "Chunk borders, mob spawn spots and your WorldEdit selection.", "World", "worldedit",
            "chunk_borders", "light_overlay", "worldedit_cui"),
         new ModuleGroup("streamer_group", "Streamer Mode", "Hide what should stay private on a stream: server, coordinates, messages, your name and inventory.", "Interface", "eye",
            "streamer", "nick_hider", "hide_inventory"),
         new ModuleGroup("profiles_auto", "Auto Profiles", "Switch profile by itself: by server, by situation or with a key.", "Interface", "profiles",
            "autoprofile", "scenes"),
         new ModuleGroup("friends_group", "Friends", "Your Swift friends, shared with the launcher, and no hits on them by mistake.", "Interface", "friends",
            "friends", "friend_guard"),
         new ModuleGroup("notifications_group", "Notifications", "Swift toasts: friends online, screenshots, weather changes and updates.", "Interface", "bell",
            "hud_notifications", "weather_notifier", "updates"),
         new ModuleGroup("client_settings", "Client", "Settings of the client itself: menu key, main menu, fullscreen, reconnect, session stats.", "Interface", "gear",
            "mod_menu", "vanillaui", "borderless", "auto_reconnect", "session").alwaysOn()
      );
   }
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
            new PerfModule(),
            new CombatRecapModule(),
            new StreamerModule(),
            new UpdateModule(),
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

         byId.keySet().removeAll(Platform.game().unavailableModules());
         for (ModuleGroup g : groups()) {
            byId.put(g.id, g);
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

         for (Module m : List.copyOf(MODULES)) {
            if (m instanceof ModuleGroup g) {
               g.link(ModuleManager::byId);
            }
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
      if (!(m instanceof HudModule) && !(m instanceof ModuleGroup)) {
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

         if (m instanceof ModuleGroup g) {
            // The master switch starts or stops every part that is switched on.
            for (Module c : g.children()) {
               if (c.rawEnabled()) {
                  onToggled(c);
               }
            }
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
