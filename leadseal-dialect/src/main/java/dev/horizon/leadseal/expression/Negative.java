package dev.horizon.leadseal.expression;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * The negative of a number: {@code -value}.
 *
 * @param operand the number
 */
public record Negative(SqlExpression operand) implements Value {

  public Negative {
    Objects.requireNonNull(operand, "operand");
  }

  @Override
  public String sql(SqlEnvironment environment) {
    return "(-" + operand.value(environment) + ")";
  }

  @Override
  public List<SqlExpression> operands() {
    return List.of(operand);
  }

  @Override
  public SqlExpression rename(UnaryOperator<String> names) {
    return new Negative(operand.rename(names));
  }
}
