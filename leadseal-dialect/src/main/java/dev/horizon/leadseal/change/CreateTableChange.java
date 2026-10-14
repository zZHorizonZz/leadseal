package dev.horizon.leadseal.change;

import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;

/**
 * Creates a table with its columns and attributes. Its foreign keys are separate {@link AddTableAttributeChange}s,
 * which run after every table exists.
 *
 * @param namespace the namespace of the table, or {@code null} for the default namespace
 * @param definition the table, without foreign keys
 */
public record CreateTableChange(String namespace, Table definition) implements Change {

  @Override
  public TableName table() {
    return new TableName(namespace, definition.name());
  }

  @Override
  public String summary() {
    return "create_" + definition.name();
  }

  @Override
  public Phase phase() {
    return Phase.CREATE_TABLES;
  }
}
