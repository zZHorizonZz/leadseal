package dev.horizon.leadseal.sqlite;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.contract.TestDatabase;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class SqliteTestDatabase implements TestDatabase {

  @Override
  public Dialect dialect() {
    return new SqliteDialect();
  }

  @Override
  public String binaryCollation() {
    return "BINARY";
  }

  @Override
  public boolean supportsSchemas() {
    return false;
  }

  @Override
  public boolean supportsComments() {
    return false;
  }

  @Override
  public Database create() {
    try {
      Path file = Files.createTempFile("leadseal", ".db");
      file.toFile().deleteOnExit();
      return new Database("jdbc:sqlite:" + file + "?foreign_keys=true", null, null);
    } catch (IOException failed) {
      throw new UncheckedIOException(failed);
    }
  }
}
