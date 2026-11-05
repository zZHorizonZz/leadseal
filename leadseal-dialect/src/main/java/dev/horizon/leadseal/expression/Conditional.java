package dev.horizon.leadseal.expression;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * One of two values, by a condition: {@code CASE WHEN condition THEN then ELSE otherwise END}.
 *
 * @param condition the condition
 * @param then the value if the condition is true
 * @param otherwise the value if the condition is false or {@code NULL}
 */
public record Conditional(SqlExpression condition, SqlExpression then, SqlExpression otherwise) implements Value {

  public Conditional {
    Objects.requireNonNull(condition, "condition");
    Objects.requireNonNull(then, "then");
    Objects.requireNonNull(otherwise, "otherwise");
  }

  @Override
  public String sql(SqlEnvironment environment) {
    return "CASE WHEN " + condition.condition(environment) + " THEN " + then.value(environment) + " ELSE "
        + otherwise.value(environment) + " END";
  }

  @Override
  public List<SqlExpression> operands() {
    return List.of(condition, then, otherwise);
  }

  @Override
  public SqlExpression rename(UnaryOperator<String> names) {
    return new Conditional(condition.rename(names), then.rename(names), otherwise.rename(names));
  }
}
