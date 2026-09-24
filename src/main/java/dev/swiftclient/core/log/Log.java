package dev.swiftclient.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SLF4J loggers named {@code SwiftClient/<area>}, e.g. {@code Log.get("Cape")}. */
public final class Log {
   public static final Logger ROOT = LoggerFactory.getLogger("SwiftClient");

   private Log() {
   }

   public static Logger get(String area) {
      return LoggerFactory.getLogger("SwiftClient/" + area);
   }
}
