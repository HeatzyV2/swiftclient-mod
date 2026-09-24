package dev.swiftclient.core.account;

import java.time.Instant;
import java.util.UUID;

public class AccountEntry {
   private final String username;
   private final UUID uuid;
   private final AccountEntry.Type type;
   private String accessToken;
   private String refreshToken;
   private Instant expiresAt;

   public AccountEntry(String username) {
      this.username = username;
      this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
      this.type = AccountEntry.Type.OFFLINE;
   }

   public AccountEntry(String username, UUID uuid, String accessToken, String refreshToken) {
      this.username = username;
      this.uuid = uuid;
      this.type = AccountEntry.Type.MICROSOFT;
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
   }

   public String getUsername() {
      return this.username;
   }

   public UUID getUuid() {
      return this.uuid;
   }

   public AccountEntry.Type getType() {
      return this.type;
   }

   public String getAccessToken() {
      return this.accessToken;
   }

   public String getRefreshToken() {
      return this.refreshToken;
   }

   public Instant getExpiresAt() {
      return this.expiresAt;
   }

   public void setAccessToken(String t) {
      this.accessToken = t;
   }

   public void setRefreshToken(String t) {
      this.refreshToken = t;
   }

   public void setExpiresAt(Instant t) {
      this.expiresAt = t;
   }

   public boolean isMicrosoft() {
      return this.type == AccountEntry.Type.MICROSOFT;
   }

   public boolean isOffline() {
      return this.type == AccountEntry.Type.OFFLINE;
   }

   public boolean isExpired() {
      return this.expiresAt != null && Instant.now().isAfter(this.expiresAt);
   }

   public String serialize() {
      if (this.type == AccountEntry.Type.MICROSOFT) {
         long epoch = this.expiresAt != null ? this.expiresAt.getEpochSecond() : 0L;
         return "MICROSOFT:"
            + this.username
            + ":"
            + this.uuid
            + ":"
            + (this.accessToken != null ? this.accessToken : "")
            + ":"
            + (this.refreshToken != null ? this.refreshToken : "")
            + ":"
            + epoch;
      } else {
         return "OFFLINE:" + this.username;
      }
   }

   public static AccountEntry deserialize(String line) {
      String[] parts = line.split(":", 6);
      if (parts.length == 0) {
         return null;
      } else if ("OFFLINE".equals(parts[0]) && parts.length >= 2) {
         return new AccountEntry(parts[1]);
      } else if ("MICROSOFT".equals(parts[0]) && parts.length >= 5) {
         try {
            UUID uuid = UUID.fromString(parts[2]);
            String at = parts[3].isEmpty() ? null : parts[3];
            String rt = parts[4].isEmpty() ? null : parts[4];
            AccountEntry e = new AccountEntry(parts[1], uuid, at, rt);
            if (parts.length == 6 && !parts[5].isEmpty()) {
               long epoch = Long.parseLong(parts[5]);
               if (epoch > 0L) {
                  e.setExpiresAt(Instant.ofEpochSecond(epoch));
               }
            }

            return e;
         } catch (IllegalArgumentException ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static enum Type {
      MICROSOFT,
      OFFLINE;
   }
}
