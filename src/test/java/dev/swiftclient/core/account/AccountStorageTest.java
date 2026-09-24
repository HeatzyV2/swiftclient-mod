package dev.swiftclient.core.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.swiftclient.core.secure.SecretsTestSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AccountStorageTest {
   private static final UUID ID = UUID.fromString("0f5c1f4e-8d7b-4f2a-9c1e-2b3a4d5e6f70");
   @TempDir
   Path dir;

   @BeforeEach
   void setUp() {
      SecretsTestSupport.useAes(this.dir);
   }

   @Test
   void legacyPlaintextFileIsMigratedEncryptedAndWiped() throws Exception {
      Files.writeString(
         this.dir.resolve("swiftclient-accounts.dat"),
         "MICROSOFT:Steve:" + ID + ":access-123:refresh-456:1893456000\nOFFLINE:Alex\ngarbage line\n"
      );

      List<AccountEntry> loaded = new AccountStorage(this.dir).load();
      assertEquals(2, loaded.size());
      AccountEntry steve = loaded.get(0);
      assertEquals("Steve", steve.getUsername());
      assertEquals(ID, steve.getUuid());
      assertEquals("access-123", steve.getAccessToken());
      assertEquals("refresh-456", steve.getRefreshToken());
      assertEquals(Instant.ofEpochSecond(1893456000L), steve.getExpiresAt());
      assertTrue(loaded.get(1).isOffline());

      assertFalse(Files.exists(this.dir.resolve("swiftclient-accounts.dat")), "plaintext file removed");
      String json = Files.readString(this.dir.resolve("swiftclient-accounts.json"));
      assertFalse(json.contains("access-123"), json);
      assertFalse(json.contains("refresh-456"), json);
      assertTrue(json.contains("aes1:"), json);

      List<AccountEntry> reloaded = new AccountStorage(this.dir).load();
      assertEquals("refresh-456", reloaded.get(0).getRefreshToken(), "second start reads the encrypted file");
   }

   @Test
   void plaintextTokensInTheJsonAreEncryptedOnLoad() throws Exception {
      Files.writeString(
         this.dir.resolve("swiftclient-accounts.json"),
         "{\"schemaVersion\":1,\"accounts\":[{\"type\":\"microsoft\",\"username\":\"Steve\",\"uuid\":\"" + ID + "\",\"accessToken\":\"a\",\"refreshToken\":\"r\",\"expiresAt\":0}]}"
      );
      AccountEntry steve = new AccountStorage(this.dir).load().get(0);
      assertEquals("r", steve.getRefreshToken());
      assertNull(steve.getExpiresAt());
      assertFalse(Files.readString(this.dir.resolve("swiftclient-accounts.json")).contains("\"r\""));
   }

   @Test
   void unreadableEntriesAreSkipped() throws Exception {
      Files.writeString(
         this.dir.resolve("swiftclient-accounts.json"),
         "{\"accounts\":[{\"type\":\"microsoft\",\"username\":\"Bad\",\"uuid\":\"not-a-uuid\"},{\"type\":\"alien\",\"username\":\"X\"},{\"type\":\"offline\",\"username\":\"Alex\"},7]}"
      );
      List<AccountEntry> loaded = new AccountStorage(this.dir).load();
      assertEquals(1, loaded.size());
      assertEquals("Alex", loaded.get(0).getUsername());
   }

   @Test
   void saveThenLoadKeepsEverything() {
      AccountStorage storage = new AccountStorage(this.dir);
      AccountEntry e = new AccountEntry("Steve", ID, "acc", "ref");
      e.setExpiresAt(Instant.ofEpochSecond(1900000000L));
      assertTrue(storage.save(List.of(e, new AccountEntry("Alex"))));
      List<AccountEntry> loaded = storage.load();
      assertEquals(2, loaded.size());
      assertEquals("acc", loaded.get(0).getAccessToken());
      assertEquals(Instant.ofEpochSecond(1900000000L), loaded.get(0).getExpiresAt());
   }
}
