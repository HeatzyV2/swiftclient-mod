package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.combat.CombatRecap;
import dev.swiftclient.core.combat.FightLog;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.session.SessionStats;
import dev.swiftclient.core.ui.Notifications;
import java.util.Locale;

/** When a fight ends, a short recap: hits, accuracy, damage taken, best combo. The last one stays on the Session page. */
public final class CombatRecapModule extends Module {
   public final ModuleSetting toast;
   public final ModuleSetting chat;

   public CombatRecapModule() {
      super("combat_recap", "Combat recap", "A short summary when a fight ends: hits, accuracy, damage taken, best combo.", "PvP", "sword", true);
      this.toast = this.toggle("toast", "Show a notification", true).desc("Pop a notification a few seconds after the fight ends. Needs the Notifications element.");
      this.chat = this.toggle("chat", "Also write it in chat", false).desc("Print the recap in your chat, only visible to you.");
   }

   /** The one-line recap shown in notifications, chat and the Session page. */
   public static String line(CombatRecap.Summary s) {
      return Tr.of(
         "swift.combat.recap.body", s.hits(), s.swings(), s.accuracy(), String.format(Locale.ROOT, "%.1f", s.damageTaken() / 2.0), s.bestCombo(), SessionStats.duration(s.durationMs() / 1000L)
      );
   }

   @Override
   protected void onTick() {
      CombatRecap.Summary s = FightLog.RECAP.tick();
      if (s != null) {
         if (this.toast.boolValue()) {
            Notifications.push(Tr.of("swift.combat.recap.title"), line(s), "sword");
         }

         if (this.chat.boolValue()) {
            Platform.game().chat(Tr.of("swift.combat.recap.title") + ": " + line(s));
         }
      }
   }

   @Override
   protected void onDisable() {
      FightLog.RECAP.reset();
   }
}
