package dev.horizon.leadseal.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Whether a value is one of a list of values: {@code value IN (a, b)}. An empty list is a condition that is never
 * true.
 *
 * @param element the value
 * @param values the list, which can be empty
 */
public record In(SqlExpression element, List<SqlExpression> values) implements Condition {

  public In {
    Objects.requireNonNull(element, "element");
    values = values == null ? List.of() : List.copyOf(values);
  }

  @Override
  public String sql(SqlEnvironment environment) {
    if (values.isEmpty()) {
      return "(1 = 0)";
    }
    return "(" + element.value(environment) + " IN (" + values.stream().map(value -> value.value(environment))
        .collect(Collectors.joining(", ")) + "))";
  }

  @Override
  public List<SqlExpression> operands() {
    List<SqlExpression> all = new ArrayList<>();
    all.add(element);
    all.addAll(values);
    return all;
  }

  @Override
  public SqlExpression rename(UnaryOperator<String> names) {
    return new In(element.rename(names), values.stream().map(value -> value.rename(names)).toList());
  }
}
