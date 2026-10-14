package dev.horizon.leadseal.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.horizon.leadseal.Operation;
import dev.horizon.leadseal.Schema;
import dev.horizon.leadseal.SchemaException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The migrations of a directory, in the order of their chain from the empty schema.
 *
 * <p>
 * Each migration folder has a {@code migration.json} with the hash of the schema it starts from and the hash of the
 * schema it creates. The snapshots of these schemas are in {@code snapshots/<hash>/schema.json}.
 */
public final class History {

  public static final String SNAPSHOTS = "snapshots";
  public static final String MIGRATION = "migration.json";
  public static final String OPERATIONS = "ops.json";
  public static final String SCHEMA = "schema.json";
  private static final Pattern FOLDER = Pattern.compile("(\\d{8}T\\d{6})_([a-z0-9_]+)");

  private final Path directory;
  private final List<Entry> chain;

  private History(Path directory, List<Entry> chain) {
    this.directory = directory;
    this.chain = chain;
  }

  /**
   * The content of a {@code migration.json} file.
   *
   * @param name the migration name
   * @param from the hash of the schema before the migration, or {@code null} for the first migration
   * @param to the hash of the schema after the migration
   * @param created the creation time, in ISO 8601 format
   */
  public record Info(String name, String from, String to, String created) {
  }

  /**
   * A migration folder.
   *
   * @param folder the folder
   * @param stamp the timestamp part of the folder name, for example {@code 20261014T070000}
   * @param info the content of {@code migration.json}
   */
  public record Entry(Path folder, String stamp, Info info) {

    /**
     * Returns the Flyway version of the migration: the timestamp without the {@code T}.
     *
     * @return the version, for example {@code 20261014070000}
     */
    public String version() {
      return stamp.replace("T", "");
    }

    /**
     * Returns the path of the migration file of a dialect.
     *
     * @param dialect the dialect name
     * @return the path, for example {@code <folder>/postgresql/V20261014070000__create_orders.sql}
     */
    public Path sqlFile(String dialect) {
      return folder.resolve(dialect).resolve("V" + version() + "__" + info.name() + ".sql");
    }

    /**
     * Reads the operations of the migration.
     *
     * @return the operations
     */
    public List<Operation> operations() {
      return Json.read(History.text(folder.resolve(OPERATIONS)), new TypeReference<List<Operation>>() {
      });
    }
  }

  /**
   * Reads the migrations of a directory and checks that they form one chain.
   *
   * @param directory the directory, which may not exist yet
   * @return the history
   * @throws SchemaException if two migrations start from the same schema, or a migration starts from a schema that no
   * migration creates
   */
  public static History read(Path directory) {
    List<Entry> entries = new ArrayList<>();
    if (Files.isDirectory(directory)) {
      try (Stream<Path> children = Files.list(directory)) {
        for (Path folder : children.filter(Files::isDirectory).sorted().toList()) {
          if (folder.getFileName().toString().equals(SNAPSHOTS) || !Files.exists(folder.resolve(MIGRATION))) {
            continue;
          }
          Matcher matcher = FOLDER.matcher(folder.getFileName().toString());
          if (!matcher.matches()) {
            throw new SchemaException("The migration folder '" + folder.getFileName() + "' does not match"
                + " <yyyyMMdd>T<HHmmss>_<name>.");
          }
          entries.add(new Entry(folder, matcher.group(1), Json.read(text(folder.resolve(MIGRATION)),
              new TypeReference<Info>() {
              })));
        }
      } catch (IOException failed) {
        throw new UncheckedIOException(failed);
      }
    }
    return new History(directory, chain(entries));
  }

  /**
   * Returns the migrations, from the first to the latest.
   *
   * @return the migrations
   */
  public List<Entry> chain() {
    return chain;
  }

  /**
   * Returns the hash of the schema after the latest migration.
   *
   * @return the hash, or {@code null} if there are no migrations
   */
  public String latest() {
    return chain.isEmpty() ? null : chain.getLast().info().to();
  }

  /**
   * Returns the timestamp of the latest migration.
   *
   * @return the timestamp, or {@code null} if there are no migrations
   */
  public String latestStamp() {
    return chain.stream().map(Entry::stamp).max(Comparator.naturalOrder()).orElse(null);
  }

  /**
   * Reads a snapshot.
   *
   * @param hash the schema hash, or {@code null} for the empty schema
   * @return the schema
   * @throws SchemaException if the snapshot does not exist
   */
  public Schema snapshot(String hash) {
    if (hash == null) {
      return Schema.empty();
    }
    Path file = directory.resolve(SNAPSHOTS).resolve(hash).resolve(SCHEMA);
    if (!Files.exists(file)) {
      throw new SchemaException("The snapshot " + file + " is missing.");
    }
    return Json.read(text(file), new TypeReference<Schema>() {
    });
  }

  /**
   * Returns the snapshot file of a schema.
   *
   * @param schema the schema
   * @return the file, {@code snapshots/<hash>/schema.json} in the directory
   */
  public Path snapshotFile(Schema schema) {
    return directory.resolve(SNAPSHOTS).resolve(schema.hash()).resolve(SCHEMA);
  }

  private static String text(Path file) {
    try {
      return Files.readString(file);
    } catch (IOException failed) {
      throw new UncheckedIOException(failed);
    }
  }

  private static List<Entry> chain(List<Entry> entries) {
    Map<String, Entry> byParent = new HashMap<>();
    for (Entry entry : entries) {
      Entry other = byParent.put(String.valueOf(entry.info().from()), entry);
      if (other != null) {
        throw new SchemaException("The migrations " + other.folder().getFileName() + " and "
            + entry.folder().getFileName() + " both start from the same schema. Delete one of them and generate it"
            + " again on top of the other.");
      }
    }
    List<Entry> chain = new ArrayList<>();
    String hash = null;
    Entry next;
    while ((next = byParent.get(String.valueOf(hash))) != null) {
      chain.add(next);
      hash = next.info().to();
    }
    if (chain.size() != entries.size()) {
      Entry orphan = entries.stream().filter(entry -> !chain.contains(entry)).findFirst().orElseThrow();
      throw new SchemaException("The migration " + orphan.folder().getFileName() + " starts from the schema "
          + orphan.info().from() + ", which no other migration creates.");
    }
    return chain;
  }
}
