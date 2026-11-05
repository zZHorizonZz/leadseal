package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/** A column that does not accept {@code NULL}. */
public record NotNull() implements ColumnAttribute {

  public static final NotNull INSTANCE = new NotNull();

  @Override
  public void validate(Column column) {
  }

  @Override
  public List<Command> add(Ddl ddl, ColumnTransition column) {
    return ddl.column().setNotNull(column);
  }

  @Override
  public List<Command> drop(Ddl ddl, ColumnTransition column) {
    return ddl.column().dropNotNull(column);
  }
}
