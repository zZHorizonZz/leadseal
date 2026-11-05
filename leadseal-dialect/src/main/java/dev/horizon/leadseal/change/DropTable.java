package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * Drops a table.
 *
 * @param table the table
 */
public record DropTable(TableName table) implements TableChange {

  @Override
  public String summary() {
    return "drop_" + table.name();
  }

  @Override
  public Phase phase() {
    return Phase.DROP_TABLES;
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return ddl.table().dropTable(table(transition));
  }
}
