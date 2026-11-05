package dev.horizon.leadseal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import dev.horizon.leadseal.expression.SqlEnvironment;
import dev.horizon.leadseal.expression.SqlExpression;
import dev.horizon.leadseal.type.BooleanType;
import dev.horizon.leadseal.type.IntegerType;
import dev.horizon.leadseal.type.TextType;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SqlExpressionTest {

  private static final SqlEnvironment CURRENT = SqlEnvironment.of(new TestDialect());
  private static final SqlEnvironment TRIGGER = CURRENT.with(Row.CURRENT, "NEW").with(Row.NEW, "NEW")
      .with(Row.OLD, "OLD");
  private static final ObjectMapper JSON = new ObjectMapper()
      .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  private static final ColumnReference QUANTITY = ColumnReference.of("quantity");
  private static final ColumnReference NAME = ColumnReference.of("name");
  private static final Literal TWO = new Literal(new IntegerType(IntegerType.Size.INT64), "2");
  private static final Literal EMPTY = new Literal(TextType.unlimited(), "");
  private static final Literal TRUE = new Literal(new BooleanType(), "true");
  private static final Table ORDERS = Table.of("shop.Order", "orders", List.of(
      Column.of("1", "id", new IntegerType(IntegerType.Size.INT64), NotNull.INSTANCE),
      Column.of("2", "quantity", new IntegerType(IntegerType.Size.INT32)),
      Column.of("3", "name", TextType.of(20))), PrimaryKey.of("orders", List.of("id")));

  private static Call call(Function function, SqlExpression... arguments) {
    return new Call(function, List.of(arguments));
  }

  static Stream<Arguments> examples() {
    return Stream.of(
        Arguments.of(new Logic(Logic.Operator.AND, new Comparison(Comparison.Operator.GREATER, QUANTITY, TWO), TRUE),
            "((\"quantity\" > 2) AND true)"),
        Arguments.of(new Logic(Logic.Operator.OR, new IsNull(NAME).negate(), new IsNull(QUANTITY)),
            "((\"name\" IS NOT NULL) OR (\"quantity\" IS NULL))"),
        Arguments.of(new Not(new Comparison(Comparison.Operator.EQUALS, QUANTITY, TWO)), "(NOT (\"quantity\" = 2))"),
        Arguments.of(new Comparison(Comparison.Operator.NOT_EQUALS, new Negative(QUANTITY), TWO),
            "((- \"quantity\") <> 2)"),
        Arguments.of(new Negative(new Literal(new IntegerType(IntegerType.Size.INT64), "-5")), "(- -5)"),
        Arguments.of(new Arithmetic(Arithmetic.Operator.MODULO, new Arithmetic(Arithmetic.Operator.INTEGER_DIVIDE,
            QUANTITY, TWO), TWO), "((\"quantity\" / 2) % 2)"),
        Arguments.of(new Arithmetic(Arithmetic.Operator.CONCAT, NAME, EMPTY), "(\"name\" || '')"),
        Arguments.of(new In(QUANTITY, List.of(TWO, TWO)), "(\"quantity\" IN (2, 2))"),
        Arguments.of(new In(QUANTITY, List.of()), "(1 = 0)"),
        Arguments.of(new Conditional(new Comparison(Comparison.Operator.LESS, QUANTITY, TWO), NAME, EMPTY),
            "CASE WHEN (\"quantity\" < 2) THEN \"name\" ELSE '' END"),
        Arguments.of(call(Function.COALESCE, call(Function.LOWER, call(Function.TRIM, NAME)), EMPTY),
            "COALESCE(LOWER(TRIM(\"name\")), '')"),
        Arguments.of(call(Function.LENGTH, call(Function.UPPER, NAME)), "CHAR_LENGTH(UPPER(\"name\"))"),
        Arguments.of(call(Function.ABS, QUANTITY), "ABS(\"quantity\")"),
        Arguments.of(call(Function.NOW), "CURRENT_TIMESTAMP"));
  }

  @ParameterizedTest(name = "({0})")
  @MethodSource("examples")
  @DisplayName("Writes the SQL of the test dialect")
  void writeSql(SqlExpression expression, String sql) {
    assertThat(expression.value(CURRENT)).isEqualTo(sql);
    assertThat(expression.condition(CURRENT)).isEqualTo(sql);
  }

  @Test
  @DisplayName("Writes the columns of the rows with their qualifiers")
  void writeQualifiedColumns() {
    SqlExpression expression = new Logic(Logic.Operator.AND, new ColumnChange("quantity"), new Comparison(
        Comparison.Operator.GREATER, new ColumnReference("quantity", Row.NEW), new ColumnReference("quantity",
            Row.OLD)));

    assertThat(expression.condition(TRIGGER)).isEqualTo("((OLD.\"quantity\" IS DISTINCT FROM NEW.\"quantity\") AND"
        + " (NEW.\"quantity\" > OLD.\"quantity\"))");
  }

  @Test
  @DisplayName("Refuses a row that the environment does not have")
  void refuseRow() {
    assertThatThrownBy(() -> new ColumnReference("quantity", Row.OLD).value(CURRENT))
        .isInstanceOf(InvalidSchemaException.class).hasMessageContaining("old row");
  }

  @Test
  @DisplayName("Negates a negation into its operand")
  void negateNegation() {
    assertThat(new IsNull(NAME).negate().negate()).isEqualTo(new IsNull(NAME));
  }

  @Test
  @DisplayName("Keeps an expression in JSON")
  void keepInJson() throws Exception {
    SqlExpression expression = new Conditional(new In(QUANTITY, List.of()), call(Function.COALESCE, NAME, EMPTY),
        new Arithmetic(Arithmetic.Operator.CONCAT, NAME, call(Function.UUID)));

    assertThat(JSON.readValue(JSON.writeValueAsString(expression), SqlExpression.class)).isEqualTo(expression);
  }

  @Test
  @DisplayName("Renames its columns")
  void renameColumns() {
    SqlExpression expression = new Logic(Logic.Operator.OR, new Comparison(Comparison.Operator.EQUALS, QUANTITY,
        call(Function.LENGTH, NAME)), new ColumnChange("name"));

    assertThat(expression.rename(name -> name.equals("name") ? "title" : name)).isEqualTo(new Logic(
        Logic.Operator.OR, new Comparison(Comparison.Operator.EQUALS, QUANTITY, call(Function.LENGTH,
            ColumnReference.of("title"))),
        new ColumnChange("title")));
    assertThat(expression.columns()).containsExactly(QUANTITY, NAME, new ColumnReference("name", Row.OLD),
        new ColumnReference("name", Row.NEW));
  }

  static Stream<Arguments> determinism() {
    return Stream.of(
        Arguments.of(new Arithmetic(Arithmetic.Operator.ADD, QUANTITY, TWO), true),
        Arguments.of(new Comparison(Comparison.Operator.LESS, ColumnReference.of("created"), call(Function.NOW)),
            false),
        Arguments.of(call(Function.LOWER, call(Function.UUID)), false));
  }

  @ParameterizedTest(name = "({0})")
  @MethodSource("determinism")
  @DisplayName("Tells if it has the same value each time")
  void tellConstancy(SqlExpression expression, boolean deterministic) {
    assertThat(expression.deterministic()).isEqualTo(deterministic);
  }

  @Test
  @DisplayName("Fits a table that has its columns and rows")
  void fitTable() {
    new Comparison(Comparison.Operator.GREATER, QUANTITY, TWO).validate(ORDERS, Set.of(Row.CURRENT));
    new ColumnChange("quantity").validate(ORDERS, Set.of(Row.CURRENT, Row.NEW, Row.OLD));
  }

  static Stream<Arguments> misfits() {
    return Stream.of(
        Arguments.of(ColumnReference.of("total"), Set.of(Row.CURRENT), "total, which the table does not have"),
        Arguments.of(new ColumnReference("quantity", Row.OLD), Set.of(Row.CURRENT), "of the old row"),
        Arguments.of(new ColumnChange("quantity"), Set.of(Row.CURRENT), "changed(), which needs the old and the new row"),
        Arguments.of(new ColumnChange("total"), Set.of(Row.CURRENT, Row.NEW, Row.OLD), "total, which the table does not"));
  }

  @ParameterizedTest(name = "({0})")
  @MethodSource("misfits")
  @DisplayName("Refuses a table that lacks its columns or rows")
  void refuseTable(SqlExpression expression, Set<Row> rows, String message) {
    assertThatThrownBy(() -> expression.validate(ORDERS, rows)).isInstanceOf(InvalidSchemaException.class)
        .hasMessageContaining(message);
  }

  @Test
  @DisplayName("Refuses a call with the wrong number of arguments")
  void refuseArity() {
    assertThatThrownBy(() -> call(Function.LOWER)).isInstanceOf(InvalidSchemaException.class)
        .hasMessageContaining("lower takes 1 arguments, not 0");
  }

  @Test
  @DisplayName("Refuses a constant that is not a value of its type")
  void refuseConstant() {
    assertThatThrownBy(() -> new Literal(new IntegerType(IntegerType.Size.INT32), "two"))
        .isInstanceOf(InvalidSchemaException.class).hasMessageContaining("not a value of its type");
  }
}
