package dev.horizon.leadseal.change;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.type.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Renames a column and keeps its values.
 *
 * @param table the table
 * @param origin the name of the column
 * @param target the new name of the column
 */
public record RenameColumn(TableName table, @JsonProperty("from") String origin, @JsonProperty("to") String target)
    implements
      TableChange {

  @Override
  public String summary() {
    return "rename_" + table.name() + "_" + origin + "_to_" + target;
  }

  @Override
  public Phase phase() {
    return Phase.RENAME_COLUMNS;
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    TableTransition table = table(transition);
    ColumnTransition column = table.column(target);
    List<Command> all = new ArrayList<>(ddl.column().renameColumn(column, origin));
    Type type = column.origin().orElseThrow().type();
    String name = table.name().name();
    ddl.dialect().typeCheck(name, origin, type).ifPresent(check -> all.addAll(ddl.constraint().renameCheck(table, check,
        ddl.dialect().typeCheck(name, target, type).orElseThrow())));
    return all;
  }
}
