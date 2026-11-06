package dev.horizon.leadseal.migration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

record MigrationChain(List<Migration> migrations) {

  MigrationChain {
    migrations = List.copyOf(migrations);
    if (!migrations.isEmpty()) {
      migrations.getFirst().start();
    }
    for (int i = 1; i < migrations.size(); i++) {
      migrations.get(i).follow(migrations.get(i - 1));
    }
  }

  static MigrationChain read(Path directory, MigrationJson json) {
    if (!Files.isDirectory(directory)) {
      return new MigrationChain(List.of());
    }
    try (Stream<Path> folders = Files.list(directory)) {
      return new MigrationChain(folders.filter(folder -> Files.exists(folder.resolve(Migration.FILE)))
          .map(folder -> Migration.read(folder, json)).sorted(Comparator.comparing(Migration::version)).toList());
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  SchemaHash latest() {
    return migrations.isEmpty() ? SchemaHash.INITIAL : migrations.getLast().target();
  }

  Version next(Instant now) {
    Version version = Version.of(now);
    return migrations.isEmpty() ? version : migrations.getLast().version().next(version);
  }
}
