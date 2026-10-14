package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;

/**
 * Drops a table.
 *
 * @param table the table
 */
public record DropTableChange(TableName table) implements Change {

  @Override
  public String summary() {
    return "drop_" + table.name();
  }

  @Override
  public Phase phase() {
    return Phase.DROP_TABLES;
  }
}
