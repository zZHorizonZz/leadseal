package dev.horizon.leadseal;

import dev.horizon.leadseal.change.Change;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A migration, as its {@code migration.json} file stores it. The migration is the folder {@code <version>} of a
 * migration directory, with the SQL of each dialect in {@code <dialect>/V<version>.sql}.
 *
 * @param version the Flyway version of the migration: its time in UTC, for example {@code 20261014070000}
 * @param name the name of the migration, for example {@code create_orders}
 * @param from the hash of the schema before the migration, or {@code null} for the first migration
 * @param to the hash of the schema after the migration
 * @param created the time that leadseal generated the migration, in ISO 8601 format
 * @param changes the changes of the migration, in the order they run
 */
record Migration(String version, String name, String from, String to, String created, List<Change> changes) {

  private static final int MAX_NAME_LENGTH = 60;
  private static final String UNNAMED = "change";

  Migration {
    changes = List.copyOf(changes);
  }

  /**
   * Returns a migration named after its changes, for example {@code add_orders_note}. Minor changes, such as new
   * indexes, name a migration only when it has no other changes.
   *
   * @param version the Flyway version of the migration
   * @param from the hash of the schema before the migration, or {@code null} for the first migration
   * @param to the hash of the schema after the migration
   * @param created the time that leadseal generated the migration
   * @param changes the changes of the migration
   * @return the migration
   */
  static Migration of(String version, String from, String to, String created, List<Change> changes) {
    Set<String> parts = new LinkedHashSet<>();
    changes.stream().filter(change -> !change.minor()).forEach(change -> parts.add(change.summary()));
    if (parts.isEmpty()) {
      changes.forEach(change -> parts.add(change.summary()));
    }
    List<String> summaries = new ArrayList<>(parts);
    String name = switch (summaries.size()) {
      case 0 -> UNNAMED;
      case 1 -> summaries.getFirst();
      case 2 -> summaries.get(0) + "_and_" + summaries.get(1);
      default -> summaries.getFirst() + "_and_" + (summaries.size() - 1) + "_more";
    };
    String clean = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    String shortened = clean.length() <= MAX_NAME_LENGTH ? clean
        : clean.substring(0, MAX_NAME_LENGTH).replaceAll("_$", "");
    return new Migration(version, shortened, from, to, created, changes);
  }

  /**
   * Returns the folder of the migration.
   *
   * @return the folder, relative to the migration directory, for example {@code 20261014070000}
   */
  Path folder() {
    return Path.of(version);
  }

  /**
   * Returns the SQL file of the migration for a dialect.
   *
   * @param dialect the dialect name
   * @return the file, relative to the migration directory, for example
   * {@code 20261014070000/postgresql/V20261014070000.sql}
   */
  Path script(String dialect) {
    return folder().resolve(dialect).resolve("V" + version + ".sql");
  }
}
