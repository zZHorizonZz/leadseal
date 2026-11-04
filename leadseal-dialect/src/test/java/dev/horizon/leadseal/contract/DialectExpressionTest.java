package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.Dialect;
import dev.horizon.leadseal.expression.Arithmetic;
import dev.horizon.leadseal.expression.Call;
import dev.horizon.leadseal.expression.Call.Function;
import dev.horizon.leadseal.expression.ColumnChange;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.Comparison;
import dev.horizon.leadseal.expression.Literal;
import dev.horizon.leadseal.expression.Not;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.expression.SqlEnvironment;
import dev.horizon.leadseal.expression.SqlExpression;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.TextType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public abstract class DialectExpressionTest {

  private static final ColumnReference QUANTITY = ColumnReference.of("quantity");
  private static final ColumnReference NAME = ColumnReference.of("name");
  private static final ColumnReference PAID = ColumnReference.of("paid");
  private static final Literal TWO = new Literal(new IntegerType(IntegerType.Size.INT64), "2");
  private static final Literal BANG = new Literal(TextType.unlimited(), "!");
  private static final Comparison POSITIVE = new Comparison(Comparison.Operator.GREATER, QUANTITY, TWO);

  protected abstract Dialect dialect();

  protected abstract String sql();

  @ParameterizedTest(name = "{0}")
  @EnumSource(Example.class)
  void writesTheSqlOfTheDatabase(Example example) {
    SqlEnvironment trigger = SqlEnvironment.of(dialect()).with(Row.CURRENT, "NEW").with(Row.NEW, "NEW")
        .with(Row.OLD, "OLD");
    Map<Example, String> expected = new EnumMap<>(Example.class);
    sql().lines().map(line -> line.strip().split("\\s+\\|\\s+", 2))
        .forEach(cells -> expected.put(Example.valueOf(cells[0]), cells[1]));

    assertThat(expected).containsKey(example);
    assertThat(example.write(trigger)).isEqualTo(expected.get(example));
  }

  protected enum Example {
    PAID_CONDITION(PAID, Place.CONDITION),
    NOT_PAID_CONDITION(new Not(PAID), Place.CONDITION),
    POSITIVE_CONDITION(POSITIVE, Place.CONDITION),
    POSITIVE_VALUE(POSITIVE, Place.VALUE),
    POSITIVE_EQUALS_PAID_CONDITION(new Comparison(Comparison.Operator.EQUALS, POSITIVE,
        PAID), Place.CONDITION),
    TRUE_CONDITION(new Literal(new BooleanType(), "true"), Place.CONDITION),
    CONCAT_VALUE(new Arithmetic(Arithmetic.Operator.CONCAT, NAME, BANG), Place.VALUE),
    INTEGER_DIVIDE_VALUE(new Arithmetic(Arithmetic.Operator.INTEGER_DIVIDE, QUANTITY, TWO), Place.VALUE),
    LENGTH_VALUE(new Call(Function.LENGTH, List.of(NAME)), Place.VALUE),
    CHANGE_CONDITION(new ColumnChange("name"), Place.CONDITION),
    NOW_VALUE(new Call(Function.NOW, List.of()), Place.VALUE),
    UUID_VALUE(new Call(Function.UUID, List.of()), Place.VALUE);

    private final SqlExpression expression;
    private final Place place;

    Example(SqlExpression expression, Place place) {
      this.expression = expression;
      this.place = place;
    }

    String write(SqlEnvironment environment) {
      return place.write(expression, environment);
    }
  }

  private enum Place {
    CONDITION {
      @Override
      String write(SqlExpression expression, SqlEnvironment environment) {
        return expression.condition(environment);
      }
    },
    VALUE {
      @Override
      String write(SqlExpression expression, SqlEnvironment environment) {
        return expression.value(environment);
      }
    };

    abstract String write(SqlExpression expression, SqlEnvironment environment);
  }
}
