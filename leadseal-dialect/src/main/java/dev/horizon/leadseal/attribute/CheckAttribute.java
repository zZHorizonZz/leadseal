package dev.horizon.leadseal.attribute;

import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.change.Change;
import java.util.List;

/**
 * A check constraint of a table.
 *
 * @param name the constraint name
 * @param expression the SQL condition that every row must meet, for example {@code quantity >= 0}. Every dialect
 * writes it as it is.
 */
public record CheckAttribute(String name, String expression) implements TableAttribute {

  public CheckAttribute {
    if (name == null || name.isBlank()) {
      throw new SchemaException("A check has no name.");
    }
    if (expression == null || expression.isBlank()) {
      throw new SchemaException("The check '" + name + "' has no expression.");
    }
  }

  /**
   * Returns a check with a generated name: {@code ck_} followed by the table name and a hash of the expression.
   *
   * @param table the table name
   * @param expression the SQL condition
   * @return the check
   */
  public static CheckAttribute of(String table, String expression) {
    return new CheckAttribute(ConstraintName.hashed("ck", table, expression).value(), expression);
  }

  @Override
  public Object key() {
    return List.of(CheckAttribute.class, name);
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
}
