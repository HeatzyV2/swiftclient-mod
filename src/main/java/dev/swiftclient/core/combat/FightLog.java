package dev.swiftclient.core.combat;

/** The one recap tracker of the running game: fed by the combat tracker, read by the module and the Session page. */
public final class FightLog {
   public static final CombatRecap RECAP = new CombatRecap(System::currentTimeMillis);

   private FightLog() {
   }
}
