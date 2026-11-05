package dev.horizon.leadseal.change;

import dev.horizon.leadseal.Namespace;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * Creates a table, without its foreign keys.
 *
 * @param namespace the namespace of the table
 * @param definition the table without its foreign keys
 */
public record CreateTable(Namespace.Name namespace, Table definition) implements Change {

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

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return ddl.table().createTable(table(transition), definition);
  }
}
