package dev.horizon.leadseal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.horizon.leadseal.change.Change;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The migrations of a directory.
 *
 * <p>
 * Each migration is a folder named {@code <yyyyMMddHHmmss>_<name>}, with these files:
 *
 * <ul>
 * <li>{@code migration.json}: the migration name, the hashes of the schemas before and after it, its creation time and
 * its changes.</li>
 * <li>{@code <dialect>/V<yyyyMMddHHmmss>__<name>.sql}: the SQL of each dialect, in Flyway's naming format. A script
 * that must run outside a transaction also has a {@code .sql.conf} file with {@code executeInTransaction=false}.</li>
 * </ul>
 *
 * <p>
 * The directory also has {@code snapshots/<hash>/contract.json}, the schema after each migration. The migrations form
 * one chain: each one starts from the schema that the one before it creates. Existing migration files are never
 * changed.
 */
public final class Migrations {

  private static final String MIGRATION = "migration.json";
  private static final String SNAPSHOTS = "snapshots";
  private static final String SNAPSHOT = "contract.json";
  private static final String HASH_ALGORITHM = "SHA-256";
  private static final int HASH_LENGTH = 16;
  private static final DateTimeFormatter VERSION = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final Path directory;
  private final List<Dialect> dialects;
  private final MigrationOptions options;
  private final ObjectMapper json = new ObjectMapper()
      .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  public Migrations(Path directory, List<Dialect> dialects) {
    this(directory, dialects, MigrationOptions.defaults());
  }

  public Migrations(Path directory, List<Dialect> dialects, MigrationOptions options) {
    this.directory = directory;
    this.dialects = List.copyOf(dialects);
    this.options = options;
    if (this.dialects.isEmpty()) {
      throw new SchemaException("Migrations need at least one dialect.");
    }
  }

  /**
   * The new files of a migration directory.
   *
   * @param migration the new migration folder, relative to the directory, or an empty optional if the schema did not
   * change
   * @param files the new files, including the SQL of dialects that existing migrations did not have yet
   */
  public record Result(Optional<Path> migration, List<MigrationFile> files) {

    public Result {
      files = List.copyOf(files);
    }
  }

  /**
   * Returns the files of a migration from the latest snapshot to a schema, without writing them.
   *
   * <p>
   * The files also include the SQL of each dialect that an existing migration does not have yet. If the schema has
   * the hash of the latest snapshot, or it has no changes, there is no new migration.
   *
   * @param schema the schema
   * @return the new files
   * @throws SchemaException if the migrations do not form one chain, a dialect cannot write a change, or a file cannot
   * be read
   */
  public Result plan(Schema schema) {
    List<Migration> migrations = migrations();
    List<MigrationFile> files = new ArrayList<>();
    for (Migration migration : migrations) {
      for (Dialect dialect : dialects) {
        if (!Files.exists(directory.resolve(migration.script(dialect.name())))) {
          script(files, migration, dialect, snapshot(migration.from()), snapshot(migration.to()));
        }
      }
    }
    String latest = migrations.isEmpty() ? null : migrations.getLast().to();
    String hash = hash(schema);
    if (hash.equals(latest)) {
      return new Result(Optional.empty(), files);
    }
    Schema previous = snapshot(latest);
    List<Change> changes = previous.changesTo(schema);
    if (changes.isEmpty()) {
      return new Result(Optional.empty(), files);
    }
    Migration migration = Migration.of(version(migrations), latest, hash, options.clock().instant().toString(),
        changes);
    add(files, directory.resolve(SNAPSHOTS).resolve(hash).resolve(SNAPSHOT), write(new Snapshot(schema, hash)));
    add(files, directory.resolve(migration.folder()).resolve(MIGRATION), write(migration));
    for (Dialect dialect : dialects) {
      script(files, migration, dialect, previous, schema);
    }
    return new Result(Optional.of(migration.folder()), files);
  }

  /**
   * Writes the files of a migration from the latest snapshot to a schema. See {@link #plan}.
   *
   * @param schema the schema
   * @return the written files
   * @throws SchemaException if the migrations do not form one chain, a dialect cannot write a change, or a file cannot
   * be read
   */
  public Result generate(Schema schema) {
    Result result = plan(schema);
    for (MigrationFile file : result.files()) {
      file.write(directory);
    }
    return result;
  }

  /** Reads the migrations in the order of their versions, and checks that each starts where the one before ends. */
  private List<Migration> migrations() {
    if (!Files.isDirectory(directory)) {
      return List.of();
    }
    List<Migration> migrations;
    try (Stream<Path> folders = Files.list(directory)) {
      migrations = folders.filter(folder -> Files.exists(folder.resolve(MIGRATION))).map(this::migration)
          .sorted(Comparator.comparing(Migration::version)).toList();
    } catch (IOException failed) {
      throw new UncheckedIOException(failed);
    }
    Migration previous = null;
    for (Migration migration : migrations) {
      String before = previous == null ? null : previous.to();
      if (!Objects.equals(migration.from(), before)) {
        throw new SchemaException("The migration " + migration.folder() + " starts from the schema "
            + migration.from() + (previous == null ? ", which no migration before it creates."
                : ", but the migration before it, " + previous.folder() + ", creates the schema " + before
                    + ". Delete the later migration and generate it again."));
      }
      previous = migration;
    }
    return migrations;
  }

  private Migration migration(Path folder) {
    Migration migration = read(folder.resolve(MIGRATION), Migration.class);
    if (!folder.getFileName().equals(migration.folder())) {
      throw new SchemaException("The migration folder " + folder.getFileName() + " has the migration "
          + migration.folder() + ". Rename the folder to " + migration.folder() + ".");
    }
    return migration;
  }

  /** Reads the snapshot with a hash, or returns the empty schema for no hash. */
  private Schema snapshot(String hash) {
    if (hash == null) {
      return Schema.empty();
    }
    Path file = directory.resolve(SNAPSHOTS).resolve(hash).resolve(SNAPSHOT);
    if (!Files.exists(file)) {
      throw new SchemaException("The snapshot " + file + " is missing.");
    }
    Snapshot snapshot = read(file, Snapshot.class);
    if (!Snapshot.VERSION.equals(snapshot.schemaVersion())) {
      throw new SchemaException("The snapshot " + file + " has the format version " + snapshot.schemaVersion()
          + ", but this version of leadseal reads version " + Snapshot.VERSION + ".");
    }
    return snapshot.storage();
  }

  /** Returns the hash of a schema: the first 16 hexadecimal digits of the SHA-256 hash of its JSON. */
  private String hash(Schema schema) {
    try {
      byte[] digest = MessageDigest.getInstance(HASH_ALGORITHM).digest(json.writeValueAsString(schema)
          .getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest).substring(0, HASH_LENGTH);
    } catch (JsonProcessingException failed) {
      throw new SchemaException("Cannot write the schema as JSON.", failed);
    } catch (NoSuchAlgorithmException missing) {
      throw new IllegalStateException(missing);
    }
  }

  /** Returns the version of a new migration: now, or one second after the latest migration if that is later. */
  private String version(List<Migration> migrations) {
    LocalDateTime now = LocalDateTime.ofInstant(options.clock().instant(), ZoneOffset.UTC).withNano(0);
    if (!migrations.isEmpty()) {
      LocalDateTime last = LocalDateTime.parse(migrations.getLast().version(), VERSION);
      if (!now.isAfter(last)) {
        now = last.plusSeconds(1);
      }
    }
    return now.format(VERSION);
  }

  /**
   * Adds the SQL file of a migration in a dialect. A script that must run outside a transaction also gets a Flyway
   * script configuration file, {@code <file>.conf}, that turns off Flyway's transaction.
   */
  private void script(List<MigrationFile> files, Migration migration, Dialect dialect, Schema before, Schema after) {
    Path file = directory.resolve(migration.script(dialect.name()));
    ScriptWriter script = new ScriptWriter(dialect, before, after, options.prefix()).write(migration.changes());
    add(files, file, script.text());
    if (!script.transactional()) {
      add(files, file.resolveSibling(file.getFileName() + ".conf"), "executeInTransaction=false\n");
    }
  }

  /** Adds a file that does not exist yet. */
  private void add(List<MigrationFile> files, Path file, String content) {
    if (!Files.exists(file)) {
      files.add(new MigrationFile(directory.relativize(file), content));
    }
  }

  private String write(Object value) {
    try {
      return json.writerWithDefaultPrettyPrinter().writeValueAsString(value) + "\n";
    } catch (JsonProcessingException failed) {
      throw new SchemaException("Cannot write " + value.getClass().getSimpleName() + " as JSON.", failed);
    }
  }

  private <T> T read(Path file, Class<T> type) {
    try {
      return json.readValue(file.toFile(), type);
    } catch (IOException failed) {
      throw new SchemaException("Cannot read " + file + ": " + failed.getMessage(), failed);
    }
  }
}
