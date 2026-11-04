package dev.horizon.leadseal.expression;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.type.Type;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A constant.
 *
 * @param type the type of the constant
 * @param value the value, in the canonical text form of the type, for example {@code 42} or
 * {@code 2026-10-14T07:00:00Z}
 */
public record Literal(Type type, @JsonInclude(JsonInclude.Include.ALWAYS) String value) implements Value {

  public Literal {
    Objects.requireNonNull(type, "type");
    if (value == null || !type.accepts(value)) {
      throw new InvalidSchemaException("The constant '" + value + "' of an expression is not a value of its type. An"
          + " example of the type is " + type.example() + ".");
    }
  }

  @Override
  public String sql(SqlEnvironment environment) {
    return environment.dialect().literal(type, value);
  }

  @Override
  public List<SqlExpression> operands() {
    return List.of();
  }

  @Override
  public SqlExpression rename(UnaryOperator<String> names) {
    return this;
  }
}
