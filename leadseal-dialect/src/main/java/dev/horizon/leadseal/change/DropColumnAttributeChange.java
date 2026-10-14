package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.ColumnAttribute;

/**
 * Drops an attribute of a column.
 *
 * @param table the table
 * @param column the column name
 * @param attribute the attribute before the change
 */
public record DropColumnAttributeChange(TableName table, String column, ColumnAttribute attribute) implements Change {

  @Override
  public String summary() {
    return "alter_" + table.name() + "_" + column;
  }

  @Override
  public Phase phase() {
    return Phase.DROP_COLUMN_ATTRIBUTES;
  }
}
