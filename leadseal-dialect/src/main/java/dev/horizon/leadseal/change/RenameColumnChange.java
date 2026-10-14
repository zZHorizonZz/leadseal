package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;

/**
 * Renames a column and keeps its values.
 *
 * @param table the table
 * @param from the old column name
 * @param to the new column name
 */
public record RenameColumnChange(TableName table, String from, String to) implements Change {

  @Override
  public String summary() {
    return "rename_" + table.name() + "_" + from + "_to_" + to;
  }

  @Override
  public Phase phase() {
    return Phase.RENAME_COLUMNS;
  }
}
