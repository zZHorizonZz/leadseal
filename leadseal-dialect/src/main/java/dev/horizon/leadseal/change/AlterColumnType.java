package dev.horizon.leadseal.change;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import dev.horizon.leadseal.type.Type;
import java.util.List;

/**
 * Changes the type of a column and keeps its values.
 *
 * @param table the table
 * @param column the name of the column
 * @param origin the type before
 * @param target the type after
 */
public record AlterColumnType(TableName table, String column, @JsonProperty("from") Type origin,
    @JsonProperty("to") Type target) implements Change {

  @Override
  public String summary() {
    return "alter_" + table.name() + "_" + column;
  }

  @Override
  public Phase phase() {
    return Phase.ALTER_COLUMN_TYPES;
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return ddl.column().alterColumnType(table(transition).column(column));
  }
}
