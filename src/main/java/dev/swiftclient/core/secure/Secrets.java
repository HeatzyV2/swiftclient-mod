package dev.swiftclient.core.secure;

import com.sun.jna.platform.win32.Crypt32Util;
import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.platform.Platform;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;

/**
 * Encrypts tokens before they are written to disk.
 *
 * <ul>
 *   <li>Windows: DPAPI (JNA, shipped with Minecraft). Only the same Windows user on the same machine can
 *       decrypt: a copied config folder is useless elsewhere.</li>
 *   <li>Other systems: AES-GCM with a random key in {@code config/swiftclient-secret.key} (owner-only
 *       permissions). Protects against a shared/synced config file, not against the same user.</li>
 * </ul>
 *
 * Stored form: {@code "dpapi1:<base64>"} or {@code "aes1:<base64>"}. Anything without a prefix is a
 * plaintext value written by an older build: {@link #reveal} returns it as is and the caller re-saves it.
 */
public final class Secrets {
   private static final Logger LOG = Log.get("Secrets");
   private static final String DPAPI = "dpapi1:";
   private static final String AES = "aes1:";
   private static final byte[] ENTROPY = "swiftclient-secrets".getBytes(StandardCharsets.UTF_8);
   private static final int CRYPTPROTECT_UI_FORBIDDEN = 1;
   private static final SecureRandom RNG = new SecureRandom();
   private static volatile Boolean dpapi;
   private static volatile Path keyDirOverride;
   private static volatile byte[] aesKey;

   private Secrets() {
   }

   /** Encrypted form of {@code plain}; null stays null, "" stays "". */
   public static String protect(String plain) {
      if (plain == null || plain.isEmpty()) {
         return plain;
      } else {
         byte[] data = plain.getBytes(StandardCharsets.UTF_8);

         try {
            return useDpapi()
               ? DPAPI + Base64.getEncoder().encodeToString(Crypt32Util.cryptProtectData(data, ENTROPY, CRYPTPROTECT_UI_FORBIDDEN, "Swift Client", null))
               : AES + Base64.getEncoder().encodeToString(aesEncrypt(data));
         } catch (Throwable t) {
            // Never fall back to writing the secret in clear.
            throw new IllegalStateException("Chiffrement du secret impossible", t);
         }
      }
   }

   /**
    * Plain value of a stored secret. Legacy plaintext (no prefix) is returned unchanged. Returns null if
    * it cannot be decrypted (e.g. config copied from another Windows account).
    */
   public static String reveal(String stored) {
      if (stored == null || stored.isEmpty() || !isProtected(stored)) {
         return stored;
      } else {
         try {
            if (stored.startsWith(DPAPI)) {
               byte[] enc = Base64.getDecoder().decode(stored.substring(DPAPI.length()));
               return new String(Crypt32Util.cryptUnprotectData(enc, ENTROPY, CRYPTPROTECT_UI_FORBIDDEN, null), StandardCharsets.UTF_8);
            } else {
               return new String(aesDecrypt(Base64.getDecoder().decode(stored.substring(AES.length()))), StandardCharsets.UTF_8);
            }
         } catch (Throwable t) {
            LOG.warn("Secret illisible sur cette machine / ce compte Windows : reconnexion necessaire ({})", t.getClass().getSimpleName());
            return null;
         }
      }
   }

   public static boolean isProtected(String stored) {
      return stored != null && (stored.startsWith(DPAPI) || stored.startsWith(AES));
   }

   // --- Backends ---

   private static boolean useDpapi() {
      Boolean d = dpapi;
      if (d == null) {
         d = System.getProperty("os.name", "").toLowerCase().contains("win");
         if (d) {
            try {
               Crypt32Util.class.getName();
            } catch (Throwable t) {
               LOG.warn("DPAPI indisponible, repli sur AES", t);
               d = false;
            }
         }

         dpapi = d;
      }

      return d;
   }

   /** Test support: force a backend and a key folder. */
   static void configureForTests(boolean useDpapi, Path keyDir) {
      dpapi = useDpapi;
      keyDirOverride = keyDir;
      aesKey = null;
   }

   private static synchronized byte[] key() throws Exception {
      if (aesKey == null) {
         Path dir = keyDirOverride != null ? keyDirOverride : Platform.game().configDir();
         Path file = dir.resolve("swiftclient-secret.key");
         if (Files.isRegularFile(file)) {
            aesKey = Base64.getDecoder().decode(Files.readString(file).trim());
         } else {
            byte[] k = new byte[32];
            RNG.nextBytes(k);
            Files.createDirectories(dir);
            Files.writeString(file, Base64.getEncoder().encodeToString(k));

            try {
               Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
               // Not a POSIX file system.
            }

            aesKey = k;
         }
      }

      return aesKey;
   }

   private static byte[] aesEncrypt(byte[] data) throws Exception {
      byte[] iv = new byte[12];
      RNG.nextBytes(iv);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(128, iv));
      byte[] enc = c.doFinal(data);
      return ByteBuffer.allocate(iv.length + enc.length).put(iv).put(enc).array();
   }

   private static byte[] aesDecrypt(byte[] blob) throws Exception {
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(128, blob, 0, 12));
      return c.doFinal(blob, 12, blob.length - 12);
   }
}
