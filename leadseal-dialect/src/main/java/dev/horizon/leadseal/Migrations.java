package dev.horizon.leadseal;

import dev.horizon.leadseal.impl.History;
import dev.horizon.leadseal.impl.Json;
import dev.horizon.leadseal.impl.MigrationName;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generates migration files in a directory.
 *
 * <p>
 * Each migration is a folder named {@code <yyyyMMdd>T<HHmmss>_<name>}, with these files:
 *
 * <ul>
 * <li>{@code migration.json}: the migration name, the hashes of the schemas before and after it, and its creation
 * time.</li>
 * <li>{@code ops.json}: the operations of the migration.</li>
 * <li>{@code <dialect>/V<yyyyMMddHHmmss>__<name>.sql}: the SQL of each dialect, in Flyway's naming format. A script
 * that must run outside a transaction also has a {@code .sql.conf} file with {@code executeInTransaction=false}.</li>
 * </ul>
 *
 * <p>
 * The directory also has {@code snapshots/<hash>/schema.json}, the schema after each migration. Existing migration
 * files are never changed.
 */
public final class Migrations {

  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

  private final Path directory;
  private final List<Dialect> dialects;
  private final Clock clock;

  public Migrations(Path directory, List<Dialect> dialects) {
    this(directory, dialects, Clock.systemUTC());
  }

  public Migrations(Path directory, List<Dialect> dialects, Clock clock) {
    this.directory = directory;
    this.dialects = List.copyOf(dialects);
    this.clock = clock;
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
   * The files also include the SQL of each dialect that an existing migration does not have yet, from the migration's
   * {@code ops.json}. If the schema has the hash of the latest snapshot, or the diff has no operations, there is no
   * new migration.
   *
   * @param schema the schema
   * @return the new files
   * @throws SchemaException if the history has two branches, a dialect cannot render an operation, or a file cannot be
   * read
   */
  public Result plan(Schema schema) {
    History history = History.read(directory);
    List<MigrationFile> files = new ArrayList<>(backfill(history));
    if (schema.hash().equals(history.latest())) {
      return new Result(Optional.empty(), files);
    }
    Schema previous = history.snapshot(history.latest());
    List<Operation> operations = new Diff(previous, schema).operations();
    if (operations.isEmpty()) {
      return new Result(Optional.empty(), files);
    }
    String stamp = stamp(history.latestStamp());
    String name = MigrationName.of(operations);
    Path folder = directory.resolve(stamp + "_" + name);
    History.Info info = new History.Info(name, history.latest(), schema.hash(), clock.instant().toString());
    History.Entry entry = new History.Entry(folder, stamp, info);
    add(files, history.snapshotFile(schema), Json.write(schema, true));
    add(files, folder.resolve(History.OPERATIONS), Json.write(operations));
    add(files, folder.resolve(History.MIGRATION), Json.write(info, true));
    for (Dialect dialect : dialects) {
      add(files, entry.sqlFile(dialect.name()), DdlWriter.script(dialect, operations, previous, schema));
    }
    return new Result(Optional.of(directory.relativize(folder)), files);
  }

  /**
   * Writes the files of a migration from the latest snapshot to a schema. See {@link #plan}.
   *
   * @param schema the schema
   * @return the written files
   * @throws SchemaException if the history has two branches, a dialect cannot render an operation, or a file cannot be
   * read
   */
  public Result generate(Schema schema) {
    Result result = plan(schema);
    for (MigrationFile file : result.files()) {
      file.write(directory);
    }
    return result;
  }

  private List<MigrationFile> backfill(History history) {
    List<MigrationFile> files = new ArrayList<>();
    String hash = null;
    for (History.Entry entry : history.chain()) {
      for (Dialect dialect : dialects) {
        Path file = entry.sqlFile(dialect.name());
        if (!Files.exists(file)) {
          add(files, file, DdlWriter.script(dialect, entry.operations(), history.snapshot(hash),
              history.snapshot(entry.info().to())));
        }
      }
      hash = entry.info().to();
    }
    return files;
  }

  /**
   * Adds the SQL file of a script. A script that must run outside a transaction also gets a Flyway script
   * configuration file, {@code <file>.conf}, that turns off Flyway's transaction.
   */
  private void add(List<MigrationFile> files, Path file, SqlScript script) {
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

  /** Returns the timestamp of a new migration: now, or one second after the latest migration if that is later. */
  private String stamp(String latest) {
    LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).withNano(0);
    if (latest != null) {
      LocalDateTime last = LocalDateTime.parse(latest, STAMP);
      if (!now.isAfter(last)) {
        now = last.plusSeconds(1);
      }
    }
    return now.format(STAMP);
  }
}
