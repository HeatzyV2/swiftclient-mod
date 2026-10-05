package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.Profiles;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.ui.Notifications;
import java.util.ArrayList;
import java.util.List;

/**
 * HUD scenes: pick a profile for singleplayer, for multiplayer and for combat, and the client switches to it by
 * itself, like scenes in a streaming tool. A profile holds every module setting and the HUD layout, so one
 * scene can be a clean building HUD and another a full PvP one.
 */
public final class ScenesModule extends Module {
   private static final String KEY_SP = "scenes.singleplayer";
   private static final String KEY_MP = "scenes.multiplayer";
   private static final String KEY_PVP = "scenes.combat";
   /** How long after the last fight the combat scene lingers. */
   private static final long COMBAT_GRACE_MS = 8_000L;
   private static final long MIN_GAP_MS = 2_500L;

   private long lastCombatMs;
   private long lastSwitchMs;

   public ScenesModule() {
      super("scenes", "HUD scenes", "Switch profile by itself: one for singleplayer, one for servers, one for fights.", "Interface", "profiles", false);
      this.action("singleplayer", "Singleplayer scene", () -> label(KEY_SP), () -> cycle(KEY_SP))
         .desc("The profile used when you play alone. Click to change. Create profiles in the Profiles page.");
      this.action("multiplayer", "Multiplayer scene", () -> label(KEY_MP), () -> cycle(KEY_MP))
         .desc("The profile used on servers.");
      this.action("combat", "Combat scene", () -> label(KEY_PVP), () -> cycle(KEY_PVP))
         .desc("The profile used while you are fighting, and for a few seconds after.");
   }

   private static String stored(String key) {
      try {
         String v = Platform.game().getConfig(key, "");
         return v == null ? "" : v.trim();
      } catch (Throwable ignored) {
         return "";
      }
   }

   private static String label(String key) {
      String v = stored(key);
      return v.isEmpty() || !Profiles.noms().contains(v) ? Tr.of("swift.scenes.off") : v;
   }

   /** off, then every profile in turn, then off again. */
   private static void cycle(String key) {
      List<String> options = new ArrayList<>();
      options.add("");
      options.addAll(Profiles.noms());
      int i = options.indexOf(stored(key));
      String next = options.get((i + 1) % options.size());
      try {
         Platform.game().setConfig(key, next);
      } catch (Throwable ignored) {
      }
   }

   @Override
   protected void onTick() {
      HudData d = HudManager.lastData();
      if (d == null || !d.inWorld()) {
         return;
      }
      long now = System.currentTimeMillis();
      if (d.combat() != null && d.combat().target() != null) {
         this.lastCombatMs = now;
      }
      boolean fighting = now - this.lastCombatMs < COMBAT_GRACE_MS;
      String address = d.serverAddress();
      boolean multiplayer = address != null && !address.isBlank();

      String wanted = fighting ? stored(KEY_PVP) : "";
      if (wanted.isEmpty()) {
         wanted = stored(multiplayer ? KEY_MP : KEY_SP);
      }
      if (wanted.isEmpty() || wanted.equals(Profiles.actif()) || !Profiles.noms().contains(wanted)) {
         return;
      }
      if (now - this.lastSwitchMs < MIN_GAP_MS) {
         return;
      }
      this.lastSwitchMs = now;
      Profiles.activer(wanted);
      Notifications.push(Tr.of("swift.scenes.title"), Tr.of("swift.scenes.switched", wanted), "profiles");
   }
}
