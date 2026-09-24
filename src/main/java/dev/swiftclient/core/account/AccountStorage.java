package dev.swiftclient.core.account;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.secure.Secrets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;

/**
 * Saved accounts, {@code config/swiftclient-accounts.json}. Access and refresh tokens are encrypted with
 * {@link Secrets} (DPAPI on Windows). The pre-v2 {@code swiftclient-accounts.dat} held them in clear: it is
 * migrated once, then overwritten and deleted (no plaintext backup is kept on purpose).
 */
public class AccountStorage {
   private static final Logger LOG = Log.get("Account");
   public static final String FILE = "swiftclient-accounts.json";
   public static final String LEGACY_FILE = "swiftclient-accounts.dat";
   private final Path path;
   private final Path legacy;

   public AccountStorage() {
      this(Platform.game().configDir());
   }

   public AccountStorage(Path dir) {
      this.path = dir.resolve(FILE);
      this.legacy = dir.resolve(LEGACY_FILE);
   }

   public List<AccountEntry> load() {
      if (Files.isRegularFile(this.path)) {
         return this.readJson();
      } else if (Files.isRegularFile(this.legacy)) {
         return this.migrateLegacy();
      } else {
         return new ArrayList<>();
      }
   }

   private List<AccountEntry> readJson() {
      List<AccountEntry> list = new ArrayList<>();
      boolean plaintextFound = false;

      try {
         JsonObject root = JsonParser.parseString(Files.readString(this.path, StandardCharsets.UTF_8)).getAsJsonObject();
         JsonElement arr = root.get("accounts");
         if (arr != null && arr.isJsonArray()) {
            for (JsonElement e : arr.getAsJsonArray()) {
               if (!e.isJsonObject()) {
                  continue;
               }

               JsonObject o = e.getAsJsonObject();
               String type = str(o, "type");
               String username = str(o, "username");
               if (username == null || username.isBlank()) {
                  LOG.warn("Compte sans pseudo ignore");
               } else if ("offline".equals(type)) {
                  list.add(new AccountEntry(username));
               } else if ("microsoft".equals(type)) {
                  try {
                     String at = str(o, "accessToken");
                     String rt = str(o, "refreshToken");
                     plaintextFound |= at != null && !at.isEmpty() && !Secrets.isProtected(at) || rt != null && !rt.isEmpty() && !Secrets.isProtected(rt);
                     AccountEntry entry = new AccountEntry(username, UUID.fromString(str(o, "uuid")), empty(Secrets.reveal(at)), empty(Secrets.reveal(rt)));
                     long exp = o.has("expiresAt") ? o.get("expiresAt").getAsLong() : 0L;
                     if (exp > 0L) {
                        entry.setExpiresAt(Instant.ofEpochSecond(exp));
                     }

                     list.add(entry);
                  } catch (RuntimeException ex) {
                     LOG.warn("Compte {} illisible, ignore ({})", username, ex.getMessage());
                  }
               } else {
                  LOG.warn("Compte {} de type inconnu '{}', ignore", username, type);
               }
            }
         }
      } catch (Exception e) {
         LOG.error("{} illisible", FILE, e);
         return list;
      }

      if (plaintextFound) {
         // Hand-edited or older file: encrypt what was in clear.
         this.save(list);
      }

      return list;
   }

   private List<AccountEntry> migrateLegacy() {
      List<AccountEntry> list = new ArrayList<>();

      try {
         for (String line : Files.readAllLines(this.legacy, StandardCharsets.UTF_8)) {
            line = line.strip();
            if (!line.isEmpty()) {
               AccountEntry entry = AccountEntry.deserialize(line);
               if (entry != null) {
                  list.add(entry);
               } else {
                  LOG.warn("Ligne de compte illisible ignoree lors de la migration");
               }
            }
         }
      } catch (IOException e) {
         LOG.error("Lecture de {} impossible, comptes non migres", LEGACY_FILE, e);
         return list;
      }

      if (this.save(list)) {
         wipe(this.legacy);
         LOG.info("{} compte(s) migre(s) vers {} avec tokens chiffres ; ancien fichier en clair efface", list.size(), FILE);
      } else {
         LOG.error("Migration des comptes non enregistree : {} laisse en place", LEGACY_FILE);
      }

      return list;
   }

   /** Writes every account; tokens encrypted. Returns false if nothing could be written. */
   public boolean save(List<AccountEntry> accounts) {
      JsonArray arr = new JsonArray();

      try {
         for (AccountEntry a : accounts) {
            JsonObject o = new JsonObject();
            o.addProperty("type", a.isMicrosoft() ? "microsoft" : "offline");
            o.addProperty("username", a.getUsername());
            if (a.isMicrosoft()) {
               o.addProperty("uuid", a.getUuid().toString());
               o.addProperty("accessToken", Secrets.protect(a.getAccessToken() == null ? "" : a.getAccessToken()));
               o.addProperty("refreshToken", Secrets.protect(a.getRefreshToken() == null ? "" : a.getRefreshToken()));
               o.addProperty("expiresAt", a.getExpiresAt() == null ? 0L : a.getExpiresAt().getEpochSecond());
            }

            arr.add(o);
         }
      } catch (IllegalStateException e) {
         LOG.error("Chiffrement des tokens impossible : comptes non enregistres", e);
         return false;
      }

      JsonObject root = new JsonObject();
      root.addProperty("schemaVersion", 1);
      root.add("accounts", arr);
      Path tmp = this.path.resolveSibling(FILE + ".tmp");

      try {
         Files.createDirectories(this.path.getParent());
         Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);

         try {
            Files.move(tmp, this.path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, this.path, StandardCopyOption.REPLACE_EXISTING);
         }

         return true;
      } catch (IOException e) {
         LOG.error("Enregistrement des comptes impossible", e);
         return false;
      }
   }

   /** Overwrites the file with zeros before deleting it, so the plaintext tokens are not left in free space. */
   static void wipe(Path file) {
      try {
         long size = Files.size(file);
         Files.write(file, new byte[(int)Math.min(size, Integer.MAX_VALUE)]);
         Files.delete(file);
      } catch (IOException e) {
         LOG.warn("Effacement de {} impossible", file.getFileName(), e);
      }
   }

   private static String str(JsonObject o, String key) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
   }

   private static String empty(String s) {
      return s == null || s.isEmpty() ? null : s;
   }
}
