package dev.swiftclient.core.secure;

import java.nio.file.Path;

/** Lets tests of other packages pick the AES backend with a key in a temporary folder. */
public final class SecretsTestSupport {
   private SecretsTestSupport() {
   }

   public static void useAes(Path keyDir) {
      Secrets.configureForTests(false, keyDir);
   }
}
