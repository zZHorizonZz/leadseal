package dev.horizon.leadseal.change;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds a column.
 *
 * @param table the table
 * @param column the column
 */
public record AddColumn(TableName table, Column column) implements TableChange {

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
    TableTransition target = table(transition);
    List<Command> all = new ArrayList<>(ddl.column().addColumn(target.column(column.name())));
    ddl.dialect().typeCheck(target.name().name(), column.name(), column.type())
        .ifPresent(check -> all.addAll(ddl.constraint().addCheck(target, check)));
    return all;
  }
}
