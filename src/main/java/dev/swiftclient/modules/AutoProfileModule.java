package dev.swiftclient.modules;

import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.mods.Profiles;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.ui.Notifications;
import dev.swiftclient.input.SwiftKeys;
import java.util.Locale;
import java.util.Set;

/**
 * Profiles that follow you: tie a profile to a server and it switches on when you join, then goes back to the
 * previous one when you leave. Also a key to cycle profiles, and three ready-made ones (PvP, Build, Stream).
 */
public final class AutoProfileModule extends Module {
   private static final String KEY_PREFIX = "autoprofile.";
   private static final Set<String> PVP_ON = Set.of(
      "hud_cps", "hud_combo", "hud_reach", "hud_keystrokes", "hud_pvpinfo", "hud_armor", "hud_ping", "hud_fps", "hit_color", "zoom", "combat_recap"
   );
   private static final Set<String> PVP_OFF = Set.of("hud_minimap", "waypoints", "streamer");
   private static final Set<String> BUILD_ON = Set.of("hud_coords", "hud_direction", "hud_biome", "hud_minimap", "waypoints", "fullbright", "hud_fps");
   private static final Set<String> BUILD_OFF = Set.of("hud_combo", "hud_reach", "hud_pvpinfo", "hud_keystrokes", "hud_cps", "streamer");
   private static final Set<String> STREAM_ON = Set.of("streamer", "hud_keystrokes", "hud_cps", "hud_notifications", "nick_hider");
   private static final Set<String> STREAM_OFF = Set.of("hud_coords", "hud_server", "hud_perf", "hud_memory", "hud_ping");
   public final ModuleSetting auto;
   private String lastAddress = "";
   /** The profile to go back to when we leave a server that switched it. */
   private String returnTo;

   public AutoProfileModule() {
      super("autoprofile", "Auto profiles", "Switch profile by server, cycle profiles with a key, and create PvP, Build and Stream profiles in one click.", "Interface", "profiles", true);
      this.auto = this.toggle("auto", "Switch by server", true).desc("When you join a server tied to a profile, that profile turns on. Leaving brings your previous one back.");
      this.action("bind", "Tie current profile to this server", () -> Tr.of("swift.autoprofile.bind"), this::bind)
         .desc("Join the server first, then press: this profile will turn on every time you join it.");
      this.action("unbind", "Untie this server", () -> Tr.of("swift.autoprofile.unbind"), this::unbind).desc("Stop switching profile on this server.");
      this.action("key", "Cycle key", () -> SwiftKeys.label(SwiftKeys.CYCLE_PROFILE), SwiftKeys::openControls)
         .desc("Press it to go to the next profile. Set it in Options > Controls > Key Binds.");
      this.action("presets", "Create PvP / Build / Stream", () -> Tr.of("swift.autoprofile.presets"), this::createPresets)
         .desc("Adds three ready-made profiles built from your current settings. Existing names are left alone.");
   }

   private static String address() {
      HudData d = HudManager.lastData();
      return d != null && d.inWorld() ? d.serverAddress() : "";
   }

   private static String key(String address) {
      return KEY_PREFIX + address.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._:-]", "_");
   }

   private static void say(String message) {
      Notifications.push(Tr.of("swift.autoprofile.title"), message, "profiles");
      Platform.game().notify(Tr.of("swift.autoprofile.title"), message);
   }

   private void bind() {
      String a = address();
      if (a.isEmpty()) {
         say(Tr.of("swift.autoprofile.need_server"));
      } else {
         Profiles.memoriser();
         Platform.game().setConfig(key(a), Profiles.actif());
         say(Tr.of("swift.autoprofile.bound", Profiles.actif(), a));
      }
   }

   private void unbind() {
      String a = address();
      if (a.isEmpty()) {
         say(Tr.of("swift.autoprofile.need_server"));
      } else {
         Platform.game().setConfig(key(a), "");
         say(Tr.of("swift.autoprofile.unbound", a));
      }
   }

   private void createPresets() {
      int made = 0;
      made += Profiles.creerPreset(Tr.of("swift.autoprofile.preset.pvp"), PVP_ON, PVP_OFF) ? 1 : 0;
      made += Profiles.creerPreset(Tr.of("swift.autoprofile.preset.build"), BUILD_ON, BUILD_OFF) ? 1 : 0;
      made += Profiles.creerPreset(Tr.of("swift.autoprofile.preset.stream"), STREAM_ON, STREAM_OFF) ? 1 : 0;
      say(made == 0 ? Tr.of("swift.autoprofile.presets_exist") : Tr.of("swift.autoprofile.presets_made", made));
   }

   @Override
   protected void onTick() {
      while (SwiftKeys.CYCLE_PROFILE.consumeClick()) {
         String next = Profiles.suivant();
         say(Tr.of("swift.autoprofile.active", next));
      }

      String address = address();
      if (address.equals(this.lastAddress)) {
         return;
      }

      this.lastAddress = address;
      if (!this.auto.boolValue()) {
         return;
      }

      String wanted = address.isEmpty() ? "" : Platform.game().getConfig(key(address), "");
      if (!wanted.isEmpty() && Profiles.noms().contains(wanted)) {
         if (!wanted.equals(Profiles.actif())) {
            if (this.returnTo == null) {
               this.returnTo = Profiles.actif();
            }

            Profiles.activer(wanted);
            say(Tr.of("swift.autoprofile.switched", wanted));
         }
      } else if (this.returnTo != null) {
         String back = this.returnTo;
         this.returnTo = null;
         if (Profiles.noms().contains(back) && !back.equals(Profiles.actif())) {
            Profiles.activer(back);
            say(Tr.of("swift.autoprofile.restored", back));
         }
      }
   }
}
