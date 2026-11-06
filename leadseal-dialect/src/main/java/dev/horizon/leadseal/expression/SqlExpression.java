package dev.horizon.leadseal.expression;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.horizon.leadseal.SchemaException;
import dev.horizon.leadseal.Table;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * An expression that a database computes, such as the condition of a trigger or the value of a computed column.
 *
 * <p>
 * An expression is a {@link Condition}, such as a comparison, or a {@link Value}, such as a column. SQL keeps the two
 * apart: you write a condition after {@code WHERE} or {@code CHECK}, and a value as a computed column. Each expression
 * writes itself in both places with {@link #condition(SqlEnvironment)} and {@link #value(SqlEnvironment)}, in the SQL
 * of the dialect of the environment. It writes every operator in parentheses, so no dialect depends on the precedence
 * of operators.
 *
 * <p>
 * An expression reads columns by their names, so a migration that renames a column renames them in the expressions
 * that read it, as {@link #rename(UnaryOperator)} does. A snapshot of a migration stores an expression as JSON.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ColumnReference.class, name = "column"),
    @JsonSubTypes.Type(value = Literal.class, name = "literal"),
    @JsonSubTypes.Type(value = Negative.class, name = "negative"),
    @JsonSubTypes.Type(value = Arithmetic.class, name = "arithmetic"),
    @JsonSubTypes.Type(value = Conditional.class, name = "conditional"),
    @JsonSubTypes.Type(value = Call.class, name = "call"),
    @JsonSubTypes.Type(value = Not.class, name = "not"),
    @JsonSubTypes.Type(value = Comparison.class, name = "comparison"),
    @JsonSubTypes.Type(value = Logic.class, name = "logic"),
    @JsonSubTypes.Type(value = IsNull.class, name = "is_null"),
    @JsonSubTypes.Type(value = In.class, name = "in"),
    @JsonSubTypes.Type(value = ColumnChange.class, name = "change"),
    @JsonSubTypes.Type(value = ForeignColumn.class, name = "foreign")
})
public sealed interface SqlExpression permits Condition, Value {

  /**
   * Writes this expression where SQL expects a condition.
   *
   * @param environment the dialect and the rows that the expression reads
   * @return the SQL
   */
  String condition(SqlEnvironment environment);

  /**
   * Writes this expression where SQL expects a value.
   *
   * @param environment the dialect and the rows that the expression reads
   * @return the SQL
   */
  String value(SqlEnvironment environment);

  /**
   * Writes the negation of this expression, as a condition.
   *
   * @param environment the dialect and the rows that the expression reads
   * @return the SQL, for example {@code (NOT condition)}
   */
  default String negation(SqlEnvironment environment) {
    return "(NOT " + condition(environment) + ")";
  }

  /**
   * Gives the negation of this expression. The negation of a negation is the expression that it negates.
   *
   * @return the negation
   */
  default SqlExpression negate() {
    return new Not(this);
  }

  /**
   * Gives the expressions that this expression reads.
   *
   * @return the operands, in order
   */
  List<SqlExpression> operands();

  /**
   * Gives this expression with other names of columns.
   *
   * @param names the new name of each column name
   * @return the expression with the new names
   */
  SqlExpression rename(UnaryOperator<String> names);

  /**
   * Tells if this expression has the same value each time that it reads the same columns. {@code now()} and
   * {@code uuid()} do not.
   *
   * @return true if the expression is deterministic
   */
  default boolean deterministic() {
    return operands().stream().allMatch(SqlExpression::deterministic);
  }

  /**
   * Gives the columns that this expression reads.
   *
   * @return the columns, in the order the expression reads them first, without repeats
   */
  default Set<ColumnReference> columns() {
    return operands().stream().flatMap(operand -> operand.columns().stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Gives the columns of other tables that this expression reads.
   *
   * @return the columns, in the order the expression reads them
   */
  default List<ForeignColumn> foreignColumns() {
    return operands().stream().flatMap(operand -> operand.foreignColumns().stream()).toList();
  }

  /**
   * Checks that this expression fits a table.
   *
   * @param table the table whose columns the expression reads
   * @param rows the rows that the expression can read, for example only {@link Row#CURRENT} in a check
   * @throws SchemaException if the expression reads a column that the table does not have, or a row that it cannot
   * read
   */
  default void validate(Table table, Set<Row> rows) {
    operands().forEach(operand -> operand.validate(table, rows));
  }

}
