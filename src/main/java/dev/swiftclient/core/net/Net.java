package dev.swiftclient.core.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The one HTTP client, background pool and scheduler shared by every Swift Client network feature
 * (backend, social, Mojang, Spotify...). Daemon threads: they never keep the game from closing.
 */
public final class Net {
   public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
   /** Blocking work: HTTP calls, file reads, image decoding. */
   public static final ExecutorService IO = Executors.newFixedThreadPool(4, daemon("swiftclient-io"));
   /** Periodic and delayed tasks. Must stay short: hand real work to {@link #IO}. */
   public static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(daemon("swiftclient-scheduler"));
   public static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(8L))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .executor(IO)
      .build();

   private Net() {
   }

   private static ThreadFactory daemon(String name) {
      AtomicInteger n = new AtomicInteger();
      return r -> {
         Thread t = new Thread(r, name + "-" + n.incrementAndGet());
         t.setDaemon(true);
         return t;
      };
   }
}
