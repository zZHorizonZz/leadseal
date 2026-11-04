package dev.horizon.leadseal.expression;

import dev.horizon.leadseal.Dialect;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * An arithmetic operator on two values, such as {@code left + right}.
 *
 * @param operator the operator
 * @param left the left operand
 * @param right the right operand
 */
public record Arithmetic(Operator operator, SqlExpression left, SqlExpression right) implements Value {

  public Arithmetic {
    Objects.requireNonNull(operator, "operator");
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");
  }

  @Override
  public String sql(SqlEnvironment environment) {
    return operator.sql(environment.dialect(), left.value(environment), right.value(environment));
  }

  @Override
  public List<SqlExpression> operands() {
    return List.of(left, right);
  }

  @Override
  public SqlExpression rename(UnaryOperator<String> names) {
    return new Arithmetic(operator, left.rename(names), right.rename(names));
  }

  /** The operators of {@link Arithmetic}. */
  public enum Operator {
    /** The sum of two numbers. */
    ADD("+"),
    /** The difference of two numbers. */
    SUBTRACT("-"),
    /** The product of two numbers. */
    MULTIPLY("*"),
    /** The quotient of two numbers that are not both integers. */
    DIVIDE("/"),
    /** The remainder of the division of two integers, with the sign of the dividend. */
    MODULO("%"),
    /** The quotient of two integers, rounded toward zero. */
    INTEGER_DIVIDE("/") {
      @Override
      String sql(Dialect dialect, String left, String right) {
        return dialect.quotient(left, right);
      }
    },
    /** Two texts, one after the other. The value is {@code NULL} if either text is. */
    CONCAT("||") {
      @Override
      String sql(Dialect dialect, String left, String right) {
        return dialect.concatenation(left, right);
      }
    };

    private final String symbol;

    Operator(String symbol) {
      this.symbol = symbol;
    }

    String sql(Dialect dialect, String left, String right) {
      return "(" + left + " " + symbol + " " + right + ")";
    }
  }
}
