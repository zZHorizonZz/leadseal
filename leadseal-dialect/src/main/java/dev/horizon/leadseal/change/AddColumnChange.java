package dev.horizon.leadseal.change;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.TableName;

/**
 * Adds a column with its attributes.
 *
 * @param table the table
 * @param column the column
 */
public record AddColumnChange(TableName table, Column column) implements Change {

  @Override
  public String summary() {
    return "add_" + table.name() + "_" + column.name();
  }

  @Override
  public Phase phase() {
    return Phase.ADD_COLUMNS;
  }
}
