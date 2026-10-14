package dev.horizon.leadseal;

import java.util.List;

/** The new names of renamed or moved tables and of renamed columns. */
public interface Renaming {

  /**
   * Returns the new name of a table.
   *
   * @param table the old table name
   * @return the new name, or the old name if the table was not renamed or moved
   */
  TableName table(TableName table);

  /**
   * Returns the new name of a column.
   *
   * @param table the old table name
   * @param column the old column name
   * @return the new name, or the old name if the column was not renamed
   */
  String column(TableName table, String column);

  /**
   * Returns the new names of columns.
   *
   * @param table the old table name
   * @param columns the old column names
   * @return the new names, in the same order
   */
  default List<String> columns(TableName table, List<String> columns) {
    return columns.stream().map(column -> column(table, column)).toList();
  }
}
