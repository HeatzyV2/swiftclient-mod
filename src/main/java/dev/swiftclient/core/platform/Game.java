package dev.swiftclient.core.platform;

import dev.swiftclient.core.config.ConfigStore;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface Game {
   List<Lang> languages();

   String currentLanguage();

   void setLanguage(String code);

   String translate(String key);

   /** Translation with {@code %s} arguments. */
   default String translate(String key, Object... args) {
      return args == null || args.length == 0 ? this.translate(key) : String.format(this.translate(key), args);
   }

   void closeScreen();

   void playClick();

   String getConfig(String key, String def);

   void setConfig(String key, String value);

   Path configDir();

   void runOnGameThread(Runnable task);

   boolean applySession(String username, UUID uuid, String accessToken);

   List<Account> accounts();

   void switchAccount(String uuid);

   void removeAccount(String uuid);

   void addAccount(Consumer<String> onStatus);

   void applyPanorama(String location);

   String getAccessToken();

   String getUuid();

   String getUsername();

   default boolean capeHasElytra(String capeId) {
      return false;
   }

   void loadCapeFrames(String capeId, byte[] png, int frameW, int frameH, BiConsumer<String, Object[]> onReady);

   default void loadImage(String key, byte[] png, BiConsumer<String, Object> onReady) {
   }

   default String gameVersion() {
      return "";
   }

   default boolean inSingleplayer() {
      return false;
   }

   default CompletableFuture<String> hostWorld(String modeJeu, String difficulte, boolean triche) {
      return CompletableFuture.failedFuture(new UnsupportedOperationException("Hosting is not available on this version"));
   }

   default String worldName() {
      return null;
   }

   default void stopHosting() {
   }

   default String hostAddress() {
      return null;
   }

   default void copyToClipboard(String texte) {
   }

   /** The Swift Client configuration (swiftclient.json). */
   ConfigStore config();

   /** Clipboard text, or "" if it cannot be read. */
   default String readClipboard() {
      return "";
   }

   /** Shows a short in-game notification (toast). Safe to call from any thread. */
   default void notify(String title, String message) {
   }

   default boolean supportsBackgroundBlur() {
      return false;
   }

   default boolean supportsCustomCrosshair() {
      return false;
   }

   default boolean supportsFreelook() {
      return false;
   }
}
