package dev.swiftclient.core;

import dev.swiftclient.core.platform.KeyCodes;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeyCodesTest {
   @Test void oldSettingsAndUiShortcutsSurviveSdl() {
      assertEquals(71, KeyCodes.fromSdl(10)); // G / radial
      assertEquals(256, KeyCodes.fromSdl(41)); // Escape
      assertEquals(259, KeyCodes.fromSdl(42)); // Backspace
      assertEquals(49, KeyCodes.fromSdl(30)); // 1
      for (int key : new int[]{32, 48, 49, 65, 70, 71, 90, 256, 257, 259, 263, 290, 313, 320, 335, 340, 347}) {
         assertEquals(key, KeyCodes.fromSdl(KeyCodes.toSdl(key)));
      }
      assertEquals(0, KeyCodes.toSdl(-1));
      assertEquals(290, KeyCodes.toSdl(KeyCodes.fromSdl(290))); // SDL-only key
   }

   @Test void sdlMouseButtonsKeepLeftRightAndMiddleActions() {
      assertEquals(0, KeyCodes.mouseFromSdl(1));
      assertEquals(1, KeyCodes.mouseFromSdl(3));
      assertEquals(2, KeyCodes.mouseFromSdl(2));
   }
}
