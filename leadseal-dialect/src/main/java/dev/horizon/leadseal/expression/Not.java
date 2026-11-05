package dev.horizon.leadseal.expression;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * The negation of a condition: {@code NOT condition}.
 *
 * @param operand the condition
 */
public record Not(SqlExpression operand) implements Condition {

  public Not {
    Objects.requireNonNull(operand, "operand");
  }

  @Override
  public String sql(SqlEnvironment environment) {
    return operand.negation(environment);
  }

  @Override
  public SqlExpression negate() {
    return operand;
  }

  @Override
  public List<SqlExpression> operands() {
    return List.of(operand);
  }

  @Override
  public SqlExpression rename(UnaryOperator<String> names) {
    return new Not(operand.rename(names));
  }
}
