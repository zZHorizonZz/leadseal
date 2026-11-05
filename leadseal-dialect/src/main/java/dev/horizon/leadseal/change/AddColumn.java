package dev.horizon.leadseal.change;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * Adds a column.
 *
 * @param table the table
 * @param column the column
 */
public record AddColumn(TableName table, Column column) implements Change {

  @Override
  public String summary() {
    return "add_" + table.name() + "_" + column.name();
  }

  @Override
  public Phase phase() {
    return Phase.ADD_COLUMNS;
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return ddl.column().addColumn(table(transition).column(column.name()));
  }
}
