package dev.swiftclient.core.platform;

/**
 * Translations for core code, resolved by the game from {@code assets/swiftclient/lang/*.json}.
 * Every key used with {@link #of} must exist in en_us.json and fr_fr.json (checked by LangTest).
 */
public final class Tr {
   private Tr() {
   }

   /** Translated text for {@code key}, with {@code %s} placeholders filled from {@code args}. */
   public static String of(String key, Object... args) {
      String t = raw(key, args);
      return t == null ? key : t;
   }

   /** Translated text for {@code key}, or {@code fallback} when the key has no translation. */
   public static String orDefault(String key, String fallback) {
      String t = raw(key);
      return t == null || t.equals(key) ? fallback : t;
   }

   private static String raw(String key, Object... args) {
      try {
         return Platform.game().translate(key, args);
      } catch (Throwable ignored) {
         return null;
      }
   }
}
