package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Type;

/**
 * Changes the type of a column and keeps its values.
 *
 * @param table the table
 * @param column the column name
 * @param from the type before
 * @param to the type after
 */
public record AlterColumnTypeChange(TableName table, String column, Type from, Type to) implements Change {

  @Override
  public String summary() {
    return "alter_" + table.name() + "_" + column;
  }

  @Override
  public Phase phase() {
    return Phase.ALTER_COLUMN_TYPES;
  }
}
