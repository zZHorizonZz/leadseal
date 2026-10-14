package dev.horizon.leadseal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A file of a migration directory.
 *
 * @param path the path, relative to the migration directory, for example
 * {@code 20261014070000/postgresql/V20261014070000.sql}
 * @param content the file content
 */
public record MigrationFile(Path path, String content) {

  /**
   * Writes the file into a migration directory, with its parent folders.
   *
   * @param directory the migration directory
   * @return the written file
   */
  public Path write(Path directory) {
    Path file = directory.resolve(path);
    try {
      Files.createDirectories(file.getParent());
      return Files.writeString(file, content);
    } catch (IOException failed) {
      throw new UncheckedIOException(failed);
    }
  }
}
