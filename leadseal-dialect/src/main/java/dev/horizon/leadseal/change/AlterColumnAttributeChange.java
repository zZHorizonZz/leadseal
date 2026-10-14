package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.attribute.ColumnAttribute;

/**
 * Replaces an attribute of a column with another attribute with the same key, such as another default.
 *
 * @param table the table
 * @param column the column name
 * @param from the attribute before
 * @param to the attribute after
 */
public record AlterColumnAttributeChange(TableName table, String column, ColumnAttribute from, ColumnAttribute to)
    implements
      Change {

  @Override
  public String summary() {
    return "alter_" + table.name() + "_" + column;
  }

  @Override
  public Phase phase() {
    return Phase.ALTER_COLUMN_ATTRIBUTES;
  }
}
