package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.Transition.ColumnTransition;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * The default value of a column: a literal, or a value that the database generates for each new row. A column has at
 * most one default.
 */
public sealed interface Default extends ColumnAttribute permits LiteralDefault, GeneratedDefault {

  @Override
  default Object key() {
    return Default.class;
  }

  /**
   * Writes this default in the SQL of a dialect.
   *
   * @param dialect the dialect
   * @param column the column of the default
   * @return the literal or the expression
   */
  String sql(Dialect dialect, Column column);

  @Override
  default List<Command> add(Ddl ddl, ColumnTransition column) {
    return ddl.column().setDefault(column);
  }

  @Override
  default List<Command> drop(Ddl ddl, ColumnTransition column) {
    return ddl.column().dropDefault(column);
  }

  /**
   * Tells if the default is a constant, which the database does not compute for each row.
   *
   * @return true for a literal. The default is false.
   */
  default boolean constant() {
    return false;
  }
}
