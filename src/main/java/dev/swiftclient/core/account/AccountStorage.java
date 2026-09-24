package dev.swiftclient.core.account;

import dev.swiftclient.core.platform.Platform;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class AccountStorage {
   private final Path path;

   public AccountStorage() {
      this.path = Platform.game().configDir().resolve("swiftclient-accounts.dat");
   }

   public AccountStorage(Path path) {
      this.path = path;
   }

   public List<AccountEntry> load() {
      List<AccountEntry> list = new ArrayList<>();
      if (!Files.exists(this.path)) {
         return list;
      } else {
         String line;
         try (BufferedReader reader = Files.newBufferedReader(this.path)) {
            while ((line = reader.readLine()) != null) {
               line = line.strip();
               if (!line.isEmpty()) {
                  AccountEntry entry = AccountEntry.deserialize(line);
                  if (entry != null) {
                     list.add(entry);
                  }
               }
            }
         } catch (IOException var7) {
            System.err.println("[SwiftClient/Account] Load failed: " + var7.getMessage());
         }

         return list;
      }
   }

   public void save(List<AccountEntry> accounts) {
      try {
         Files.createDirectories(this.path.getParent());

         try (BufferedWriter writer = Files.newBufferedWriter(this.path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (AccountEntry acc : accounts) {
               writer.write(acc.serialize());
               writer.newLine();
            }
         }
      } catch (IOException var7) {
         System.err.println("[SwiftClient/Account] Save failed: " + var7.getMessage());
      }
   }
}
