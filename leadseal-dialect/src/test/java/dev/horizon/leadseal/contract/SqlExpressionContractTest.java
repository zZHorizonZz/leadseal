package dev.horizon.leadseal.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.leadseal.Schema;
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
import dev.horizon.leadseal.expression.SqlEnvironment;
import dev.horizon.leadseal.expression.SqlExpression;
import dev.horizon.leadseal.type.TextType;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class SqlExpressionContractTest extends ContractTest {

  private static final ColumnReference QUANTITY = ColumnReference.of("quantity");
  private static final ColumnReference PRICE = ColumnReference.of("price");
  private static final ColumnReference NAME = ColumnReference.of("name");
  private static final ColumnReference NICKNAME = ColumnReference.of("nickname");
  private static final ColumnReference PAID = ColumnReference.of("paid");
  private static final ColumnReference CREATED = ColumnReference.of("created");

  private static Literal integer(long value) {
    return new Literal(BIGINT, Long.toString(value));
  }

  private static Literal real(String value) {
    return new Literal(DOUBLE, value);
  }

  private static Literal text(String value) {
    return new Literal(TEXT, value);
  }

  private static Literal bool(boolean value) {
    return new Literal(BOOLEAN, Boolean.toString(value));
  }

  private static Comparison compare(Comparison.Operator operator, SqlExpression left, SqlExpression right) {
    return new Comparison(operator, left, right);
  }

  private static Comparison equal(SqlExpression left, SqlExpression right) {
    return compare(Comparison.Operator.EQUALS, left, right);
  }

  private static Arithmetic arithmetic(Arithmetic.Operator operator, SqlExpression left, SqlExpression right) {
    return new Arithmetic(operator, left, right);
  }

  private static Call call(Function function, SqlExpression... arguments) {
    return new Call(function, List.of(arguments));
  }

  @BeforeEach
  void createSamples() throws SQLException {
    apply(Schema.of(table("shop.Sample", "samples", List.of(column("1", "id", BIGINT), column("2", "quantity", INT),
        column("3", "price", DOUBLE), text("4", "name", 40), nullable("5", "nickname", TextType.of(40)),
        column("6", "paid", BOOLEAN), column("7", "created", TIMESTAMP)), "id")));
    insert(1, 7, "-2.5", " Ana ", null, true, "2020-01-01T00:00:00Z");
    insert(2, -3, "4.0", "Bob", "Bobby", false, "2099-01-01T00:00:00Z");
  }

  private void insert(long id, int quantity, String price, String name, String nickname, boolean paid,
      String created) throws SQLException {
    execute("INSERT INTO " + q("samples") + " (" + q("id") + ", " + q("quantity") + ", " + q("price") + ", "
        + q("name") + ", " + q("nickname") + ", " + q("paid") + ", " + q("created") + ") VALUES ("
        + dialect.literal(BIGINT, Long.toString(id)) + ", " + dialect.literal(INT, Integer.toString(quantity)) + ", "
        + dialect.literal(DOUBLE, price) + ", " + dialect.literal(TEXT, name) + ", "
        + (nickname == null ? "NULL" : dialect.literal(TEXT, nickname)) + ", "
        + dialect.literal(BOOLEAN, Boolean.toString(paid)) + ", " + dialect.literal(TIMESTAMP, created) + ")");
  }

  static Stream<Arguments> conditions() {
    return Stream.of(
        Arguments.of(compare(Comparison.Operator.GREATER, QUANTITY, integer(0)), List.of("1")),
        Arguments.of(new Logic(Logic.Operator.AND, compare(Comparison.Operator.GREATER_EQUALS, QUANTITY, integer(7)),
            PAID), List.of("1")),
        Arguments.of(new Logic(Logic.Operator.OR, new Not(PAID), compare(Comparison.Operator.LESS, QUANTITY,
            integer(0))), List.of("2")),
        Arguments.of(PAID, List.of("1")),
        Arguments.of(new Not(PAID), List.of("2")),
        Arguments.of(equal(PAID, bool(false)), List.of("2")),
        Arguments.of(bool(true), List.of("1", "2")),
        Arguments.of(equal(compare(Comparison.Operator.GREATER, QUANTITY, integer(0)), PAID), List.of("1", "2")),
        Arguments.of(new IsNull(NICKNAME), List.of("1")),
        Arguments.of(new IsNull(NICKNAME).negate(), List.of("2")),
        Arguments.of(compare(Comparison.Operator.NOT_EQUALS, NAME, text("Bob")), List.of("1")),
        Arguments.of(compare(Comparison.Operator.LESS_EQUALS, QUANTITY, integer(-3)), List.of("2")),
        Arguments.of(new In(QUANTITY, List.of(integer(7), integer(8))), List.of("1")),
        Arguments.of(new In(QUANTITY, List.of()), List.of()),
        Arguments.of(equal(call(Function.ABS, PRICE), real("2.5")), List.of("1")),
        Arguments.of(equal(call(Function.ABS, QUANTITY), integer(3)), List.of("2")),
        Arguments.of(equal(call(Function.LOWER, call(Function.TRIM, NAME)), text("ana")), List.of("1")),
        Arguments.of(equal(call(Function.UPPER, NAME), text("BOB")), List.of("2")),
        Arguments.of(equal(call(Function.LENGTH, NAME), integer(5)), List.of("1")),
        Arguments.of(equal(arithmetic(Arithmetic.Operator.CONCAT, NAME, text("!")), text("Bob!")), List.of("2")),
        Arguments.of(new IsNull(arithmetic(Arithmetic.Operator.CONCAT, NICKNAME, text("!"))), List.of("1")),
        Arguments.of(equal(call(Function.COALESCE, NICKNAME, NAME), text("Bobby")), List.of("2")),
        Arguments.of(equal(call(Function.COALESCE, NICKNAME, text("none")), text("none")), List.of("1")),
        Arguments.of(equal(arithmetic(Arithmetic.Operator.INTEGER_DIVIDE, QUANTITY, integer(2)), integer(3)),
            List.of("1")),
        Arguments.of(equal(arithmetic(Arithmetic.Operator.INTEGER_DIVIDE, QUANTITY, integer(2)), integer(-1)),
            List.of("2")),
        Arguments.of(equal(arithmetic(Arithmetic.Operator.MODULO, QUANTITY, integer(4)), integer(3)), List.of("1")),
        Arguments.of(equal(arithmetic(Arithmetic.Operator.MODULO, QUANTITY, integer(2)), integer(-1)), List.of("2")),
        Arguments.of(equal(arithmetic(Arithmetic.Operator.DIVIDE, PRICE, real("2.0")), real("2.0")), List.of("2")),
        Arguments.of(equal(new Negative(QUANTITY), integer(3)), List.of("2")),
        Arguments.of(equal(arithmetic(Arithmetic.Operator.SUBTRACT, arithmetic(Arithmetic.Operator.MULTIPLY,
            QUANTITY, integer(2)), integer(1)), integer(13)), List.of("1")),
        Arguments.of(equal(arithmetic(Arithmetic.Operator.ADD, PRICE, real("0.5")), real("4.5")), List.of("2")),
        Arguments.of(equal(new Conditional(PAID, QUANTITY, integer(0)), integer(7)), List.of("1")),
        Arguments.of(equal(new Conditional(compare(Comparison.Operator.GREATER, QUANTITY, integer(0)), text("pos"),
            text("neg")), text("neg")), List.of("2")),
        Arguments.of(compare(Comparison.Operator.LESS, CREATED, call(Function.NOW)), List.of("1")),
        Arguments.of(compare(Comparison.Operator.NOT_EQUALS, call(Function.UUID), call(Function.UUID)),
            List.of("1", "2")));
  }

  @ParameterizedTest(name = "({0})")
  @MethodSource("conditions")
  @DisplayName("Selects the rows of a condition")
  void selectRows(SqlExpression condition, List<String> ids) throws SQLException {
    String select = "SELECT " + q("id") + " FROM " + q("samples") + " WHERE " + condition.condition(SqlEnvironment.of(
        dialect)) + " ORDER BY " + q("id");

    assertThat(query(select).stream().map(List::getFirst).toList()).as(select).isEqualTo(ids);
  }

  static Stream<Arguments> values() {
    return Stream.of(
        Arguments.of(arithmetic(Arithmetic.Operator.ADD, arithmetic(Arithmetic.Operator.MULTIPLY, QUANTITY,
            integer(2)), integer(1)), "15"),
        Arguments.of(arithmetic(Arithmetic.Operator.CONCAT, NAME, text("?")), " Ana ?"),
        Arguments.of(call(Function.COALESCE, NICKNAME, text("none")), "none"),
        Arguments.of(call(Function.LENGTH, NAME), "5"),
        Arguments.of(arithmetic(Arithmetic.Operator.INTEGER_DIVIDE, QUANTITY, integer(2)), "3"),
        Arguments.of(new Conditional(PAID, text("y"), text("n")), "y"),
        Arguments.of(new Conditional(new IsNull(NICKNAME), text("unset"), NICKNAME), "unset"));
  }

  @ParameterizedTest(name = "({0})")
  @MethodSource("values")
  @DisplayName("Selects the value of an expression")
  void selectValue(SqlExpression value, String expected) throws SQLException {
    String select = "SELECT " + value.value(SqlEnvironment.of(dialect)) + " FROM " + q("samples") + " WHERE "
        + q("id") + " = 1";

    assertThat(query(select)).as(select).containsExactly(List.of(expected));
  }

  static Stream<Arguments> changes() {
    return Stream.of(
        Arguments.of(new ColumnChange("quantity"), 1, 2, 1),
        Arguments.of(new ColumnChange("quantity"), 1, 1, 0),
        Arguments.of(new ColumnChange("nickname"), 1, 2, 1),
        Arguments.of(new ColumnChange("nickname"), 2, 1, 1),
        Arguments.of(new ColumnChange("nickname"), 1, 1, 0),
        Arguments.of(new Not(new ColumnChange("nickname")), 2, 2, 1),
        Arguments.of(compare(Comparison.Operator.GREATER, new ColumnReference("quantity", Row.NEW),
            new ColumnReference("quantity", Row.OLD)), 2, 1, 1),
        Arguments.of(compare(Comparison.Operator.GREATER, QUANTITY, new ColumnReference("quantity", Row.OLD)), 1, 2,
            0));
  }

  @ParameterizedTest(name = "({0})")
  @MethodSource("changes")
  @DisplayName("Compares the old and the new row")
  void compareRows(SqlExpression condition, long old, long current, long expected)
      throws SQLException {
    SqlEnvironment environment = SqlEnvironment.of(dialect).with(Row.OLD, q("o")).with(Row.NEW, q("n"))
        .with(Row.CURRENT, q("n"));
    String select = "SELECT COUNT(*) FROM " + q("samples") + " " + q("o") + ", " + q("samples") + " " + q("n")
        + " WHERE " + q("o") + "." + q("id") + " = " + old + " AND " + q("n") + "." + q("id") + " = " + current
        + " AND " + condition.condition(environment);

    assertThat(count(select)).as(select).isEqualTo(expected);
  }
}
