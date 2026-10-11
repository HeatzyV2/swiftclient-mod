package dev.swiftclient.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.swiftclient.core.chat.ChatRules;
import dev.swiftclient.core.perf.FrameStats;
import dev.swiftclient.core.perf.FrameWindow;
import dev.swiftclient.core.update.Versions;
import dev.swiftclient.core.combat.CombatRecap;
import dev.swiftclient.world.WaypointCodec;
import dev.swiftclient.world.Waypoints;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** The rules behind the new features, tested without the game. */
class FeatureLogicTest {
   @Test
   void frameWindowSeesStutterThatTheAverageHides() {
      FrameWindow w = new FrameWindow(600);
      for (int i = 0; i < 495; i++) {
         w.add(5.0); // 200 fps
      }

      for (int i = 0; i < 5; i++) {
         w.add(60.0); // five hitches
      }

      assertTrue(w.averageFps() > 150.0, "average hides the hitches: " + w.averageFps());
      assertTrue(w.lowFps(1.0) < 30.0, "1% low shows them: " + w.lowFps(1.0));
      assertEquals(500, w.size());
   }

   @Test
   void frameWindowIgnoresPausesAndKeepsOrder() {
      FrameWindow w = new FrameWindow(16);
      w.add(5000.0);
      assertEquals(0, w.size());
      for (int i = 1; i <= 20; i++) {
         w.add(i);
      }

      float[] last = w.recent(4);
      assertEquals(4, last.length);
      assertEquals(17.0F, last[0]);
      assertEquals(20.0F, last[3]);
   }

   @Test
   void adviceMatchesTheSymptom() {
      assertNull(FrameStats.advise(0, 0, 12));
      assertNull(FrameStats.advise(144, 110, 12));
      assertEquals(FrameStats.Advice.FAR_VIEW, FrameStats.advise(40, 30, 16));
      assertEquals(FrameStats.Advice.LOW_FPS, FrameStats.advise(40, 30, 8));
      assertEquals(FrameStats.Advice.STUTTER, FrameStats.advise(144, 40, 12));
   }

   @Test
   void combatRecapEndsAfterQuietAndReportsOnce() {
      AtomicLong now = new AtomicLong(1000);
      CombatRecap r = new CombatRecap(now::get);
      r.swing();
      now.addAndGet(500);
      r.hit(1);
      r.swing();
      now.addAndGet(500);
      r.hit(2);
      r.tookDamage(3.0);
      assertTrue(r.inFight());
      assertNull(r.tick(), "still fighting");

      now.addAndGet(CombatRecap.IDLE_MS);
      CombatRecap.Summary s = r.tick();
      assertNotNull(s);
      assertEquals(2, s.hits());
      assertEquals(2, s.swings());
      assertEquals(100, s.accuracy());
      assertEquals(2, s.bestCombo());
      assertEquals(3.0, s.damageTaken());
      assertEquals(1000L, s.durationMs());
      assertNull(r.tick(), "reported once");
      assertFalse(r.inFight());
      assertEquals(s, r.last());
   }

   @Test
   void aScuffleIsNotWorthARecap() {
      AtomicLong now = new AtomicLong(0);
      CombatRecap r = new CombatRecap(now::get);
      r.swing();
      now.addAndGet(CombatRecap.IDLE_MS);
      assertNull(r.tick());
      assertNull(r.last());
   }

   @Test
   void waypointCodeRoundTrips() {
      List<Waypoints.Waypoint> in = List.of(
         new Waypoints.Waypoint("Base", 10, 64, -20, "minecraft:overworld", 0xFF3B82F6),
         new Waypoints.Waypoint("Nether portal", -5, 70, 8, "minecraft:the_nether", 0xFFFF6B6B)
      );
      String code = WaypointCodec.encode(in);
      assertTrue(code.startsWith(WaypointCodec.PREFIX));
      assertEquals(in, WaypointCodec.decode(code));
   }

   @Test
   void waypointCodeRejectsGarbage() {
      assertThrows(IllegalArgumentException.class, () -> WaypointCodec.decode("hello"));
      assertThrows(IllegalArgumentException.class, () -> WaypointCodec.decode(WaypointCodec.PREFIX + "!!!"));
      assertThrows(IllegalArgumentException.class, () -> WaypointCodec.decode(null));
   }

   @Test
   void chatRulesRecogniseJoinsWhispersAndMentions() {
      assertTrue(ChatRules.isJoinLeave("Steve joined the game"));
      assertTrue(ChatRules.isJoinLeave("Steve a quitté la partie"));
      assertTrue(ChatRules.isJoinLeave("[+] Steve"));
      assertFalse(ChatRules.isJoinLeave("Steve: I joined the game late"));

      assertTrue(ChatRules.isWhisper("Alex whispers to you: hi"));
      assertTrue(ChatRules.isWhisper("[Alex -> moi] salut"));
      assertTrue(ChatRules.isWhisper("From Alex: hey"));
      assertFalse(ChatRules.isWhisper("<Alex> hey everyone"));

      assertTrue(ChatRules.mentions("<Alex> hey Zor4tox, you there?", "Zor4tox", List.of()));
      assertTrue(ChatRules.mentions("Alex: ping zor4tox", "Zor4tox", List.of()));
      assertFalse(ChatRules.mentions("<Zor4tox> hello", "Zor4tox", List.of()));
      assertFalse(ChatRules.mentions("Zor4tox » hello", "Zor4tox", List.of()));
      assertFalse(ChatRules.mentions("<Alex> Zor4toxx is great", "Zor4tox", List.of()));
      assertTrue(ChatRules.mentions("<Alex> elysia is down", "Zor4tox", List.of("elysia")));
   }

   @Test
   void blockedWordsMatchWholeWordsOnly() {
      List<String> words = ChatRules.words(" Spam, , x ,discord.gg ");
      assertEquals(List.of("spam", "discord.gg"), words);
      assertTrue(ChatRules.blocked("join discord.gg/abc now", words));
      assertTrue(ChatRules.blocked("this is SPAM!", words));
      assertFalse(ChatRules.blocked("spammer", words));
      assertFalse(ChatRules.blocked("nothing here", words));
   }

   @Test
   void versionsCompareLikeSemver() {
      assertTrue(Versions.isNewer("2.3.0", "2.3.1"));
      assertTrue(Versions.isNewer("2.3.0", "v2.10.0"));
      assertTrue(Versions.isNewer("2.3.0-beta.1", "2.3.0"));
      assertFalse(Versions.isNewer("2.3.0", "2.3.0"));
      assertFalse(Versions.isNewer("2.3.0", "2.2.9"));
      assertFalse(Versions.isNewer("2.3.0", "2.3.0-beta.2"));
      assertFalse(Versions.isNewer("?", "9.9.9"));
      assertFalse(Versions.isNewer("2.3.0", ""));
   }
}
