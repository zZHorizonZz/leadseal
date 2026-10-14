package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.ColumnAttribute;

/**
 * Adds an attribute to a column, such as {@code NOT NULL} or a default.
 *
 * @param table the table
 * @param column the column name
 * @param attribute the attribute
 */
public record AddColumnAttributeChange(TableName table, String column, ColumnAttribute attribute) implements Change {

  @Override
  public String summary() {
    return "alter_" + table.name() + "_" + column;
  }

  @Override
  public Phase phase() {
    return Phase.ALTER_COLUMN_ATTRIBUTES;
  }
}
