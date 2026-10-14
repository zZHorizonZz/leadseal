package dev.horizon.leadseal.sqlserver;

import dev.horizon.leadseal.attribute.ConstraintName;
import java.util.List;

/**
 * The name of the default constraint of a column: {@code df_} followed by the table name and the column name.
 *
 * @param table the table name
 * @param column the column name
 */
record SqlServerDefaultName(String table, String column) {

  /**
   * Returns the name.
   *
   * @return the name, at most 63 characters long
   */
  String value() {
    return new ConstraintName("df", table, List.of(column)).value();
  }
}
