package dev.swiftclient.core.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class PlatformFeedTest {
   private static final String BOOTSTRAP = """
      {"success":true,"data":{
        "announcements":[
          {"id":"a1","type":"warning","title":"Restart tonight","body":"Servers restart at 22:00","linkUrl":null,"dismissible":true},
          {"id":"","type":"info","title":"no id is ignored","body":""},
          {"id":"a2","type":"info","title":"","body":"no title is ignored"}
        ],
        "maintenance":{"active":true,"message":{"en":"Upgrading","fr":"Mise à niveau"}},
        "config":{"mod.disabledModules":["zoom","hud_fps","Bad Id","x/y"],"other":1}
      },"meta":{"requestId":"r"}}
      """;

   @Test
   void readsAnnouncementsMaintenanceAndKillSwitch() {
      PlatformFeed.Snapshot s = PlatformFeed.parse(JsonParser.parseString(BOOTSTRAP), "fr");
      assertNotNull(s);
      assertEquals(1, s.announcements().size());
      assertEquals("a1", s.announcements().get(0).id());
      assertEquals("warning", s.announcements().get(0).type());
      assertEquals("Mise à niveau", s.maintenance());
      assertTrue(s.disabledModules().contains("zoom"));
      assertTrue(s.disabledModules().contains("hud_fps"));
      assertFalse(s.disabledModules().contains("Bad Id"), "ids that are not module ids are dropped");
      assertEquals(2, s.disabledModules().size());
   }

   @Test
   void maintenanceFallsBackToEnglishThenFrench() {
      var m = JsonParser.parseString("{\"en\":\"Back soon\",\"fr\":\"Bientôt\"}");
      assertEquals("Back soon", PlatformFeed.pickLocalized(m, "de"));
      assertEquals("Bientôt", PlatformFeed.pickLocalized(m, "fr_fr"));
      assertEquals("Only fr", PlatformFeed.pickLocalized(JsonParser.parseString("{\"fr\":\"Only fr\"}"), "es"));
      assertEquals("", PlatformFeed.pickLocalized(null, "en"));
   }

   @Test
   void inactiveMaintenanceIsNull() {
      PlatformFeed.Snapshot s = PlatformFeed.parse(JsonParser.parseString(
         "{\"success\":true,\"data\":{\"maintenance\":{\"active\":false,\"message\":{}},\"announcements\":[],\"config\":{}}}"), "en");
      assertNotNull(s);
      assertNull(s.maintenance());
      assertTrue(s.announcements().isEmpty());
   }

   @Test
   void anythingThatIsNotASuccessEnvelopeKeepsThePreviousState() {
      assertNull(PlatformFeed.parse(null, "en"));
      assertNull(PlatformFeed.parse(JsonParser.parseString("[]"), "en"));
      assertNull(PlatformFeed.parse(JsonParser.parseString("{\"success\":false,\"error\":{\"code\":\"X\"}}"), "en"));
      assertNull(PlatformFeed.parse(JsonParser.parseString("{\"message\":\"not found\"}"), "en"));
   }

   @Test
   void unknownModuleIsNeverDisabledByDefault() {
      assertFalse(PlatformFeed.isDisabled("zoom"));
   }
}
