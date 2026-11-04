package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * Drops an attribute of a column.
 *
 * @param table the table
 * @param column the name of the column
 * @param attribute the attribute
 */
public record DropColumnAttribute(TableName table, String column, ColumnAttribute attribute) implements Change {

  @Override
  public String summary() {
    return "alter_" + table.name() + "_" + column;
  }

  @Override
  public Phase phase() {
    return Phase.DROP_COLUMN_ATTRIBUTES;
  }

  @Override
  public List<Command> commands(Ddl ddl, Transition transition) {
    return attribute.drop(ddl, table(transition).column(column));
  }
}
