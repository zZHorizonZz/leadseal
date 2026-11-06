package dev.horizon.leadseal.migration;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.horizon.leadseal.BrokenHistoryException;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.Script;
import dev.horizon.leadseal.ScriptWriter;
import dev.horizon.leadseal.change.Change;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

record Migration(Version version, String name, @JsonProperty("from") SchemaHash origin,
    @JsonProperty("to") SchemaHash target, String created, List<Change> changes) {

  static final String FILE = "migration.json";

  Migration {
    origin = origin == null ? SchemaHash.INITIAL : origin;
    changes = List.copyOf(changes);
  }

  static Migration of(Version version, SchemaHash origin, SchemaHash target, String created, List<Change> changes) {
    return new Migration(version, new MigrationName(changes).text(), origin, target, created, changes);
  }

  static Migration read(Path folder, MigrationJson json) {
    Migration migration = json.read(folder.resolve(FILE), Migration.class);
    if (!folder.getFileName().equals(migration.folder())) {
      throw new BrokenHistoryException("The migration folder " + folder.getFileName() + " has the migration "
          + migration.folder() + ". Rename the folder to " + migration.folder() + ".");
    }
    return migration;
  }

  Path folder() {
    return Path.of(version.text());
  }

  void start() {
    if (!origin.equals(SchemaHash.INITIAL)) {
      throw new BrokenHistoryException("The migration " + folder() + " starts from the schema " + origin + ", which no"
          + " migration before it creates.");
    }
  }

  void follow(Migration previous) {
    if (!origin.equals(previous.target)) {
      throw new BrokenHistoryException("The migration " + folder() + " starts from the schema " + origin + ", but the"
          + " migration before it, " + previous.folder() + ", creates the schema " + previous.target + ". Delete the"
          + " later migration and generate it again.");
    }
  }

  MigrationFile file(MigrationJson json) {
    return new MigrationFile(folder().resolve(FILE), json.write(this));
  }

  Path script(Dialect dialect) {
    return folder().resolve(dialect.name()).resolve("V" + version.text() + ".sql");
  }

  List<MigrationFile> scripts(Dialect dialect, Schema origin, Schema target, MigrationOptions options) {
    Path script = script(dialect);
    Script sql = new ScriptWriter(dialect, origin, target, options.prefix()).write(changes).script();
    List<MigrationFile> all = new ArrayList<>(List.of(new MigrationFile(script, sql.text(dialect))));
    if (!sql.transactional()) {
      all.add(new MigrationFile(script.resolveSibling(script.getFileName() + ".conf"), "executeInTransaction=false\n"));
    }
    return all;
  }
}
