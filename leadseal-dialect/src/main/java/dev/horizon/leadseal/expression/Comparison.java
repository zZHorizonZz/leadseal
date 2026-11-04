package dev.horizon.leadseal.expression;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A comparison of two values, such as {@code left < right}.
 *
 * @param operator the operator
 * @param left the left operand
 * @param right the right operand
 */
public record Comparison(Operator operator, SqlExpression left, SqlExpression right) implements Condition {

  public Comparison {
    Objects.requireNonNull(operator, "operator");
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");
  }

  @Override
  public String sql(SqlEnvironment environment) {
    return "(" + left.value(environment) + " " + operator.symbol + " " + right.value(environment) + ")";
  }

  @Override
  public List<SqlExpression> operands() {
    return List.of(left, right);
  }

  @Override
  public SqlExpression rename(UnaryOperator<String> names) {
    return new Comparison(operator, left.rename(names), right.rename(names));
  }

  /** The operators of {@link Comparison}. */
  public enum Operator {
    /** The values are equal. */
    EQUALS("="),
    /** The values differ. */
    NOT_EQUALS("<>"),
    /** The left value is less than the right value. */
    LESS("<"),
    /** The left value is less than or equal to the right value. */
    LESS_EQUALS("<="),
    /** The left value is greater than the right value. */
    GREATER(">"),
    /** The left value is greater than or equal to the right value. */
    GREATER_EQUALS(">=");

    private final String symbol;

    Operator(String symbol) {
      this.symbol = symbol;
    }
  }
}
