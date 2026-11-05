package dev.horizon.leadseal.change;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
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
      Change {

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
    return ddl.column().renameColumn(table(transition).column(target), origin);
  }
}
