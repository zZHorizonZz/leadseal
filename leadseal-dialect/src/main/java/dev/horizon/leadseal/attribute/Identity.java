package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * A column whose values the database generates for new rows. An identity column is an integer, does not accept
 * {@code NULL}, has no default, and is the only column of the primary key of its table.
 */
public record Identity() implements ColumnAttribute {

  public static final Identity INSTANCE = new Identity();

  @Override
  public void validate(Column column) {
    if (!column.type().counts() || column.nullable()
        || column.attribute(Default.class).isPresent()) {
      throw new InvalidSchemaException("The identity column '" + column.name() + "' must be an integer column that does not"
          + " accept NULL and has no default.");
    }
  }

  @Override
  public List<Command> add(Ddl ddl, ColumnTransition column) {
    return ddl.column().addIdentity(column);
  }

  @Override
  public List<Command> drop(Ddl ddl, ColumnTransition column) {
    return ddl.column().dropIdentity(column);
  }

  @Override
  public void validate(Table table, Column column) {
    if (!table.primaryKey().columns().equals(List.of(column.name()))) {
      throw new InvalidSchemaException("The identity column '" + table.name() + "." + column.name() + "' must be the only"
          + " column of the primary key.");
    }
  }
}
