package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;

/**
 * Moves a table to another namespace and keeps its rows.
 *
 * @param from the table
 * @param to the new namespace, or {@code null} for the default namespace
 */
public record MoveTableChange(TableName from, String to) implements Change {

  @Override
  public TableName table() {
    return from.inNamespace(to);
  }

  @Override
  public String summary() {
    return "move_" + from.name() + "_to_" + (to == null ? "default" : to);
  }

  @Override
  public Phase phase() {
    return Phase.MOVE_TABLES;
  }
}
