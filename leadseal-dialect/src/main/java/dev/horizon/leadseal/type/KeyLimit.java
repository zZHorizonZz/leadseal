package dev.horizon.leadseal.type;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.UnsupportedChangeException;

/**
 * A database type that a database cannot use in a key or an index, such as a text without a length.
 *
 * @param database the name of the database in a message, for example {@code MariaDB}
 * @param type the database type, for example {@code LONGTEXT}
 * @param advice what the schema can do instead, for example {@code Give the column a length.}
 */
public record KeyLimit(String database, String type, String advice) {

  /**
   * Writes the database type of a column.
   *
   * @param table the table of the column
   * @param column the column
   * @return the database type
   * @throws SchemaException if a key or an index of the table covers the column
   */
  public String sql(Table table, Column column) {
    if (table.covers(column.name())) {
      throw new UnsupportedChangeException(database + " cannot use the column " + table.name() + "." + column.name() + " in a"
          + " key or an index, because its type is " + type + ". " + advice);
    }
    return type;
  }
}
