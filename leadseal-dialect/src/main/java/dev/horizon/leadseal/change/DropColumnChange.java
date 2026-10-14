package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;

/**
 * Drops a column.
 *
 * @param table the table
 * @param column the column name
 */
public record DropColumnChange(TableName table, String column) implements Change {

  @Override
  public String summary() {
    return "drop_" + table.name() + "_" + column;
  }

  @Override
  public Phase phase() {
    return Phase.DROP_COLUMNS;
  }
}
