package dev.horizon.leadseal;

import java.util.List;

/**
 * An index of a table.
 *
 * @param name the index name
 * @param columns the indexed columns, in order
 * @param unique {@code true} for a unique index
 */
public record Index(String name, List<String> columns, boolean unique) {

  public Index {
    Names.require(name, "index name");
    columns = List.copyOf(columns);
    if (columns.isEmpty()) {
      throw new SchemaException("The index '" + name + "' has no columns.");
    }
  }

  /**
   * Returns an index with a generated name: {@code ix_} for an index or {@code ux_} for a unique index, followed by
   * the table name and the column names. Names longer than 63 characters are shortened with a hash.
   *
   * @param table the table name
   * @param columns the indexed columns
   * @param unique {@code true} for a unique index
   * @return the index
   */
  public static Index of(String table, List<String> columns, boolean unique) {
    return new Index(Names.generate(unique ? "ux" : "ix", table, columns), columns, unique);
  }
}
