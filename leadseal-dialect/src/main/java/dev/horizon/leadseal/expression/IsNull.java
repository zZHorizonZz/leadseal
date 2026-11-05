package dev.horizon.leadseal.expression;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Whether a value is {@code NULL}: {@code value IS NULL}. Its negation is {@code value IS NOT NULL}.
 *
 * @param operand the value
 */
public record IsNull(SqlExpression operand) implements Condition {

  public IsNull {
    Objects.requireNonNull(operand, "operand");
  }

  @Override
  public String sql(SqlEnvironment environment) {
    return "(" + operand.value(environment) + " IS NULL)";
  }

  @Override
  public String negation(SqlEnvironment environment) {
    return "(" + operand.value(environment) + " IS NOT NULL)";
  }

  @Override
  public List<SqlExpression> operands() {
    return List.of(operand);
  }

  @Override
  public SqlExpression rename(UnaryOperator<String> names) {
    return new IsNull(operand.rename(names));
  }
}
