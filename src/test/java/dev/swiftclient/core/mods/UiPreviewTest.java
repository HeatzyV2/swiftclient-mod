package dev.swiftclient.core.mods;

import dev.swiftclient.testing.PreviewCanvas;
import dev.swiftclient.testing.TestGame;
import dev.swiftclient.core.screen.HostWorldScreen;
import dev.swiftclient.core.screen.HudEditorScreen;
import dev.swiftclient.core.screen.SessionScreen;
import dev.swiftclient.core.screen.WardrobeScreen;
import dev.swiftclient.core.ui.UiScreen;
import dev.swiftclient.modules.PlatformModules;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Renders the main screens to build/ui-preview/*.png with {@link PreviewCanvas}, so layouts can be checked
 * without Minecraft: ./gradlew test --tests '*UiPreviewTest*' -Ppreview=true
 */
@EnabledIfSystemProperty(named = "swiftclient.preview", matches = ".+")
class UiPreviewTest {
   private static final Path OUT = Path.of("build", "ui-preview");

   @BeforeEach
   void setUp() {
      TestGame.install();
      ModuleManager.resetForTests();
      PlatformModules.all().forEach(ModuleManager::register);
   }

   private static void shot(String name, int w, int h, UiScreen screen, Consumer<UiScreen> before) throws Exception {
      screen.layout(w, h);
      if (before != null) {
         before.accept(screen);
      }

      PreviewCanvas c = new PreviewCanvas(w, h, 3);
      for (int i = 0; i < 3; i++) {
         screen.draw(c, w / 2, h / 2, 0.0F);
      }

      c.save(OUT.resolve(name + ".png"));
   }

   @Test
   void mods() throws Exception {
      shot("mods-427x240", 427, 240, new ModsScreen(), null);
      shot("mods-640x360", 640, 360, new ModsScreen(), null);
      for (String id : new String[]{"zoom", "freelook", "togglesprint", "hitboxes"}) {
         ModuleManager.byId(id).setEnabled(true);
      }

      shot("mods-enabled-640x360", 640, 360, new ModsScreen(), null);
   }

   /** Fills the private state of a screen (the real one comes from the backend). */
   private static void set(Object o, String field, Object value) {
      try {
         var f = o.getClass().getDeclaredField(field);
         f.setAccessible(true);
         f.set(o, value);
      } catch (ReflectiveOperationException e) {
         throw new IllegalStateException(e);
      }
   }

   private static Object inner(Class<?> outer, String simple, Class<?>[] types, Object... args) throws Exception {
      for (Class<?> k : outer.getDeclaredClasses()) {
         if (k.getSimpleName().equals(simple)) {
            var ctor = k.getDeclaredConstructor(types);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
         }
      }

      throw new IllegalStateException(simple);
   }

   private static Consumer<UiScreen> wardrobeData(boolean pets) {
      return s -> {
         try {
            Class<?>[] ct = {String.class, String.class, String.class, String.class};
            java.util.List<Object> items = new java.util.ArrayList<>();
            items.add(inner(WardrobeScreen.class, "Item", ct, "__none__", "None", "", null));
            for (String n : new String[]{"Founder", "Lightning", "Aurora", "Pixel Heart"}) {
               items.add(inner(WardrobeScreen.class, "Item", ct, n.toLowerCase(), n, "Swift Client capes", null));
            }
            for (String n : new String[]{"Migrator", "Vanilla", "Mojang Office", "Minecon 2016", "Cherry Blossom"}) {
               items.add(inner(WardrobeScreen.class, "Item", ct, "mojang:" + n, n, "Minecraft capes", "id"));
            }
            set(s, "items", java.util.List.copyOf(items));
            set(s, "selected", "aurora");
            set(s, "coins", 1250);
            set(s, "loaded", true);
            Class<?>[] pt = {String.class, String.class, int.class, boolean.class};
            set(s, "petItems", java.util.List.of(
               inner(WardrobeScreen.class, "PetItem", pt, "fox", "Fox", 400, true),
               inner(WardrobeScreen.class, "PetItem", pt, "wolf", "Wolf", 650, false),
               inner(WardrobeScreen.class, "PetItem", pt, "owl", "Owl", 900, false)));
            set(s, "equippedPet", "fox");
            if (pets) {
               Class<?> tabEnum = Class.forName("dev.swiftclient.core.screen.WardrobeScreen$Tab");
               set(s, "tab", tabEnum.getEnumConstants()[1]);
            }
         } catch (Exception e) {
            throw new IllegalStateException(e);
         }
      };
   }

   @Test
   void wardrobe() throws Exception {
      shot("wardrobe-380x170", 380, 170, new WardrobeScreen(), wardrobeData(false));
      shot("wardrobe-560x250", 560, 250, new WardrobeScreen(), wardrobeData(false));
      shot("wardrobe-pets-420x180", 420, 180, new WardrobeScreen(), wardrobeData(true));
   }

   private static Consumer<UiScreen> hostLive() {
      return s -> {
         try {
            Class<?> et = Class.forName("dev.swiftclient.core.screen.HostWorldScreen$Etat");
            set(s, "etat", et.getEnumConstants()[2]);
            set(s, "adresse", "zor4tox.swiftclient.fr:10061");
            set(s, "amisCharges", true);
            java.util.List<dev.swiftclient.core.social.SocialApi.Friend> f = new java.util.ArrayList<>();
            for (String n : new String[]{"Notch", "Dinnerbone", "jeb_", "Alex", "Steve42", "Herobrine"}) {
               f.add(new dev.swiftclient.core.social.SocialApi.Friend(n, "u" + n, n, n.length() % 2 == 0, "accepted"));
            }
            set(s, "amis", f);
         } catch (Exception e) {
            throw new IllegalStateException(e);
         }
      };
   }

   @Test
   void host() throws Exception {
      TestGame.SINGLEPLAYER[0] = true;
      shot("host-380x170", 380, 170, new HostWorldScreen(), null);
      shot("host-560x250", 560, 250, new HostWorldScreen(), null);
      shot("host-live-380x170", 380, 170, new HostWorldScreen(), hostLive());
      shot("host-live-560x250", 560, 250, new HostWorldScreen(), hostLive());
      TestGame.SINGLEPLAYER[0] = false;
      shot("host-noworld-380x170", 380, 170, new HostWorldScreen(), null);
   }

   private static Consumer<UiScreen> hudSelect(boolean drawer, String... ids) {
      return s -> {
         try {
            @SuppressWarnings("unchecked")
            java.util.Set<String> sel = (java.util.Set<String>)field(s, "sel");
            sel.addAll(java.util.List.of(ids));
            set(s, "primary", ids[0]);
            set(s, "drawerOpen", drawer);
            if (drawer) {
               set(s, "drawerT", 1.0F);
            }
            // Pretend the player is in a world with the Swift HUD enabled
            for (String id : new String[]{"fps", "coords", "clock", "cps", "keystrokes", "ping"}) {
               var m = ModuleManager.byId("hud_" + id);
               if (m != null) {
                  m.setEnabled(true);
               }
            }
         } catch (Exception e) {
            throw new IllegalStateException(e);
         }
      };
   }

   private static Object field(Object o, String name) throws ReflectiveOperationException {
      var f = o.getClass().getDeclaredField(name);
      f.setAccessible(true);
      return f.get(o);
   }

   @Test
   void hudEditor() throws Exception {
      shot("hud-427x240", 427, 240, new HudEditorScreen(), hudSelect(false, "fps"));
      shot("hud-640x360", 640, 360, new HudEditorScreen(), hudSelect(true, "coords"));
      shot("hud-multi-640x360", 640, 360, new HudEditorScreen(), hudSelect(false, "fps", "coords", "clock"));
   }

   /** Now Playing card with synced lyrics (looked up on LRCLIB, so it needs the network). */
   @Test
   void nowPlaying() throws Exception {
      ModuleManager.byId("hud_music").setEnabled(true);
      ModuleManager.byId("hud_music").setting("lyrics").setBool(true);
      ModuleManager.byId("hud_music").setting("lyric_lines").setValue(3.0);
      var el = new dev.swiftclient.core.hud.elements.MusicElement();
      for (int i = 0; i < 40; i++) {
         dev.swiftclient.core.music.MusicState.apply("Billie Jean", "Michael Jackson", true, 88_000L, 294_000L, "bj", "", false);
         if (dev.swiftclient.core.music.MusicLyrics.current().status() != dev.swiftclient.core.music.lyrics.Lyrics.Status.LOADING) {
            break;
         }

         Thread.sleep(250);
      }

      PreviewCanvas c = new PreviewCanvas(200, 110, 3);
      c.fill(0, 0, 200, 110, 0xFF3A5A40);
      for (int i = 0; i < 30; i++) {
         el.draw(c, null, 10, 10);
      }

      c.save(OUT.resolve("music-card.png"));
   }

   @Test
   void sessionAndPerf() throws Exception {
      java.util.Random rnd = new java.util.Random(7);
      for (int i = 0; i < 90; i++) {
         dev.swiftclient.core.session.SessionGraph.sample(i * 10_000L + 10_000L, true, 110 + rnd.nextInt(60) - (i > 60 && i < 66 ? 70 : 0), 38 + rnd.nextInt(30));
      }

      for (int i = 0; i < 600; i++) {
         dev.swiftclient.core.perf.FrameStats.WINDOW.add(i % 97 == 0 ? 48.0 : 6.0 + rnd.nextDouble() * 2.5);
      }

      shot("session-380x260", 380, 260, new SessionScreen(), null);
      shot("session-480x300", 480, 300, new SessionScreen(), null);
      shot("hud-perf-640x360", 640, 360, new HudEditorScreen(), hudSelect(true, "perf"));
   }
}
