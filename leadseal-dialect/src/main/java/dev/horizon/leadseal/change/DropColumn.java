package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * Drops a column.
 *
 * @param table the table
 * @param column the name of the column
 */
public record DropColumn(TableName table, String column) implements Change {

  @Override
  public String summary() {
    return "drop_" + table.name() + "_" + column;
  }

  @Override
  public Phase phase() {
    return Phase.DROP_COLUMNS;
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return ddl.column().dropColumn(table(transition).originColumn(column));
  }
}
