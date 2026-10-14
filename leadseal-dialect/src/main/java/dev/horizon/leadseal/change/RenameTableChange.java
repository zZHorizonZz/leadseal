package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;

/**
 * Renames a table and keeps its rows. The table stays in its namespace.
 *
 * @param from the table
 * @param to the new table name
 */
public record RenameTableChange(TableName from, String to) implements Change {

  @Override
  public TableName table() {
    return from.withName(to);
  }

  @Override
  public String summary() {
    return "rename_" + from.name() + "_to_" + to;
  }

  @Override
  public Phase phase() {
    return Phase.RENAME_TABLES;
  }
}
