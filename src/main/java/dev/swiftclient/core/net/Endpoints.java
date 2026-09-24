package dev.swiftclient.core.net;

import dev.swiftclient.core.log.Log;
import java.net.URI;
import org.slf4j.Logger;

/**
 * Where Swift Client services live. Each one can be overridden with a JVM property or an environment
 * variable, so a launcher (or a developer) can point the client at another deployment.
 *
 * <ul>
 *   <li>{@code -Dswiftclient.api=<url>} / {@code SWIFTCLIENT_API}: backend, {@code off} disables it</li>
 *   <li>{@code -Dswiftclient.relay=<host:port>} / {@code SWIFTCLIENT_RELAY}: world hosting relay</li>
 * </ul>
 */
public final class Endpoints {
   private static final Logger LOG = Log.get("Net");
   public static final String DEFAULT_API = "http://151.240.30.3:10049";
   /** Relay control port on the backend host, unless overridden. */
   public static final int DEFAULT_RELAY_PORT = 7777;
   private static volatile String API = resolveApi();
   private static volatile String RELAY = resolveRelay();

   private Endpoints() {
   }

   /** Test support: point the client at a local server (null disables the backend). */
   static void overrideForTests(String api, String relay) {
      API = api;
      RELAY = relay;
   }

   /** Backend base URL without trailing slash, or null when disabled / invalid. */
   public static String api() {
      return API;
   }

   public static boolean backendEnabled() {
      return API != null;
   }

   /** Relay "host:port", or null when no backend and no explicit relay. */
   public static String relay() {
      return RELAY;
   }

   public static String relayHost() {
      return RELAY == null ? null : RELAY.substring(0, RELAY.lastIndexOf(':'));
   }

   public static int relayPort() {
      return RELAY == null ? -1 : Integer.parseInt(RELAY.substring(RELAY.lastIndexOf(':') + 1));
   }

   private static String setting(String property, String env) {
      String v = System.getProperty(property);
      if (v == null || v.isBlank()) {
         v = System.getenv(env);
      }

      return v == null || v.isBlank() ? null : v.trim();
   }

   private static String resolveApi() {
      String v = setting("swiftclient.api", "SWIFTCLIENT_API");
      if (v == null) {
         v = DEFAULT_API;
      }

      if ("off".equalsIgnoreCase(v)) {
         LOG.info("Backend desactive : cosmetiques, badges, amis et heartbeat coupes");
         return null;
      } else {
         while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
         }

         if (!v.startsWith("https://") && !v.startsWith("http://")) {
            LOG.warn("swiftclient.api invalide (http/https attendu) : {}", v);
            return null;
         } else {
            LOG.info("Backend : {}", v);
            return v;
         }
      }
   }

   private static String resolveRelay() {
      String v = setting("swiftclient.relay", "SWIFTCLIENT_RELAY");
      if (v != null) {
         if (v.lastIndexOf(':') > 0) {
            try {
               Integer.parseInt(v.substring(v.lastIndexOf(':') + 1));
               return v;
            } catch (NumberFormatException ignored) {
            }
         }

         LOG.warn("swiftclient.relay invalide (host:port attendu) : {}", v);
         return null;
      } else if (API == null) {
         return null;
      } else {
         return URI.create(API).getHost() + ":" + DEFAULT_RELAY_PORT;
      }
   }
}
