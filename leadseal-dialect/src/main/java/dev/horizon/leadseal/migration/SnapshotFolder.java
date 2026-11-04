package dev.horizon.leadseal.migration;

import dev.horizon.leadseal.BrokenHistoryException;
import dev.horizon.leadseal.Schema;
import java.nio.file.Files;
import java.nio.file.Path;

record SnapshotFolder(Path directory, MigrationJson json) {

  private static final String FOLDER = "snapshots";
  private static final String FILE = "contract.json";

  Schema schema(SchemaHash hash) {
    return hash.map(this::read, Schema::empty);
  }

  MigrationFile file(Schema schema, SchemaHash hash) {
    return new MigrationFile(path(hash.json()), json.write(new Snapshot(schema, hash)));
  }

  private Schema read(String hash) {
    Path file = directory.resolve(path(hash));
    if (!Files.exists(file)) {
      throw new BrokenHistoryException("The snapshot " + file + " is missing.");
    }
    return json.read(file, Snapshot.class).schema(file);
  }

  private Path path(String hash) {
    return Path.of(FOLDER, hash, FILE);
  }
}
