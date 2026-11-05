package dev.horizon.leadseal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A namespace of tables: a database schema, such as {@code public} or {@code billing}. On MariaDB, a namespace is a
 * database. The default namespace is the default schema of the database connection.
 *
 * @param name the namespace name
 * @param tables the tables, with names that are unique in this namespace
 */
public record Namespace(Name name, List<Table> tables) {

  public Namespace {
    name = name == null ? Name.DEFAULT : name;
    tables = List.copyOf(tables);
    Set<String> names = new HashSet<>();
    for (Table table : tables) {
      if (!names.add(table.name())) {
        throw new InvalidSchemaException("The namespace " + name.map(text -> "'" + text + "'", () -> "default")
            + " has the table '" + table.name() + "' twice.");
      }
    }
  }

  /**
   * Gives a namespace with a name.
   *
   * @param name the namespace name, such as {@code billing}
   * @param tables the tables
   */
  public Namespace(String name, List<Table> tables) {
    this(Name.of(name), tables);
  }

  /**
   * Gives the default namespace with some tables.
   *
   * @param tables the tables
   * @return the namespace
   */
  public static Namespace defaults(Table... tables) {
    return new Namespace(Name.DEFAULT, List.of(tables));
  }

  /**
   * Gives the table with a name.
   *
   * @param name the table name
   * @return the table, or an empty optional if this namespace has no table with that name
   */
  public Optional<Table> table(String name) {
    return tables.stream().filter(table -> table.name().equals(name)).findFirst();
  }

  /**
   * Gives the name of a table of this namespace.
   *
   * @param table the table
   * @return the table name in this namespace
   */
  public TableName nameOf(Table table) {
    return new TableName(name, table.name());
  }

  /** The name of a namespace: a database schema, or the default schema of the database connection. */
  public sealed interface Name {

    /** The default schema of the database connection. */
    Name DEFAULT = new Default();

    /**
     * Gives a namespace name from its form in JSON.
     *
     * @param text the name, or {@code null} for the default namespace
     * @return the name
     * @throws SchemaException if the name is blank
     */
    @JsonCreator
    static Name of(String text) {
      return text == null ? DEFAULT : new Identifier(text);
    }

    /**
     * Gives a value for this name, or for the default namespace.
     *
     * @param identifier the value for the name of a database schema
     * @param fallback the value for the default namespace
     * @param <T> the type of the value
     * @return the value
     */
    <T> T map(Function<String, T> identifier, Supplier<T> fallback);

    /**
     * Gives the form of this name in JSON.
     *
     * @return the name, or {@code null} for the default namespace, which JSON leaves out
     */
    @JsonValue
    default String json() {
      return map(text -> text, () -> null);
    }

    /** The default schema of the database connection. */
    record Default() implements Name {

      @Override
      public <T> T map(Function<String, T> identifier, Supplier<T> fallback) {
        return fallback.get();
      }

      @Override
      public String toString() {
        return "default";
      }
    }

    /**
     * The name of a database schema.
     *
     * @param text the name
     */
    record Identifier(String text) implements Name {

      public Identifier {
        if (text == null || text.isBlank()) {
          throw new InvalidSchemaException("A namespace name is empty.");
        }
      }

      @Override
      public <T> T map(Function<String, T> identifier, Supplier<T> fallback) {
        return identifier.apply(text);
      }

      @Override
      public String toString() {
        return text;
      }
    }
  }
}
