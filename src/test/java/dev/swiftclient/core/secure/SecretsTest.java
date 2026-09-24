package dev.swiftclient.core.secure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecretsTest {
   private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.refresh-token.with:colons/and+plus";

   @Test
   void aesRoundTripAndNoPlaintextOnDisk(@TempDir Path dir) throws Exception {
      Secrets.configureForTests(false, dir);
      String stored = Secrets.protect(TOKEN);
      assertTrue(stored.startsWith("aes1:"), stored);
      assertFalse(stored.contains("refresh-token"));
      assertNotEquals(stored, Secrets.protect(TOKEN), "random IV: same secret, different ciphertext");
      assertEquals(TOKEN, Secrets.reveal(stored));
      assertTrue(Files.isRegularFile(dir.resolve("swiftclient-secret.key")));
   }

   @Test
   void anotherKeyCannotDecrypt(@TempDir Path a, @TempDir Path b) {
      Secrets.configureForTests(false, a);
      String stored = Secrets.protect(TOKEN);
      Secrets.configureForTests(false, b);
      assertNull(Secrets.reveal(stored), "copied config without its key");
   }

   @Test
   void legacyPlaintextPassesThrough(@TempDir Path dir) {
      Secrets.configureForTests(false, dir);
      assertEquals("old-token", Secrets.reveal("old-token"));
      assertFalse(Secrets.isProtected("old-token"));
      assertNull(Secrets.protect(null));
      assertEquals("", Secrets.protect(""));
   }

   @Test
   void dpapiRoundTripOnWindows(@TempDir Path dir) {
      assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"), "DPAPI only exists on Windows");
      Secrets.configureForTests(true, dir);
      String stored = Secrets.protect(TOKEN);
      assertTrue(stored.startsWith("dpapi1:"), stored);
      assertEquals(TOKEN, Secrets.reveal(stored));
      assertFalse(Files.exists(dir.resolve("swiftclient-secret.key")), "DPAPI needs no key file");
   }
}
