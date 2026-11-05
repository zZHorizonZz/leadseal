package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Drops a column.
 *
 * @param table the table
 * @param column the name of the column
 */
public record DropColumn(TableName table, String column) implements TableChange {

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
    TableTransition target = table(transition);
    ColumnTransition origin = target.originColumn(column);
    List<Command> all = new ArrayList<>();
    ddl.dialect().typeCheck(target.name().name(), column, origin.origin().orElseThrow().type())
        .ifPresent(check -> all.addAll(ddl.constraint().dropCheck(target, check)));
    all.addAll(ddl.column().dropColumn(origin));
    return all;
  }

  @Override
  public Optional<String> warning() {
    return Optional.of("drops the column " + table + "." + column + " and its values");
  }
}
