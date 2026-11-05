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
  }

  /**
   * A new migration, which changes the schema of the last migration into the schema.
   *
   * @param folder the folder of the migration, relative to the directory
   * @param files the files of the migration, and the SQL of dialects that an earlier migration has no file for yet
   */
  record Step(Path folder, List<MigrationFile> files) implements MigrationPlan {

    public Step {
      files = List.copyOf(files);
    }

    @Override
    public Optional<Path> migration() {
      return Optional.of(folder);
    }
  }
}
