package dev.horizon.leadseal.change;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * Changes an attribute of a column.
 *
 * @param table the table
 * @param column the name of the column
 * @param origin the attribute before
 * @param target the attribute after, with the same key
 */
public record AlterColumnAttribute(TableName table, String column, @JsonProperty("from") ColumnAttribute origin,
    @JsonProperty("to") ColumnAttribute target) implements Change {

  @Override
  public String summary() {
    return "alter_" + table.name() + "_" + column;
  }

  @Override
  public Phase phase() {
    return Phase.ALTER_COLUMN_ATTRIBUTES;
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return target.alter(ddl, table(transition).column(column));
  }
}
