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

   void setLanguage(String var1);

   String translate(String var1);

   void closeScreen();

   void playClick();

   String getConfig(String var1, String var2);

   void setConfig(String var1, String var2);

   Path configDir();

   void runOnGameThread(Runnable var1);

   boolean applySession(String var1, UUID var2, String var3);

   List<Account> accounts();

   void switchAccount(String var1);

   void removeAccount(String var1);

   void addAccount(Consumer<String> var1);

   void applyPanorama(String var1);

   String getAccessToken();

   String getUuid();

   String getUsername();

   default boolean capeHasElytra(String capeId) {
      return false;
   }

   void loadCapeFrames(String var1, byte[] var2, int var3, int var4, BiConsumer<String, Object[]> var5);

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
