package dev.horizon.leadseal;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A namespace of tables: a database schema, such as {@code public} or {@code billing}. On MariaDB, a namespace is a
 * database. The default namespace is the default schema of the database connection.
 *
 * @param name the namespace name, or {@code null} for the default namespace
 * @param tables the tables, with names that are unique in this namespace
 */
public record Namespace(String name, List<Table> tables) {

  public Namespace {
    if (name != null && name.isBlank()) {
      throw new SchemaException("A namespace name is empty.");
    }
    tables = List.copyOf(tables);
    Set<String> names = new HashSet<>();
    for (Table table : tables) {
      if (!names.add(table.name())) {
        throw new SchemaException("The namespace " + (name == null ? "default" : "'" + name + "'")
            + " has the table '" + table.name() + "' twice.");
      }
    }
  }

  /**
   * Returns the default namespace with some tables.
   *
   * @param tables the tables
   * @return the namespace
   */
  public static Namespace defaults(Table... tables) {
    return new Namespace(null, List.of(tables));
  }

  /**
   * Returns the table with a name.
   *
   * @param name the table name
   * @return the table, or an empty optional if this namespace has no table with that name
   */
  public Optional<Table> table(String name) {
    return tables.stream().filter(table -> table.name().equals(name)).findFirst();
  }

  /**
   * Returns the name of a table of this namespace.
   *
   * @param table the table
   * @return the table name in this namespace
   */
  public TableName nameOf(Table table) {
    return new TableName(name, table.name());
  }
}
