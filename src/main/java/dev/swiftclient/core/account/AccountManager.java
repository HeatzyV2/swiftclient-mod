package dev.swiftclient.core.account;

import dev.swiftclient.core.platform.Platform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class AccountManager {
   private static final AccountManager INSTANCE = new AccountManager();
   private final List<AccountEntry> accounts = new ArrayList<>();
   private final AccountStorage storage = new AccountStorage();
   private AccountEntry active = null;

   public static AccountManager get() {
      return INSTANCE;
   }

   private AccountManager() {
   }

   public void load() {
      this.accounts.clear();

      for (AccountEntry e : this.storage.load()) {
         this.upsert(e);
      }

      try {
         String current = Platform.game().getUuid();
         if (current != null && !current.isEmpty()) {
            for (AccountEntry e : this.accounts) {
               if (e.getUuid() != null && e.getUuid().toString().replace("-", "").equalsIgnoreCase(current)) {
                  this.active = e;
                  break;
               }
            }
         }
      } catch (Throwable var4) {
      }

      System.out.println("[LC-Account] " + this.accounts.size() + " comptes chargés.");
   }

   public void save() {
      this.storage.save(this.accounts);
   }

   public List<AccountEntry> getAccounts() {
      return Collections.unmodifiableList(this.accounts);
   }

   public Optional<AccountEntry> getActive() {
      return Optional.ofNullable(this.active);
   }

   private void upsert(AccountEntry entry) {
      boolean wasActive = this.active != null && this.active.getUuid().equals(entry.getUuid());
      this.accounts.removeIf(e -> e.getUuid().equals(entry.getUuid()));
      this.accounts.add(entry);
      if (wasActive) {
         this.active = entry;
      }
   }

   public AccountEntry addOfflineAccount(String username) {
      AccountEntry entry = new AccountEntry(username.trim());
      this.upsert(entry);
      this.save();
      return entry;
   }

   public void startBrowserLogin(Consumer<String> onStatus, Consumer<AccountEntry> onDone) {
      new MicrosoftAuthFlow()
         .browserLogin(msg -> Platform.game().runOnGameThread(() -> onStatus.accept(msg)))
         .thenAccept(entry -> Platform.game().runOnGameThread(() -> {
            this.upsert(entry);
            this.save();
            onDone.accept(entry);
         }))
         .exceptionally(ex -> {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            Platform.game().runOnGameThread(() -> {
               System.err.println("[LC-Account] MSA login failed: " + cause.getMessage());
               onStatus.accept("Erreur : " + cause.getMessage());
               onDone.accept(null);
            });
            return null;
         });
   }

   public boolean switchTo(AccountEntry entry) {
      boolean ok = Platform.game().applySession(entry.getUsername(), entry.getUuid(), entry.isMicrosoft() ? entry.getAccessToken() : "");
      if (ok) {
         this.active = entry;
         System.out.println("[LC-Account] Session swappée → " + entry.getUsername() + " (profileKeys reset)");
      } else {
         System.err.println("[LC-Account] Switch failed pour " + entry.getUsername());
      }

      return ok;
   }

   public void refreshAndSwitch(AccountEntry entry, Consumer<Boolean> onDone) {
      if (entry.isMicrosoft() && entry.getRefreshToken() != null) {
         CompletableFuture.runAsync(() -> {
            try {
               AccountEntry refreshed = new MicrosoftAuthFlow().refreshAccessToken(entry.getRefreshToken());
               entry.setAccessToken(refreshed.getAccessToken());
               entry.setRefreshToken(refreshed.getRefreshToken());
               entry.setExpiresAt(refreshed.getExpiresAt());
               this.save();
               Platform.game().runOnGameThread(() -> {
                  this.switchTo(entry);
                  onDone.accept(true);
               });
            } catch (Exception var4) {
               System.err.println("[LC-Account] Refresh failed, fallback direct switch: " + var4.getMessage());
               Platform.game().runOnGameThread(() -> {
                  this.switchTo(entry);
                  onDone.accept(false);
               });
            }
         });
      } else {
         boolean ok = this.switchTo(entry);
         Platform.game().runOnGameThread(() -> onDone.accept(ok));
      }
   }

   public void remove(AccountEntry entry) {
      this.accounts.remove(entry);
      if (this.active == entry) {
         this.active = null;
      }

      this.save();
   }
}
