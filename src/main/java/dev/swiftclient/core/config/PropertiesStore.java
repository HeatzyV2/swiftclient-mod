package dev.swiftclient.core.config;

import dev.swiftclient.core.log.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * In-memory view of a {@code .properties} file. The file is read once; writes update memory
 * immediately and reach the disk after {@link #DEBOUNCE_MS} of quiet, through a temporary file
 * moved over the original, so a crash mid-write never leaves a truncated config. The on-disk
 * format is plain {@link Properties}, exactly as before.
 */
public final class PropertiesStore {
   private static final Logger LOG = Log.get("Config");
   private static final long DEBOUNCE_MS = 500L;
   private static final ScheduledExecutorService WRITER = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "swiftclient-config-writer");
      t.setDaemon(true);
      return t;
   });
   private final Path file;
   private final String header;
   private final Map<String, String> values = new ConcurrentHashMap<>();
   private final Object flushLock = new Object();
   private volatile boolean loaded;
   private boolean dirty;
   private ScheduledFuture<?> pending;

   public PropertiesStore(Path file, String header) {
      this.file = file;
      this.header = header;
      Runtime.getRuntime().addShutdownHook(new Thread(this::flush, "swiftclient-config-shutdown"));
   }

   public String get(String key, String def) {
      this.ensureLoaded();
      String v = this.values.get(key);
      return v != null ? v : def;
   }

   public void set(String key, String value) {
      this.ensureLoaded();
      if (value == null) {
         if (this.values.remove(key) == null) {
            return;
         }
      } else if (value.equals(this.values.put(key, value))) {
         return;
      }

      synchronized (this.flushLock) {
         this.dirty = true;
         if (this.pending != null) {
            this.pending.cancel(false);
         }

         this.pending = WRITER.schedule(this::flush, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
      }
   }

   /** Writes pending changes now. Called by the debounce timer and on JVM shutdown. */
   public void flush() {
      synchronized (this.flushLock) {
         if (!this.dirty) {
            return;
         }

         this.dirty = false;
         this.pending = null;
         Properties props = new Properties();
         props.putAll(this.values);
         Path tmp = this.file.resolveSibling(this.file.getFileName() + ".tmp");

         try {
            Files.createDirectories(this.file.getParent());
            try (OutputStream out = Files.newOutputStream(tmp)) {
               props.store(out, this.header);
            }

            try {
               Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
               Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING);
            }
         } catch (IOException e) {
            this.dirty = true;
            LOG.error("Ecriture de {} impossible", this.file, e);
         }
      }
   }

   private void ensureLoaded() {
      if (!this.loaded) {
         synchronized (this) {
            if (!this.loaded) {
               this.load();
               this.loaded = true;
            }
         }
      }
   }

   private void load() {
      if (Files.exists(this.file)) {
         Properties props = new Properties();

         try (InputStream in = Files.newInputStream(this.file)) {
            props.load(in);

            for (String k : props.stringPropertyNames()) {
               this.values.put(k, props.getProperty(k));
            }

            LOG.debug("{} cles chargees depuis {}", this.values.size(), this.file);
         } catch (IOException | IllegalArgumentException e) {
            // Keep the unreadable file aside rather than overwriting it on the next save.
            Path bak = this.file.resolveSibling(this.file.getFileName() + ".bak");
            LOG.error("{} illisible, copie dans {} et repli sur les valeurs par defaut", this.file, bak, e);

            try {
               Files.copy(this.file, bak, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e2) {
               LOG.warn("Sauvegarde de {} impossible", this.file, e2);
            }
         }
      }
   }
}
