package dev.horizon.leadseal.type;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.UnsupportedChangeException;

/**
 * The largest decimal that a database stores.
 *
 * @param database the name of the database in a message, for example {@code MariaDB}
 * @param precision the largest number of digits
 * @param scale the largest number of digits after the decimal point
 */
public record DecimalLimit(String database, int precision, int scale) {

  /**
   * Writes the database type of a decimal column.
   *
   * @param keyword the name of the database type, for example {@code DECIMAL}
   * @param table the table of the column
   * @param column the column
   * @param type the type of the column
   * @return the database type, for example {@code DECIMAL(12, 2)}
   * @throws SchemaException if the database cannot store the decimal
   */
  public String sql(String keyword, Table table, Column column, DecimalType type) {
    if (type.precision() > precision || type.scale() > scale) {
      throw new UnsupportedChangeException(database + " cannot store the column " + table.name() + "." + column.name()
          + " with a precision of " + type.precision() + " and a scale of " + type.scale() + ". The maximum"
          + " precision is " + precision + " and the maximum scale is " + scale + ".");
    }
    return keyword + "(" + type.precision() + ", " + type.scale() + ")";
  }
}
