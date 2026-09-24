package dev.swiftclient.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.swiftclient.core.log.Log;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * Swift Client configuration, {@code config/swiftclient.json} (schema in {@link ConfigCodec}).
 *
 * <p>Read once, kept in memory; writes reach the disk after {@link #DEBOUNCE_MS} of quiet through a
 * temporary file moved over the original. On first start after an update, the older
 * {@code swiftclient.properties} and {@code swiftclient-profiles.json} are migrated automatically and
 * kept as {@code .bak}. Sections this build does not know (written by a newer one) are preserved.
 */
public final class ConfigStore {
   private static final Logger LOG = Log.get("Config");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final long DEBOUNCE_MS = 500L;
   private static final ScheduledExecutorService WRITER = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "swiftclient-config-writer");
      t.setDaemon(true);
      return t;
   });
   public static final String FILE = "swiftclient.json";
   public static final String LEGACY_PROPERTIES = "swiftclient.properties";
   public static final String LEGACY_PROFILES = "swiftclient-profiles.json";
   private final Path dir;
   private final Path file;
   private final Object lock = new Object();
   private JsonObject root;
   private boolean dirty;
   private ScheduledFuture<?> pending;
   /** What the last load did, for logs and tests. */
   private final List<String> migrationWarnings = new ArrayList<>();
   private boolean migrated;

   public ConfigStore(Path dir) {
      this.dir = dir;
      this.file = dir.resolve(FILE);
      Runtime.getRuntime().addShutdownHook(new Thread(this::flush, "swiftclient-config-shutdown"));
   }

   // --- Flat key API (Game.getConfig / setConfig) ---

   public String get(String key, String def) {
      synchronized (this.lock) {
         String v = ConfigCodec.get(this.root(), key);
         return v != null ? v : def;
      }
   }

   /** {@code null} removes the key. */
   public void set(String key, String value) {
      synchronized (this.lock) {
         JsonObject r = this.root();
         if (value == null) {
            if (ConfigCodec.get(r, key) == null) {
               return;
            }

            ConfigCodec.remove(r, key);
         } else {
            if (value.equals(ConfigCodec.get(r, key))) {
               return;
            }

            try {
               ConfigCodec.put(r, key, value);
            } catch (RuntimeException e) {
               LOG.warn("{} : valeur rejetee '{}' ({})", key, value, e.getMessage());
               return;
            }
         }

         this.scheduleWrite();
      }
   }

   // --- Profiles section ---

   /** Copy of the profiles section ({"active", "list"}), or null if there is none yet. */
   public JsonObject profiles() {
      synchronized (this.lock) {
         JsonElement p = this.root().get(ConfigCodec.PROFILES);
         return p != null && p.isJsonObject() ? p.getAsJsonObject().deepCopy() : null;
      }
   }

   public void setProfiles(JsonObject profiles) {
      synchronized (this.lock) {
         this.root().add(ConfigCodec.PROFILES, profiles.deepCopy());
         this.scheduleWrite();
      }
   }

   public List<String> migrationWarnings() {
      synchronized (this.lock) {
         this.root();
         return List.copyOf(this.migrationWarnings);
      }
   }

   public boolean migrated() {
      synchronized (this.lock) {
         this.root();
         return this.migrated;
      }
   }

   // --- Loading ---

   private JsonObject root() {
      if (this.root == null) {
         this.root = this.load();
      }

      return this.root;
   }

   private JsonObject load() {
      if (Files.isRegularFile(this.file)) {
         try {
            JsonElement e = JsonParser.parseString(Files.readString(this.file, StandardCharsets.UTF_8));
            if (!e.isJsonObject()) {
               throw new IllegalStateException("objet JSON attendu");
            }

            JsonObject r = e.getAsJsonObject();
            int version = r.has("schemaVersion") && r.get("schemaVersion").isJsonPrimitive() ? r.get("schemaVersion").getAsInt() : 0;
            if (version > ConfigCodec.SCHEMA_VERSION) {
               LOG.warn("{} vient d'une version plus recente (schema {}) : sections inconnues conservees telles quelles", FILE, version);
            } else if (version < ConfigCodec.SCHEMA_VERSION) {
               LOG.warn("{} : schemaVersion {} inattendu, lu comme {}", FILE, version, ConfigCodec.SCHEMA_VERSION);
               r.addProperty("schemaVersion", ConfigCodec.SCHEMA_VERSION);
            }

            return r;
         } catch (Exception ex) {
            Path corrupt = this.dir.resolve(FILE + ".corrupt");
            LOG.error("{} illisible, copie dans {} et repli sur les valeurs par defaut", FILE, corrupt.getFileName(), ex);
            copyQuietly(this.file, corrupt);
            return ConfigCodec.newRoot();
         }
      }

      Path props = this.dir.resolve(LEGACY_PROPERTIES);
      Path profiles = this.dir.resolve(LEGACY_PROFILES);
      if (!Files.isRegularFile(props) && !Files.isRegularFile(profiles)) {
         return ConfigCodec.newRoot();
      }

      return this.migrate(props, profiles);
   }

   private JsonObject migrate(Path props, Path profiles) {
      Map<String, String> values = new HashMap<>();
      if (Files.isRegularFile(props)) {
         Properties p = new Properties();

         try (InputStream in = Files.newInputStream(props)) {
            p.load(in);

            for (String k : p.stringPropertyNames()) {
               values.put(k, p.getProperty(k));
            }
         } catch (IOException | IllegalArgumentException e) {
            this.migrationWarnings.add(LEGACY_PROPERTIES + " illisible (" + e.getMessage() + "), reglages par defaut");
         }
      }

      JsonObject legacyProfiles = null;
      if (Files.isRegularFile(profiles)) {
         try {
            JsonElement e = JsonParser.parseString(Files.readString(profiles, StandardCharsets.UTF_8));
            if (e.isJsonObject()) {
               legacyProfiles = e.getAsJsonObject();
            } else {
               this.migrationWarnings.add(LEGACY_PROFILES + " : format illisible, profils ignores");
            }
         } catch (Exception e) {
            this.migrationWarnings.add(LEGACY_PROFILES + " illisible (" + e.getMessage() + "), profils ignores");
         }
      }

      JsonObject r = ConfigMigration.fromLegacy(values, legacyProfiles, this.migrationWarnings);
      this.migrated = true;
      if (this.write(r)) {
         // Only once the new file is safely on disk: keep the old ones as backups.
         moveQuietly(props, this.dir.resolve(LEGACY_PROPERTIES + ".bak"));
         moveQuietly(profiles, this.dir.resolve(LEGACY_PROFILES + ".bak"));
         LOG.info("Configuration migree vers {} ({} cle(s), {} avertissement(s)); anciens fichiers gardes en .bak", FILE, values.size(), this.migrationWarnings.size());
      } else {
         LOG.error("Migration de la configuration non enregistree : anciens fichiers laisses en place");
      }

      for (String w : this.migrationWarnings) {
         LOG.warn("Migration : {}", w);
      }

      return r;
   }

   // --- Writing ---

   private void scheduleWrite() {
      this.dirty = true;
      if (this.pending != null) {
         this.pending.cancel(false);
      }

      this.pending = WRITER.schedule(this::flush, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
   }

   /** Writes pending changes now. Called by the debounce timer and on JVM shutdown. */
   public void flush() {
      synchronized (this.lock) {
         if (this.dirty && this.root != null) {
            this.dirty = false;
            this.pending = null;
            if (!this.write(this.root)) {
               this.dirty = true;
            }
         }
      }
   }

   private boolean write(JsonObject r) {
      Path tmp = this.file.resolveSibling(FILE + ".tmp");

      try {
         Files.createDirectories(this.dir);
         Files.writeString(tmp, GSON.toJson(r), StandardCharsets.UTF_8);

         try {
            Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING);
         }

         return true;
      } catch (IOException e) {
         LOG.error("Ecriture de {} impossible", this.file, e);
         return false;
      }
   }

   private static void moveQuietly(Path from, Path to) {
      try {
         if (Files.isRegularFile(from)) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException e) {
         LOG.warn("Impossible de renommer {} en {}", from.getFileName(), to.getFileName(), e);
      }
   }

   private static void copyQuietly(Path from, Path to) {
      try {
         Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
         LOG.warn("Impossible de copier {}", from.getFileName(), e);
      }
   }
}
