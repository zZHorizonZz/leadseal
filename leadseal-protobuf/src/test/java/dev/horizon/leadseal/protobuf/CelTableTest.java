package dev.horizon.leadseal.protobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.horizon.leadseal.Column;
import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Table;
import dev.horizon.leadseal.attribute.NotNull;
import dev.horizon.leadseal.attribute.PrimaryKey;
import dev.horizon.leadseal.expression.Arithmetic;
import dev.horizon.leadseal.expression.Call;
import dev.horizon.leadseal.expression.Call.Function;
import dev.horizon.leadseal.expression.ColumnChange;
import dev.horizon.leadseal.expression.ColumnReference;
import dev.horizon.leadseal.expression.Comparison;
import dev.horizon.leadseal.expression.Conditional;
import dev.horizon.leadseal.expression.In;
import dev.horizon.leadseal.expression.IsNull;
import dev.horizon.leadseal.expression.Literal;
import dev.horizon.leadseal.expression.Logic;
import dev.horizon.leadseal.expression.Negative;
import dev.horizon.leadseal.expression.Not;
import dev.horizon.leadseal.expression.Row;
import dev.horizon.leadseal.expression.SqlExpression;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.DateType;
import dev.horizon.leadseal.type.DecimalType;
import dev.horizon.leadseal.type.FloatType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.TextType;
import dev.horizon.leadseal.type.TimestampType;
import dev.horizon.leadseal.type.Type;
import dev.horizon.leadseal.type.UuidType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class CelTableTest {

  private static final Type BOOLEAN = new BooleanType();
  private static final Type BIGINT = new IntegerType(IntegerType.Size.INT64);
  private static final Type DOUBLE = new FloatType(FloatType.Size.FLOAT64);
  private static final Type TEXT = TextType.unlimited();
  private static final Table ORDERS = Table.of("shop.Order", "orders", List.of(
      Column.of("1", "id", new UuidType(), NotNull.INSTANCE),
      Column.of("2", "quantity", new IntegerType(IntegerType.Size.INT32), NotNull.INSTANCE),
      Column.of("3", "price", new DecimalType(12, 2), NotNull.INSTANCE),
      Column.of("4", "state", TextType.of(20), NotNull.INSTANCE),
      Column.of("5", "nick_name", TextType.of(40)),
      Column.of("6", "paid", BOOLEAN, NotNull.INSTANCE),
      Column.of("7", "created", new TimestampType(), NotNull.INSTANCE),
      Column.of("8", "due", new DateType())), PrimaryKey.of("orders", List.of("id")));
  private static final CelTable TABLE = new CelTable(ORDERS);
  private static final ColumnReference QUANTITY = ColumnReference.of("quantity");
  private static final ColumnReference STATE = ColumnReference.of("state");
  private static final ColumnReference NICK_NAME = ColumnReference.of("nick_name");
  private static final ColumnReference PAID = ColumnReference.of("paid");

  private static Literal integer(long value) {
    return new Literal(BIGINT, Long.toString(value));
  }

  private static Literal text(String value) {
    return new Literal(TEXT, value);
  }

  private static Call call(Function function, SqlExpression... arguments) {
    return new Call(function, List.of(arguments));
  }

  static Stream<Arguments> expressions() {
    return Stream.of(
        Arguments.of("quantity > 0 && !paid || state in ['OPEN', 'PAID']", BOOLEAN, new Logic(Logic.Operator.OR,
            new Logic(Logic.Operator.AND, new Comparison(Comparison.Operator.GREATER, QUANTITY, integer(0)),
                new Not(PAID)),
            new In(STATE, List.of(text("OPEN"), text("PAID"))))),
        Arguments.of("nick_name == null", BOOLEAN, new IsNull(NICK_NAME)),
        Arguments.of("nick_name != null", BOOLEAN, new Not(new IsNull(NICK_NAME))),
        Arguments.of("has(nick_name)", BOOLEAN, new Not(new IsNull(NICK_NAME))),
        Arguments.of("-quantity / 2 % 3", BIGINT, new Arithmetic(Arithmetic.Operator.MODULO, new Arithmetic(
            Arithmetic.Operator.INTEGER_DIVIDE, new Negative(QUANTITY), integer(2)), integer(3))),
        Arguments.of("price / 2.0", DOUBLE, new Arithmetic(Arithmetic.Operator.DIVIDE, ColumnReference.of("price"),
            new Literal(DOUBLE, "2.0"))),
        Arguments.of("state + '!'", TEXT, new Arithmetic(Arithmetic.Operator.CONCAT, STATE, text("!"))),
        Arguments.of("paid ? lower(coalesce(nick_name, state)) : uuid()", TEXT, new Conditional(PAID,
            call(Function.LOWER, call(Function.COALESCE, NICK_NAME, STATE)), call(Function.UUID))),
        Arguments.of("abs(quantity) == length(trim(state)) && created < now()", BOOLEAN, new Logic(Logic.Operator.AND,
            new Comparison(Comparison.Operator.EQUALS, call(Function.ABS, QUANTITY), call(Function.LENGTH,
                call(Function.TRIM, STATE))),
            new Comparison(Comparison.Operator.LESS, ColumnReference.of("created"),
                call(Function.NOW)))));
  }

  @ParameterizedTest(name = "({0})", quoteTextArguments = false)
  @MethodSource("expressions")
  @DisplayName("Reads an expression")
  void readExpression(String text, Type result, SqlExpression expression) {
    assertThat(TABLE.parse(text, Set.of(Row.CURRENT), result)).isEqualTo(expression);
  }

  @Test
  @DisplayName("Reads the rows of a trigger")
  void readTriggerRows() {
    assertThat(TABLE.parse("changed(state) && new.quantity > old.quantity", Set.of(Row.CURRENT, Row.NEW, Row.OLD),
        BOOLEAN)).isEqualTo(new Logic(Logic.Operator.AND, new ColumnChange("state"),
            new Comparison(
                Comparison.Operator.GREATER, new ColumnReference("quantity", Row.NEW), new ColumnReference("quantity",
                    Row.OLD))));
  }

  @Test
  @DisplayName("Reads fields by other names")
  void readRenamedFields() {
    assertThat(new CelTable(ORDERS, Map.of("nickName", "nick_name")).parse("nickName == 'x'", Set.of(Row.CURRENT),
        BOOLEAN)).isEqualTo(new Comparison(Comparison.Operator.EQUALS, NICK_NAME, text("x")));
  }

  @ParameterizedTest(name = "({0})", quoteTextArguments = false)
  @CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
      quantity + 1                          | expected an expression of type bool
      missing > 0                           | undeclared reference to 'missing'
      due == null                           | the field due has the type date
      state == null                         | the field 'state' always has a value
      price * 2 > 1.0                       | no matching overload
      quantity / quantity > 1               | must be a constant that is not zero
      state.startsWith('O')                 | startsWith()
      new.quantity > 0                      | undeclared reference to 'new'
      changed(state)                        | changed(), which needs the old and the new row
      changed(lower(state))                 | changed() takes a field of the row
      created - created > duration('1s')    | durations are not supported
      """)
  void refusesWhatItCannotWrite(String text, String message) {
    assertThatThrownBy(() -> TABLE.parse(text, Set.of(Row.CURRENT), BOOLEAN)).isInstanceOf(InvalidSchemaException.class)
        .hasMessageContaining(message).hasMessageContaining("The expression '" + text + "'");
  }
}
