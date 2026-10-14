package dev.horizon.leadseal;

import java.util.List;

/**
 * An index of a table.
 *
 * @param name the index name
 * @param columns the indexed columns, in order
 * @param unique {@code true} for a unique index
 * @param descending the indexed columns that the index sorts in descending order
 * @param where the SQL condition of a partial index, which indexes only the rows that meet it, or {@code null} to
 * index every row
 */
public record Index(String name, List<String> columns, boolean unique, List<String> descending, String where) {

  public Index {
    Names.require(name, "index name");
    columns = List.copyOf(columns);
    descending = descending == null ? List.of() : List.copyOf(descending);
    where = where == null || where.isBlank() ? null : where;
    if (columns.isEmpty()) {
      throw new SchemaException("The index '" + name + "' has no columns.");
    }
    for (String column : descending) {
      if (!columns.contains(column)) {
        throw new SchemaException("The index '" + name + "' sorts the column '" + column + "' in descending order,"
            + " but does not index it.");
      }
    }
  }

  public Index(String name, List<String> columns, boolean unique) {
    this(name, columns, unique, List.of(), null);
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

  /**
   * Returns a copy of this index with other columns, in the same order and direction.
   *
   * @param columns the new names of the columns, in the order of {@link #columns()}
   * @return the new index
   */
  public Index withColumns(List<String> columns) {
    List<String> renamed = descending.stream().map(column -> columns.get(this.columns.indexOf(column))).toList();
    return new Index(name, columns, unique, renamed, where);
  }

  /**
   * Returns a copy of this index that sorts some columns in descending order.
   *
   * @param descending the columns to sort in descending order
   * @return the new index
   */
  public Index withDescending(List<String> descending) {
    return new Index(name, columns, unique, descending, where);
  }

  /**
   * Returns a copy of this index that indexes only the rows that meet a condition.
   *
   * @param where the SQL condition, or {@code null} to index every row
   * @return the new index
   */
  public Index withWhere(String where) {
    return new Index(name, columns, unique, descending, where);
  }
}
