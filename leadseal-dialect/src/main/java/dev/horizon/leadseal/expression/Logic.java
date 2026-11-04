package dev.horizon.leadseal.expression;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Two conditions joined with {@code AND} or {@code OR}.
 *
 * @param operator the operator
 * @param left the left condition
 * @param right the right condition
 */
public record Logic(Operator operator, SqlExpression left, SqlExpression right) implements Condition {

  public Logic {
    Objects.requireNonNull(operator, "operator");
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");
  }

  @Override
  public String sql(SqlEnvironment environment) {
    return "(" + left.condition(environment) + " " + operator.name() + " " + right.condition(environment) + ")";
  }

  @Override
  public List<SqlExpression> operands() {
    return List.of(left, right);
  }

  @Override
  public SqlExpression rename(UnaryOperator<String> names) {
    return new Logic(operator, left.rename(names), right.rename(names));
  }

  /** The operators of {@link Logic}. */
  public enum Operator {
    /** Both conditions. */
    AND,
    /** One or both of the conditions. */
    OR
  }
}
