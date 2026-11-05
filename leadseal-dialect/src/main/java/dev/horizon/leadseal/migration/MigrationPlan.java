package dev.horizon.leadseal.migration;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** The files that bring a migration directory up to a schema. */
public sealed interface MigrationPlan {

  /**
   * Gives the files to write.
   *
   * @return the files, which the directory does not have yet
   */
  List<MigrationFile> files();

  /**
   * Gives the folder of the new migration.
   *
   * @return the folder, relative to the directory, or empty if the schema is the schema of the last migration
   */
  Optional<Path> migration();

  /**
   * Describes the data that the new migration deletes, such as the rows of a dropped table.
   *
   * @return the descriptions, for example {@code drops the table orders and its rows}, or empty if the migration keeps
   * every value or if there is no new migration
   */
  List<String> warnings();

  /**
   * The schema is the schema of the last migration. The files are the SQL of dialects that a migration has no file
   * for yet.
   *
   * @param files the files
   */
  record Current(List<MigrationFile> files) implements MigrationPlan {

    public Current {
      files = List.copyOf(files);
    }

    @Override
    public Optional<Path> migration() {
      return Optional.empty();
    }

    @Override
    public List<String> warnings() {
      return List.of();
    }
  }

  /**
   * A new migration, which changes the schema of the last migration into the schema.
   *
   * @param folder the folder of the migration, relative to the directory
   * @param files the files of the migration, and the SQL of dialects that an earlier migration has no file for yet
   * @param warnings the descriptions of the data that the migration deletes
   */
  record Step(Path folder, List<MigrationFile> files, List<String> warnings) implements MigrationPlan {

    public Step {
      files = List.copyOf(files);
      warnings = List.copyOf(warnings);
    }

    @Override
    public Optional<Path> migration() {
      return Optional.of(folder);
    }
  }
}
