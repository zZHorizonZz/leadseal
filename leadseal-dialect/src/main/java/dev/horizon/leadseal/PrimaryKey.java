package dev.horizon.leadseal;

import java.util.List;

/**
 * The primary key of a table.
 *
 * @param name the constraint name
 * @param columns the key columns, in order
 */
public record PrimaryKey(String name, List<String> columns) {

  public PrimaryKey {
    Names.require(name, "primary key name");
    columns = List.copyOf(columns);
    if (columns.isEmpty()) {
      throw new SchemaException("The primary key '" + name + "' has no columns.");
    }
  }

  /**
   * Returns a primary key with a generated name: {@code pk_} followed by the table name.
   *
   * @param table the table name
   * @param columns the key columns
   * @return the primary key
   */
  public static PrimaryKey of(String table, List<String> columns) {
    return new PrimaryKey(Names.generate("pk", table, List.of()), columns);
  }
}
