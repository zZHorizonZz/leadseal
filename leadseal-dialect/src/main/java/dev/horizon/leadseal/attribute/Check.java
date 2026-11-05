package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.Transition.TableTransition;
import dev.horizon.leadseal.change.Change;
import dev.horizon.leadseal.command.Command;
import dev.horizon.leadseal.command.Ddl;
import java.util.List;

/**
 * A check constraint of a table.
 *
 * @param name the constraint name
 * @param expression the SQL condition that every row must meet, for example {@code quantity >= 0}. Every dialect
 * writes it as it is.
 */
public record Check(String name, String expression) implements TableAttribute {

  public Check {
    if (name == null || name.isBlank()) {
      throw new InvalidSchemaException("A check has no name.");
    }
    if (expression == null || expression.isBlank()) {
      throw new InvalidSchemaException("The check '" + name + "' has no expression.");
    }
  }

  /**
   * Returns a check with a generated name: {@code ck_} followed by the table name and a hash of the expression.
   *
   * @param table the table name
   * @param expression the SQL condition
   * @return the check
   */
  public static Check of(String table, String expression) {
    return new Check(ConstraintName.digest("ck", table, expression).value(), expression);
  }

  @Override
  public Object key() {
    return List.of(Check.class, name);
  }

  @Override
  public String summary(String table) {
    return name;
  }

  @Override
  public void validate(Table table) {
  }

  @Override
  public Change.Phase phase() {
    return Change.Phase.ADD_CHECKS;
  }

  @Override
  public Change.Phase dropPhase() {
    return Change.Phase.DROP_CHECKS;
  }

  @Override
  public List<Command> add(Ddl ddl, TableTransition table) {
    return ddl.constraint().addCheck(table, this);
  }

  @Override
  public List<Command> drop(Ddl ddl, TableTransition table) {
    return ddl.constraint().dropCheck(table, this);
  }
}
