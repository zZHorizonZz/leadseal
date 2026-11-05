package dev.horizon.leadseal.migration;

import dev.horizon.leadseal.ConfigurationException;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.change.Change;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A directory of migrations.
 *
 * <p>
 * Each migration is a folder named {@code <yyyyMMddHHmmss>}, with these files:
 *
 * <ul>
 * <li>{@code migration.json}: the migration name, the hashes of the schemas before and after it, its creation time and
 * its changes.</li>
 * <li>{@code <dialect>/V<yyyyMMddHHmmss>.sql}: the SQL of each dialect, in Flyway's naming format. A script
 * that must run outside a transaction also has a {@code .sql.conf} file with {@code executeInTransaction=false}.</li>
 * </ul>
 *
 * <p>
 * The directory also has {@code snapshots/<hash>/contract.json}, the schema after each migration. The migrations form
 * one chain: each one starts from the schema that the one before it creates. A directory never changes a file that it
 * has already.
 */
public final class MigrationDirectory {

  private final Path directory;
  private final List<Dialect> dialects;
  private final MigrationOptions options;
  private final MigrationJson json = new MigrationJson();

  /**
   * Opens a directory with the default options.
   *
   * @param directory the directory
   * @param dialects the dialects to write SQL for
   * @throws SchemaException if there is no dialect
   */
  public MigrationDirectory(Path directory, List<Dialect> dialects) {
    this(directory, dialects, MigrationOptions.defaults());
  }

  /**
   * Opens a directory.
   *
   * @param directory the directory
   * @param dialects the dialects to write SQL for
   * @param options the options
   * @throws SchemaException if there is no dialect
   */
  public MigrationDirectory(Path directory, List<Dialect> dialects, MigrationOptions options) {
    this.directory = directory;
    this.dialects = List.copyOf(dialects);
    this.options = options;
    if (this.dialects.isEmpty()) {
      throw new ConfigurationException("Migrations need at least one dialect.");
    }
  }

  /**
   * Gives the files that bring the directory up to a schema, without writing them: the SQL of a dialect that a
   * migration has no file for yet, and a new migration if the schema differs from the schema of the last migration.
   *
   * @param schema the schema
   * @return the plan
   * @throws SchemaException if the migrations of the directory do not form one chain
   */
  public MigrationPlan plan(Schema schema) {
    MigrationChain chain = MigrationChain.read(directory, json);
    SnapshotFolder snapshots = new SnapshotFolder(directory, json);
    List<MigrationFile> files = new ArrayList<>();
    for (Migration migration : chain.migrations()) {
      dialects.stream().filter(dialect -> !Files.exists(directory.resolve(migration.script(dialect))))
          .forEach(dialect -> files.addAll(migration.scripts(dialect, snapshots.schema(migration.origin()),
              snapshots.schema(migration.target()), options)));
    }
    SchemaHash hash = json.hash(schema);
    if (hash.equals(chain.latest())) {
      return new MigrationPlan.Current(absent(files));
    }
    Schema latest = snapshots.schema(chain.latest());
    List<Change> changes = latest.changesTo(schema);
    if (changes.isEmpty()) {
      return new MigrationPlan.Current(absent(files));
    }
    Migration migration = Migration.of(chain.next(options.clock().instant()), chain.latest(), hash,
        options.clock().instant().toString(), changes);
    files.add(snapshots.file(schema, hash));
    files.add(migration.file(json));
    dialects.forEach(dialect -> files.addAll(migration.scripts(dialect, latest, schema, options)));
    return new MigrationPlan.Step(migration.folder(), absent(files));
  }

  /**
   * Writes the files of the plan of a schema.
   *
   * @param schema the schema
   * @return the plan, whose files are written
   * @throws SchemaException if the migrations of the directory do not form one chain
   */
  public MigrationPlan generate(Schema schema) {
    MigrationPlan plan = plan(schema);
    plan.files().forEach(file -> file.write(directory));
    return plan;
  }

  private List<MigrationFile> absent(List<MigrationFile> files) {
    return files.stream().filter(file -> !Files.exists(directory.resolve(file.path()))).toList();
  }
}
