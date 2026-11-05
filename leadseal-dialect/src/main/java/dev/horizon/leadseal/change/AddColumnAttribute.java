package dev.horizon.leadseal.change;

import dev.horizon.leadseal.TableName;
import dev.horizon.leadseal.Transition;
import dev.horizon.leadseal.attribute.ColumnAttribute;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * Adds an attribute to a column.
 *
 * @param table the table
 * @param column the name of the column
 * @param attribute the attribute
 */
public record AddColumnAttribute(TableName table, String column, ColumnAttribute attribute) implements Change {

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
    return attribute.add(ddl, table(transition).column(column));
  }
}
