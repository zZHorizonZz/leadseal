package dev.horizon.leadseal.expression;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.InvalidSchemaException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A call of a function.
 *
 * @param function the function
 * @param arguments the arguments
 */
public record Call(Function function, List<SqlExpression> arguments) implements Value {

  public Call {
    Objects.requireNonNull(function, "function");
    arguments = arguments == null ? List.of() : List.copyOf(arguments);
    if (arguments.size() != function.arity()) {
      throw new InvalidSchemaException("The function " + function.describe() + " takes " + function.arity()
          + " arguments, not " + arguments.size() + ".");
    }
  }

  @Override
  public String sql(SqlEnvironment environment) {
    return function.sql(environment.dialect(), arguments.stream().map(argument -> argument.value(environment))
        .toList());
  }

  @Override
  public List<SqlExpression> operands() {
    return arguments;
  }

  @Override
  public boolean deterministic() {
    return function.deterministic() && Value.super.deterministic();
  }

  @Override
  public SqlExpression rename(UnaryOperator<String> names) {
    return new Call(function, arguments.stream().map(argument -> argument.rename(names)).toList());
  }

  /** The functions of {@link Call}. */
  public enum Function {
    /** The current time, in UTC, as a default of the current time writes it. */
    NOW(0, false) {
      @Override
      String sql(Dialect dialect, List<String> arguments) {
        return dialect.currentTimestamp();
      }
    },
    /** A random UUID, as a default of a random UUID writes it. */
    UUID(0, false) {
      @Override
      String sql(Dialect dialect, List<String> arguments) {
        return dialect.randomUuid();
      }
    },
    /** A text in lower case. */
    LOWER(1, true),
    /** A text in upper case. */
    UPPER(1, true),
    /** A text without the spaces at its start and its end. */
    TRIM(1, true),
    /** The number of characters of a text, with the spaces at its end. */
    LENGTH(1, true) {
      @Override
      String sql(Dialect dialect, List<String> arguments) {
        return dialect.length(arguments.getFirst());
      }
    },
    /** The absolute value of a number. */
    ABS(1, true),
    /** The first of two values that is not {@code NULL}. */
    COALESCE(2, true);

    private final int arity;
    private final boolean deterministic;

    Function(int arity, boolean deterministic) {
      this.arity = arity;
      this.deterministic = deterministic;
    }

    /**
     * Gives the number of arguments of the function.
     *
     * @return the number of arguments
     */
    public int arity() {
      return arity;
    }

    /**
     * Tells if the function gives the same value for the same arguments each time.
     *
     * @return false for {@link #NOW} and {@link #UUID}
     */
    public boolean deterministic() {
      return deterministic;
    }

    /**
     * Gives the name of the function in a message.
     *
     * @return the name, for example {@code lower}
     */
    public String describe() {
      return name().toLowerCase(Locale.ROOT);
    }

    String sql(Dialect dialect, List<String> arguments) {
      return name() + "(" + String.join(", ", arguments) + ")";
    }
  }
}
