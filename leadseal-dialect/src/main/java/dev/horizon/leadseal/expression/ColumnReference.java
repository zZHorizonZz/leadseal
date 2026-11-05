package dev.horizon.leadseal.expression;

import dev.horizon.leadseal.InvalidSchemaException;
import dev.horizon.leadseal.Table;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * A column of a row.
 *
 * @param name the column name
 * @param row the row of the column
 */
public record ColumnReference(String name, Row row) implements Value {

  public ColumnReference {
    if (name == null || name.isBlank()) {
      throw new InvalidSchemaException("A column of an expression has no name.");
    }
    Objects.requireNonNull(row, "row");
  }

  /**
   * Gives a column of the current row.
   *
   * @param name the column name
   * @return the column
   */
  public static ColumnReference of(String name) {
    return new ColumnReference(name, Row.CURRENT);
  }

  @Override
  public String sql(SqlEnvironment environment) {
    return environment.column(name, row);
  }

  @Override
  public List<SqlExpression> operands() {
    return List.of();
  }

  @Override
  public Set<ColumnReference> columns() {
    return Set.of(this);
  }

  @Override
  public SqlExpression rename(UnaryOperator<String> names) {
    return new ColumnReference(names.apply(name), row);
  }

  @Override
  public void validate(Table table, Set<Row> rows) {
    if (!rows.contains(row)) {
      throw new InvalidSchemaException("An expression of the table " + table.name() + " reads the column " + name
          + " of the " + row.describe() + ", which it cannot read there.");
    }
    if (table.column(name).isEmpty()) {
      throw new InvalidSchemaException("An expression of the table " + table.name() + " reads the column " + name
          + ", which the table does not have.");
    }
  }
}
